package com.financial.cloud.service.hr;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.financial.cloud.authn.handler.PersistFieldAutoFillHandler;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ConstsUser;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.dto.hr.SalaryDetailChangeDto;
import com.financial.cloud.dto.hr.SalaryDetailPageDto;
import com.financial.cloud.dto.voucher.GenerateVoucherDto;
import com.financial.cloud.dto.voucher.VoucherChangeDto;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.book.SettlementCarryforwardMapper;
import com.financial.cloud.repository.hr.EmployeeMapper;
import com.financial.cloud.repository.hr.EmployeeSalaryMapper;
import com.financial.cloud.repository.hr.EmployeeSalaryTempMapper;
import com.financial.cloud.repository.voucher.*;
import com.financial.cloud.service.book.BookSealGuard;
import com.financial.cloud.service.book.BookSubjectService;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.voucher.VoucherService;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Explicit-only integration test: mvn -Dtest=PayrollTransactionMysqlIT test.
 *
 * Requires a fresh, explicitly named financial_cloud_e2e_20261003_it4_* database
 * containing repository schema only. Missing configuration/permissions FAIL,
 * rather than silently skip. No app/server, database creation/drop, truncation,
 * business fixture or seed script is invoked. Cleanup targets owned book IDs.
 *
 * Real: Spring transactions, book/salary/preview reads and writes, VoucherService
 * save/delete, voucher header/items/words and every MyBatis SQL result. Mocked:
 * configuration and postable subject metadata, plus unused constructor services.
 * The interceptor pauses AFTER real SQL, or injects real fixture DB failures;
 * it never fabricates lock/query/update results or copies production mapper SQL.
 * One explicit advisor fault changes the callee's service response only after
 * real VoucherService.save succeeds and its database rows are observed; it
 * models an unsuccessful return after partial writes without inventing SQL.
 * New helper registration is reflective so this same source compiles on the
 * preserved pre-fix project, where its concurrency/zero-row assertions fail.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(value = 45, unit = TimeUnit.SECONDS)
class PayrollTransactionMysqlIT {
    private static final YearMonth MONTH = YearMonth.now();
    private static final String PREFIX = "it4_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private static final String TRIGGER = PREFIX + "_link_fault";
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();
    private static final String SALARY_UPDATE = EmployeeSalaryMapper.class.getName() + ".updateById";
    private static final String VOUCHER_INSERT = VoucherMapper.class.getName() + ".insert";
    private final Set<String> ownedBooks = new LinkedHashSet<>();
    private AnnotationConfigApplicationContext context;
    private DataSource dataSource;
    private JdbcTemplate jdbc;
    private EmployeeSalaryService salaries;
    private EmployeeSalaryTempService previews;
    private SqlProbe probe;
    private ExecutorService workers;
    private String database;
    private String declaredSalaryIsolation;
    private String book;
    private String employee;
    private String salary;
    private int sequence;

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class Transactions { }

    @BeforeAll
    void configureRealMybatisAndTransactions() throws Exception {
        Map<String, String> environment = System.getenv();
        for (String name : List.of("FC_DB_HOST", "FC_DB_PORT", "FC_DB_NAME", "FC_DB_USER", "FC_DB_PASSWORD")) {
            assertTrue(environment.containsKey(name), "Explicit " + name + " is required for this IT");
        }
        database = environment.get("FC_DB_NAME");
        assertTrue(database.matches("financial_cloud_e2e_20261003_it4_[a-z0-9_]+") && database.length() <= 64,
                "Only the dedicated fourth-iteration database namespace is allowed");
        String host = environment.get("FC_DB_HOST");
        assertTrue(host.matches("[A-Za-z0-9_.:-]+"), "Explicit plain database host is required");
        int port = Integer.parseInt(environment.get("FC_DB_PORT"));
        assertTrue(port >= 1 && port <= 65535, "Explicit valid database port is required");
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        source.setUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=10000&socketTimeout=20000");
        source.setUsername(environment.get("FC_DB_USER"));
        source.setPassword(environment.get("FC_DB_PASSWORD"));
        dataSource = source;
        jdbc = new JdbcTemplate(dataSource);
        assertEquals(database, jdbc.queryForObject("SELECT DATABASE()", String.class));
        assertEquals("REPEATABLE-READ", jdbc.queryForObject("SELECT @@transaction_isolation", String.class),
                "Keep the database session default at REPEATABLE READ; service annotations must select their own isolation");
        Transactional declaredTransaction = EmployeeSalaryService.class.getMethod("generateVoucher", GenerateVoucherDto.class)
                .getAnnotation(Transactional.class);
        assertNotNull(declaredTransaction, "The actual salary service must declare a transaction");
        declaredSalaryIsolation = switch (declaredTransaction.isolation()) {
            case READ_COMMITTED -> "READ-COMMITTED";
            case DEFAULT -> "REPEATABLE-READ"; // The preserved original source uses the asserted database default.
            default -> throw new AssertionError("Unsupported actual salary transaction isolation: " + declaredTransaction.isolation());
        };
        assertProductionSalaryScopeIndex();
        // This read proves lock-observation permissions before starting workers.
        jdbc.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits", Long.class);
        for (String table : List.of("book", "employee_salary", "voucher", "voucher_item", "voucher_word")) {
            assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM `" + table + "`", Long.class),
                    "This IT requires repository schema without business rows or seeds: " + table);
        }

        context = new AnnotationConfigApplicationContext();
        context.register(Transactions.class);
        context.registerBean(DataSource.class, () -> dataSource);
        context.registerBean("transactionManager", DataSourceTransactionManager.class,
                () -> new DataSourceTransactionManager(dataSource));
        IdentifierGenerator identifiers = new DefaultIdentifierGenerator(0L, 0L);
        context.registerBean(IdentifierGenerator.class, () -> identifiers);
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        GlobalConfig globals = new GlobalConfig();
        globals.setMetaObjectHandler(new PersistFieldAutoFillHandler());
        globals.setIdentifierGenerator(identifiers);
        MybatisPlusInterceptor pagination = new MybatisPlusInterceptor();
        pagination.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        probe = new SqlProbe();
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setApplicationContext(context);
        factoryBean.setDataSource(dataSource);
        factoryBean.setConfiguration(configuration);
        factoryBean.setGlobalConfig(globals);
        factoryBean.setPlugins(pagination, probe);
        factoryBean.setMapperLocations(new ClassPathResource("com/financial/cloud/repository/hr/EmployeeSalaryTempMapper.xml"));
        SqlSessionFactory factory = factoryBean.getObject();
        SqlSessionTemplate session = new SqlSessionTemplate(factory);
        context.registerBean(SqlSessionFactory.class, () -> factory);
        context.registerBean(SqlSessionTemplate.class, () -> session);
        context.registerBean("fixtureSaveFailureBoundary", Advisor.class, this::saveFailureBoundary,
                definition -> definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE));
        for (Class<?> mapper : List.of(BookMapper.class, EmployeeMapper.class,
                EmployeeSalaryMapper.class, EmployeeSalaryTempMapper.class,
                VoucherMapper.class, VoucherItemMapper.class, VoucherWordMapper.class,
                VoucherItemAuxiliaryMapper.class, VoucherItemCashFlowMapper.class,
                VoucherTemplateMapper.class, VoucherTemplateItemMapper.class,
                SettlementCarryforwardMapper.class)) {
            if (!factory.getConfiguration().hasMapper(mapper)) {
                factory.getConfiguration().addMapper(mapper);
            }
            registerMapper(mapper, session);
        }
        ConfigSysService settings = mock(ConfigSysService.class);
        when(settings.getCurrentTerm(anyString())).thenReturn(MONTH.toString());
        when(settings.getCurrentTermLastDate(anyString())).thenReturn(
                Date.from(MONTH.atEndOfMonth().atStartOfDay(ZoneOffset.UTC).toInstant()));
        context.getBeanFactory().registerSingleton("fixtureConfigSysService", settings);
        BookSubjectService subjects = mock(BookSubjectService.class);
        when(subjects.resolvePostableSubject(anyString(), anyString())).thenAnswer(call ->
                subject(call.getArgument(0), call.getArgument(1)));
        when(subjects.getById(anyString())).thenAnswer(call -> {
            String identifier = call.getArgument(0);
            int separator = identifier.lastIndexOf('_');
            return subject(identifier.substring(0, separator), identifier.substring(separator + 1));
        });
        context.getBeanFactory().registerSingleton("fixtureBookSubjectService", subjects);
        for (String helperName : List.of("com.financial.cloud.service.hr.PayrollWriteLock",
                "com.financial.cloud.service.book.PayrollWriteLock")) {
            try {
                registerServiceAndMetadata(Class.forName(helperName));
            } catch (ClassNotFoundException beforeFix) {
                // Intended compatibility: no hard dependency on new source/API.
            }
        }
        registerServiceAndMetadata(BookSealGuard.class);
        registerServiceAndMetadata(VoucherService.class);
        registerServiceAndMetadata(EmployeeSalaryService.class);
        registerServiceAndMetadata(EmployeeSalaryTempService.class);
        context.refresh();
        salaries = context.getBean(EmployeeSalaryService.class);
        previews = context.getBean(EmployeeSalaryTempService.class);
        assertTrue(AopUtils.isAopProxy(salaries), "Salary service must be a real Spring transactional proxy");
        assertTrue(AopUtils.isAopProxy(previews), "Preview push must be a real Spring transactional proxy");
        assertTrue(AopUtils.isAopProxy(context.getBean(VoucherService.class)), "VoucherService.save must join Spring transactions");
        jdbc.execute("CREATE TRIGGER `" + TRIGGER + "` BEFORE UPDATE ON employee_salary FOR EACH ROW "
                + "BEGIN IF @payroll_it4_fail_link = 1 AND LEFT(NEW.book_id," + PREFIX.length() + ")='" + PREFIX + "' "
                + "AND (NEW.accrual_voucher_id IS NOT NULL OR NEW.salary_voucher_id IS NOT NULL) "
                + "THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'IT4 actual link update failure'; END IF; END");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerMapper(Class mapper, SqlSessionTemplate session) {
        context.registerBean(mapper, () -> session.getMapper(mapper));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerServiceAndMetadata(Class service) {
        if (context.getBeanFactory().getBeanNamesForType(service).length > 0) return;
        for (Constructor<?> constructor : service.getDeclaredConstructors()) {
            for (Class<?> dependency : constructor.getParameterTypes()) {
                if (dependency == ObjectProvider.class) continue;
                if (context.getBeanFactory().getBeanNamesForType(dependency).length == 0) {
                    // Preconstructed metadata mocks must bypass Spring field
                    // injection; ServiceImpl mocks otherwise require extra
                    // production mappers although none of their SQL is used.
                    context.getBeanFactory().registerSingleton("fixtureMetadata_" + dependency.getName(), mock(dependency));
                }
            }
        }
        context.registerBean(service);
    }

    private void assertProductionSalaryScopeIndex() {
        List<Map<String, Object>> rows = new ArrayList<>(jdbc.queryForList(
                "SHOW INDEX FROM employee_salary WHERE Key_name='idx_salary_payroll_scope'"));
        List<String> requiredColumns = List.of("book_id", "belong_date", "employee_id", "created_date");
        rows.sort(Comparator.comparingInt(row -> ((Number) row.get("Seq_in_index")).intValue()));
        boolean found = rows.size() == requiredColumns.size()
                    && rows.stream().map(row -> (String) row.get("Column_name")).toList().equals(requiredColumns)
                    && rows.stream().allMatch(row -> ((Number) row.get("Non_unique")).intValue() == 1
                    && row.get("Sub_part") == null && "YES".equals(row.get("Visible"))
                    && "A".equals(row.get("Collation")) && "BTREE".equals(row.get("Index_type")));
        assertTrue(found, "Apply the production nonunique full-column salary scope index before this IT: " + requiredColumns);
    }

    private Advisor saveFailureBoundary() {
        StaticMethodMatcherPointcut pointcut = new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return VoucherService.class.isAssignableFrom(targetClass) && method.getName().equals("save")
                        && Arrays.equals(method.getParameterTypes(), new Class<?>[]{VoucherChangeDto.class});
            }
        };
        MethodInterceptor failureAfterRealSave = invocation -> {
            Object actual = invocation.proceed();
            if (probe.fault == Fault.SAVE_RETURN_FAILURE) {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                assertInstanceOf(Message.class, actual);
                assertEquals(Message.SUCCESS, ((Message<?>) actual).getCode(), "Real VoucherService.save must finish successfully first");
                assertTrue(probe.headerInserted, "No response fault until the actual header SQL completed");
                assertAccountingRows(1, 3, 1);
                probe.saveFailureBoundaryObserved = true;
                return Message.failed("IT4 injected unsuccessful service response after real voucher save");
            }
            return actual;
        };
        DefaultPointcutAdvisor boundary = new DefaultPointcutAdvisor(pointcut, failureAfterRealSave);
        boundary.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return boundary;
    }

    @BeforeEach
    void seedOwnedFixture() {
        book = PREFIX + "_" + (++sequence) + "a";
        employee = book + "e";
        salary = book + "s";
        ownedBooks.add(book);
        seedBook(book);
        seedEmployee(book, employee);
        seedSalary(book, employee, salary);
        seedTemplates(book);
        jdbc.update("INSERT INTO employee_salary_temp (id,employee_id,book_id,belong_date,pay_amount,personal_tax,total_amount,deleted) VALUES (?,?,?,?,100,20,80,'n')",
                book + "t", employee, book, MONTH.toString());
        probe.reset();
        workers = Executors.newFixedThreadPool(2);
    }

    private void seedBook(String identifier) {
        jdbc.update("INSERT INTO book (id,name,company_name,enable_date,standard_id,status,deleted) VALUES (?,?,?,?,?,1,'n')",
                identifier, "Payroll IT", "Payroll IT", MONTH.toString(), "fixture-standard");
    }

    private void seedEmployee(String bookId, String identifier) {
        jdbc.update("INSERT INTO employee (id,book_id,display_name,employee_type,deleted) VALUES (?,?,?,?,'n')",
                identifier, bookId, "Payroll IT", ConstsUser.EMPLOYEE_TYPE.PARTTIME);
    }

    private void seedSalary(String bookId, String employeeId, String identifier) {
        jdbc.update("INSERT INTO employee_salary (id,employee_id,book_id,belong_date,pay_amount,personal_tax,total_amount,deleted) VALUES (?,?,?,?,100,20,80,'n')",
                identifier, employeeId, bookId, MONTH.toString());
    }

    private void seedTemplates(String bookId) {
        for (String code : List.of("fp_lwf", "zf_lwf")) {
            String template = bookId + code;
            jdbc.update("INSERT INTO voucher_template (id,related_id,code,name,remark,voucher_type,voucher_date,word_head,status,deleted) VALUES (?,?,?,?,?,0,0,'记',1,'n')",
                    template, bookId, code, "Payroll IT", "{yyyy}-{mm} {name}");
            List<String> codes = code.equals("fp_lwf") ? List.of("660222", "222114", "224101") : List.of("224101", "100201");
            for (int index = 0; index < codes.size(); index++) {
                jdbc.update("INSERT INTO voucher_template_item (id,related_id,template_id,subject_code,direction,summary,deleted) VALUES (?,?,?,?,?,?,'n')",
                        template + index, bookId, template, codes.get(index), index == 0 ? 1 : 2, "Payroll integration");
            }
        }
    }

    private static BookSubject subject(String bookId, String code) {
        BookSubject subject = new BookSubject();
        subject.setId(bookId + "_" + code);
        subject.setBookId(bookId);
        subject.setCode(code);
        subject.setName("Payroll fixture " + code);
        subject.setBalance(BigDecimal.ZERO);
        subject.setAuxiliary("[]");
        return subject;
    }

    @AfterEach
    void stopWorkersAndRemoveOwnedRows() throws Exception {
        probe.release();
        if (workers != null) {
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(20, TimeUnit.SECONDS), "Worker must release its real transaction before cleanup");
        }
        ROLE.remove();
        for (String owned : ownedBooks) {
            assertTrue(owned.startsWith(PREFIX + "_"), "Cleanup must target this run's fixture namespace");
            for (String table : List.of("voucher_auxiliary", "voucher_item_cash_flow", "voucher_item", "voucher_word", "voucher",
                    "employee_salary_temp", "employee_salary", "employee")) {
                jdbc.update("DELETE FROM `" + table + "` WHERE book_id = ?", owned);
            }
            jdbc.update("DELETE FROM voucher_template_item WHERE related_id = ?", owned);
            jdbc.update("DELETE FROM voucher_template WHERE related_id = ?", owned);
            jdbc.update("DELETE FROM book WHERE id = ?", owned);
        }
        ownedBooks.clear();
    }

    @AfterAll
    void closeFixtureContext() {
        if (jdbc != null && database != null && database.matches("financial_cloud_e2e_20261003_it4_[a-z0-9_]+")) {
            jdbc.execute("DROP TRIGGER IF EXISTS `" + TRIGGER + "`");
        }
        if (context != null) context.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "delete", "generate", "push", "unlink", "save"})
    void everySameBookWriteWaitsForActualGenerateTransaction(String competing) throws Exception {
        probe.pause("holder", VOUCHER_INSERT);
        Future<Outcome> holder = start("holder", () -> salaries.generateVoucher(generate(book, salary, 2)));
        probe.awaitPaused();
        Future<Outcome> contender = start("contender", () -> competing(competing));
        try {
            awaitBookWait(contender);
        } finally {
            probe.release();
        }
        assertSuccess(holder.get(20, TimeUnit.SECONDS));
        Outcome second = contender.get(20, TimeUnit.SECONDS);
        if (competing.equals("unlink")) {
            assertSuccess(second);
            assertNull(link(salary));
            assertEquals(0, count("voucher", "deleted='n'"));
        } else if (competing.equals("save")) {
            // The existing edit DTO has no employee/month; its constraint error
            // occurs only after the actual book lock, not concurrently with it.
            SQLException constraint = cause(second.error, SQLException.class);
            assertNotNull(constraint, "Save must reach an actual MySQL required-column constraint after waiting");
            assertTrue(Set.of(1048, 1364).contains(constraint.getErrorCode()),
                    "Missing belong_date must cause an actual MySQL required-column violation");
            assertTrue(constraint.getMessage().contains("belong_date"));
            assertEquals(1, count("employee_salary", "deleted='n'"));
            assertAccountingRows(1, 3, 1);
        } else {
            assertRejected(second);
            assertNotNull(link(salary));
            assertAccountingRows(1, 3, 1);
            assertEquals(new BigDecimal("100.00000"), amount(salary));
            assertEquals("n", deletion(salary));
        }
        assertFirstStatementIsBookLock("holder");
        assertFirstStatementIsBookLock("contender");
    }

    @Test
    void separateSalaryRowsForSameEmployeeCannotGenerateTwoLiveAccruals() throws Exception {
        String peer = book + "p";
        seedSalary(book, employee, peer);
        probe.pause("holder", VOUCHER_INSERT);
        Future<Outcome> first = start("holder", () -> salaries.generateVoucher(generate(book, salary, 2)));
        probe.awaitPaused();
        Future<Outcome> second = start("contender", () -> salaries.generateVoucher(generate(book, peer, 2)));
        try { awaitBookWait(second); } finally { probe.release(); }
        assertSuccess(first.get(20, TimeUnit.SECONDS));
        assertRejected(second.get(20, TimeUnit.SECONDS));
        assertNotNull(link(salary));
        assertNull(link(peer));
        assertAccountingRows(1, 3, 1);
    }

    @Test
    void generateWithOlderOuterRepeatableReadSnapshotSeesCommittedLivePeerAfterBookWait() throws Exception {
        String peer = book + "p";
        seedSalary(book, employee, peer);
        probe.pause("holder", VOUCHER_INSERT);
        Future<Outcome> first = start("holder", () -> salaries.generateVoucher(generate(book, salary, 2)));
        probe.awaitPaused();
        AtomicBoolean oldSnapshotProved = new AtomicBoolean();
        TransactionTemplate outer = new TransactionTemplate(context.getBean(DataSourceTransactionManager.class));
        outer.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        probe.expectedIsolations.put("contender", "REPEATABLE-READ");
        Future<Outcome> second = start("contender", () -> outer.execute(status -> {
            assertEquals("REPEATABLE-READ", jdbc.queryForObject("SELECT @@transaction_isolation", String.class));
            // A genuine ordinary consistent read creates the outer transaction's
            // old snapshot while the first generated header remains uncommitted.
            assertNull(link(salary));
            assertEquals(0L, count("voucher", "deleted='n'"));
            Message<String> generated = salaries.generateVoucher(generate(book, peer, 2));
            // The plain read remains old after the first transaction commits.
            // A rejected generate therefore proves its critical read was current.
            assertNull(link(salary));
            assertEquals(0L, count("voucher", "deleted='n'"));
            oldSnapshotProved.set(true);
            return generated;
        }));
        try { awaitBookWait(second); } finally { probe.release(); }
        assertSuccess(first.get(20, TimeUnit.SECONDS));
        assertRejected(second.get(20, TimeUnit.SECONDS));
        assertTrue(oldSnapshotProved.get(), "The outer ordinary read must demonstrably retain its old snapshot");
        assertNotNull(link(salary));
        assertNull(link(peer));
        assertAccountingRows(1, 3, 1);
        assertFirstStatementIsBookLock("contender");
    }

    @Test
    void unlinkWithOlderOuterRepeatableReadSnapshotDeletesNewVoucherAndItemsAfterBookWait() throws Exception {
        probe.seedCashFlowAfterHeader = true;
        probe.pause("holder", VOUCHER_INSERT);
        Future<Outcome> first = start("holder", () -> salaries.generateVoucher(generate(book, salary, 2)));
        probe.awaitPaused();
        AtomicBoolean oldSnapshotProved = new AtomicBoolean();
        TransactionTemplate outer = new TransactionTemplate(context.getBean(DataSourceTransactionManager.class));
        outer.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        probe.expectedIsolations.put("contender", "REPEATABLE-READ");
        Future<Outcome> unlinked = start("contender", () -> outer.execute(status -> {
            assertEquals("REPEATABLE-READ", jdbc.queryForObject("SELECT @@transaction_isolation", String.class));
            assertNull(link(salary));
            assertEquals(0L, count("voucher", "1=1"));
            assertEquals(0L, count("voucher_item", "1=1"));
            assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM voucher_item_cash_flow WHERE book_id=?", Long.class, book));
            oldSnapshotProved.set(true);
            return salaries.deleteVoucher(generate(book, salary, 2));
        }));
        try { awaitBookWait(unlinked); } finally { probe.release(); }
        assertSuccess(first.get(20, TimeUnit.SECONDS));
        assertSuccess(unlinked.get(20, TimeUnit.SECONDS));
        assertTrue(probe.cashFlowSeedObserved, "The real generated item must receive an actual cashflow relation in the holder transaction");
        assertTrue(oldSnapshotProved.get(), "The deleting outer transaction must establish its old consistent-read snapshot before waiting");
        assertNull(link(salary));
        assertEquals("n", deletion(salary));
        assertAccountingRows(1, 3, 1);
        assertEquals(0L, count("voucher", "deleted='n'"));
        assertEquals(1L, count("voucher", "deleted='y'"));
        assertEquals(0L, count("voucher_item", "deleted='n'"));
        assertEquals(3L, count("voucher_item", "deleted='y'"));
        assertEquals(1L, count("voucher_word", "1=1"), "Existing voucher number retention must be preserved");
        // VoucherItemCashFlow has no @TableLogic/deleted column; its original
        // mapper contract physically deletes relations for current item IDs.
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM voucher_item_cash_flow WHERE book_id=?", Long.class, book));
        assertFirstStatementIsBookLock("holder");
        assertFirstStatementIsBookLock("contender");
    }

    @Test
    void generateWaitsForUpdateAndUsesCommittedAmounts() throws Exception {
        probe.pause("holder", SALARY_UPDATE);
        Future<Outcome> update = start("holder", () -> salaries.update(change(book, salary, "150", "130")));
        probe.awaitPaused();
        Future<Outcome> generated = start("contender", () -> salaries.generateVoucher(generate(book, salary, 2)));
        try { awaitBookWait(generated); } finally { probe.release(); }
        assertSuccess(update.get(20, TimeUnit.SECONDS));
        assertSuccess(generated.get(20, TimeUnit.SECONDS));
        assertEquals(new BigDecimal("150.00"), jdbc.queryForObject("SELECT debit_amount FROM voucher WHERE book_id=? AND deleted='n'", BigDecimal.class, book));
        assertAccountingRows(1, 3, 1);
    }

    @Test
    void generateWaitsForDeleteAndDoesNotCreateOrphan() throws Exception {
        probe.pauseSalaryDeletion("holder");
        Future<Outcome> deleted = start("holder", () -> salaries.delete(ids(book, salary)));
        probe.awaitPaused();
        Future<Outcome> generated = start("contender", () -> salaries.generateVoucher(generate(book, salary, 2)));
        try { awaitBookWait(generated); } finally { probe.release(); }
        assertSuccess(deleted.get(20, TimeUnit.SECONDS));
        assertRejected(generated.get(20, TimeUnit.SECONDS));
        assertEquals("y", deletion(salary));
        assertAccountingRows(0, 0, 0);
    }

    @Test
    void oppositeBatchInputOrderUsesSameBookLockAndHasNoPartialMutation() throws Exception {
        String peer = book + "p";
        seedSalary(book, employee, peer);
        probe.pauseSalaryDeletion("holder");
        Future<Outcome> first = start("holder", () -> salaries.delete(ids(book, salary, peer)));
        probe.awaitPaused();
        Future<Outcome> second = start("contender", () -> salaries.delete(ids(book, peer, salary)));
        try { awaitBookWait(second); } finally { probe.release(); }
        assertSuccess(first.get(20, TimeUnit.SECONDS));
        assertRejected(second.get(20, TimeUnit.SECONDS));
        assertEquals("y", deletion(salary));
        assertEquals("y", deletion(peer));
        assertFirstStatementIsBookLock("holder");
        assertFirstStatementIsBookLock("contender");
    }

    @Test
    void anotherBookCanCommitWhileFirstBookGenerationIsPaused() throws Exception {
        String otherBook = PREFIX + "_" + sequence + "b";
        ownedBooks.add(otherBook);
        seedBook(otherBook);
        seedEmployee(otherBook, otherBook + "e");
        seedSalary(otherBook, otherBook + "e", otherBook + "s");
        seedTemplates(otherBook);
        probe.pause("holder", VOUCHER_INSERT);
        Future<Outcome> first = start("holder", () -> salaries.generateVoucher(generate(book, salary, 2)));
        probe.awaitPaused();
        Future<Outcome> independent = start("contender", () -> salaries.generateVoucher(generate(otherBook, otherBook + "s", 2)));
        try {
            assertSuccess(independent.get(10, TimeUnit.SECONDS));
            assertFalse(first.isDone(), "First book must still be held at the controlled pause");
        } catch (TimeoutException timeout) {
            dumpFixtureLockWaits();
            throw timeout;
        } finally { probe.release(); }
        assertSuccess(first.get(20, TimeUnit.SECONDS));
        assertNotNull(link(salary));
        assertNotNull(link(otherBook + "s"));
    }

    @Test
    void missingOwnVoucherBeyondExistingMaximumDoesNotBlockAnotherBookGeneration() throws Exception {
        String otherBook = seedOtherBook();
        // An actual existing header makes MAX(id) non-null. The missing FK lies
        // above that binary-collated maximum and would lock the upper gap in RR.
        jdbc.update("INSERT INTO voucher (id,word,word_head,word_num,book_id,company_name,voucher_year,voucher_month,voucher_date,status,deleted) "
                        + "VALUES (?,'记-0','记',0,?,'Payroll IT',?,?,?,'draft','n')",
                "0000_" + book, book, MONTH.getYear(), MONTH.getMonthValue(), java.sql.Date.valueOf(MONTH.atEndOfMonth()));
        String stale = "zzzz_" + book;
        String maximum = jdbc.queryForObject("SELECT MAX(id) FROM voucher", String.class);
        assertNotNull(maximum);
        assertTrue(stale.compareTo(maximum) > 0, "The own missing FK must be beyond the actual voucher ID maximum");
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM voucher WHERE id=?", Long.class, stale));
        jdbc.update("UPDATE employee_salary SET accrual_voucher_id=? WHERE id=? AND book_id=?", stale, salary, book);
        probe.pause("holder", VOUCHER_INSERT);
        Future<Outcome> first = start("holder", () -> salaries.generateVoucher(generate(book, salary, 2)));
        probe.awaitPaused();
        Future<Outcome> independent = start("contender", () -> salaries.generateVoucher(generate(otherBook, otherBook + "s", 2)));
        try {
            assertSuccess(independent.get(10, TimeUnit.SECONDS));
            assertFalse(first.isDone(), "Another book must commit while the first generated transaction stays paused");
        } catch (TimeoutException timeout) {
            dumpFixtureLockWaits();
            throw timeout;
        } finally { probe.release(); }
        assertSuccess(first.get(20, TimeUnit.SECONDS));
        assertNotEquals(stale, link(salary));
        assertNotNull(link(otherBook + "s"));
        assertAccountingRows(2, 3, 1); // One real prior fixture header plus the generated voucher.
        assertFirstStatementIsBookLock("holder");
        assertFirstStatementIsBookLock("contender");
    }

    @Test
    void anotherBookPreviewCanReplaceSalaryWhileFirstBookGenerationIsPaused() throws Exception {
        String otherBook = seedOtherBook();
        String prior = otherBook + "s";
        jdbc.update("INSERT INTO employee_salary_temp (id,employee_id,book_id,belong_date,pay_amount,personal_tax,total_amount,deleted) "
                        + "VALUES (?,?,?,?,120,20,100,'n')",
                otherBook + "t", otherBook + "e", otherBook, MONTH.toString());
        probe.pause("holder", VOUCHER_INSERT);
        Future<Outcome> first = start("holder", () -> salaries.generateVoucher(generate(book, salary, 2)));
        probe.awaitPaused();
        Future<Outcome> independent = start("contender", () -> {
            SalaryDetailPageDto dto = new SalaryDetailPageDto(); dto.setBookId(otherBook);
            return previews.createFinalDetail(dto);
        });
        try {
            assertSuccess(independent.get(10, TimeUnit.SECONDS));
            assertFalse(first.isDone(), "The first book's real generation must remain paused during the other book's replacement");
            assertEquals("y", deletion(prior), "Preview push must really delete the old salary row");
            assertEquals(2L, jdbc.queryForObject("SELECT COUNT(*) FROM employee_salary WHERE book_id=?", Long.class, otherBook));
            assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM employee_salary WHERE book_id=? AND deleted='n'", Long.class, otherBook));
            assertEquals(new BigDecimal("120.00000"), jdbc.queryForObject("SELECT pay_amount FROM employee_salary WHERE book_id=? AND deleted='n'", BigDecimal.class, otherBook));
        } finally { probe.release(); }
        assertSuccess(first.get(20, TimeUnit.SECONDS));
        assertNotNull(link(salary));
        assertAccountingRows(1, 3, 1);
        assertFirstStatementIsBookLock("holder");
        assertFirstStatementIsBookLock("contender");
    }

    private String seedOtherBook() {
        String otherBook = PREFIX + "_" + sequence + "b";
        ownedBooks.add(otherBook);
        seedBook(otherBook);
        seedEmployee(otherBook, otherBook + "e");
        seedSalary(otherBook, otherBook + "e", otherBook + "s");
        seedTemplates(otherBook);
        return otherBook;
    }

    @Test
    void mixedBookDeleteIsRejectedWithoutDeletingEitherFixture() {
        String otherBook = PREFIX + "_" + sequence + "b";
        ownedBooks.add(otherBook);
        seedBook(otherBook);
        seedEmployee(otherBook, otherBook + "e");
        seedSalary(otherBook, otherBook + "e", otherBook + "s");
        Outcome result = direct("direct", () -> salaries.delete(ids(book, salary, otherBook + "s")));
        assertRejected(result);
        assertEquals("n", deletion(salary));
        assertEquals("n", deletion(otherBook + "s"));
        assertFirstStatementIsBookLock("direct");
    }

    @Test
    void mismatchedRecordBookIsRejectedWithoutMutation() {
        String otherBook = PREFIX + "_" + sequence + "b";
        ownedBooks.add(otherBook);
        seedBook(otherBook);
        Outcome result = direct("direct", () -> salaries.update(change(otherBook, salary, "150", "130")));
        assertRejected(result);
        assertEquals(new BigDecimal("100.00000"), amount(salary));
        assertFirstStatementIsBookLock("direct");
    }

    @Test
    void actualZeroRowLinkUpdateRollsBackHeaderItemsWordAndSalaryMutation() {
        probe.fault = Fault.ZERO;
        Outcome result = direct("fault", () -> salaries.generateVoucher(generate(book, salary, 2)));
        assertRejected(result);
        assertTrue(probe.zeroObserved, "Actual EmployeeSalaryMapper update must return zero");
        assertAccountingRows(0, 0, 0);
        assertNull(link(salary));
        assertEquals("n", deletion(salary));
    }

    @Test
    void actualSqlLinkExceptionRollsBackHeaderItemsWordAndSalaryState() {
        probe.fault = Fault.SQL_EXCEPTION;
        Outcome result = direct("fault", () -> salaries.generateVoucher(generate(book, salary, 2)));
        assertNotNull(result.error, "Actual MySQL trigger must reject the final link update");
        SQLException signalled = cause(result.error, SQLException.class);
        assertNotNull(signalled, "Failure must come from the actual MySQL trigger");
        assertEquals("45000", signalled.getSQLState());
        assertEquals(1644, signalled.getErrorCode());
        assertTrue(signalled.getMessage().contains("IT4 actual link update failure"));
        assertTrue(probe.headerInserted, "Failure must follow the real voucher header insert");
        assertAccountingRows(0, 0, 0);
        assertNull(link(salary));
        assertEquals("n", deletion(salary));
    }

    @Test
    void unsuccessfulSaveResponseAfterActualVoucherWritesRollsBackEntireGeneration() {
        probe.fault = Fault.SAVE_RETURN_FAILURE;
        Outcome result = direct("fault", () -> salaries.generateVoucher(generate(book, salary, 2)));
        assertNull(result.error, "Failed callee response must return a reviewable service failure");
        assertRejected(result);
        assertTrue(probe.saveFailureBoundaryObserved, "Fault must follow the real successful save and all actual accounting rows");
        assertAccountingRows(0, 0, 0);
        assertNull(link(salary));
        assertEquals("n", deletion(salary));
    }

    @Test
    void failedTemplateLookupPreservesOwnStaleLinkAndCreatesNoAccountingRows() {
        String stale = book + "_missing";
        jdbc.update("UPDATE employee_salary SET accrual_voucher_id=? WHERE id=? AND book_id=?", stale, salary, book);
        jdbc.update("DELETE FROM voucher_template WHERE related_id=? AND code='fp_lwf'", book);
        Outcome result = direct("direct", () -> salaries.generateVoucher(generate(book, salary, 2)));
        assertNull(result.error, "Missing template must return a reviewable failure");
        assertRejected(result);
        assertEquals(stale, link(salary), "An unsuccessful generate must not clean stale salary history");
        assertAccountingRows(0, 0, 0);
        assertFirstStatementIsBookLock("direct");
    }

    private Object competing(String entry) {
        return switch (entry) {
            case "update" -> salaries.update(change(book, salary, "150", "130"));
            case "delete" -> salaries.delete(ids(book, salary));
            case "generate" -> salaries.generateVoucher(generate(book, salary, 2));
            case "unlink" -> salaries.deleteVoucher(generate(book, salary, 2));
            case "push" -> { SalaryDetailPageDto dto = new SalaryDetailPageDto(); dto.setBookId(book); yield previews.createFinalDetail(dto); }
            case "save" -> salaries.save(change(book, book + "new", "100", "80"));
            default -> throw new IllegalArgumentException(entry);
        };
    }

    private Future<Outcome> start(String role, Callable<?> operation) {
        return workers.submit(() -> direct(role, operation));
    }

    private Outcome direct(String role, Callable<?> operation) {
        ROLE.set(role);
        try { return new Outcome(operation.call(), null); }
        catch (Throwable error) { return new Outcome(null, error); }
        finally { ROLE.remove(); }
    }

    private record Outcome(Object value, Throwable error) { }

    private static void assertSuccess(Outcome result) {
        assertNull(result.error, () -> "Unexpected service error: " + result.error);
        assertInstanceOf(Message.class, result.value);
        assertEquals(Message.SUCCESS, ((Message<?>) result.value).getCode(), () -> "Service rejected: " + ((Message<?>) result.value).getMessage());
    }

    private static void assertRejected(Outcome result) {
        if (result.error != null) {
            assertNotNull(cause(result.error, BusinessException.class),
                    () -> "Expected a business rejection, not an unrelated failure: " + result.error);
            return;
        }
        assertInstanceOf(Message.class, result.value);
        assertNotEquals(Message.SUCCESS, ((Message<?>) result.value).getCode(), "Concurrent operation must not claim success");
    }

    private static <T extends Throwable> T cause(Throwable error, Class<T> type) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (error != null && visited.add(error)) {
            if (type.isInstance(error)) return type.cast(error);
            error = error.getCause();
        }
        return null;
    }

    private void awaitBookWait(Future<?> contender) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            Long holderId = probe.connections.get("holder"), contenderId = probe.connections.get("contender");
            if (holderId != null && contenderId != null) {
                assertNotEquals(holderId, contenderId, "Use distinct actual MySQL connections");
                Long waits = jdbc.queryForObject("""
                        SELECT COUNT(*) FROM performance_schema.data_lock_waits w
                        JOIN performance_schema.threads waiting ON waiting.THREAD_ID=w.REQUESTING_THREAD_ID
                        JOIN performance_schema.threads holding ON holding.THREAD_ID=w.BLOCKING_THREAD_ID
                        JOIN performance_schema.data_locks blocked ON blocked.ENGINE_LOCK_ID=w.BLOCKING_ENGINE_LOCK_ID
                        WHERE waiting.PROCESSLIST_ID=? AND holding.PROCESSLIST_ID=?
                          AND blocked.OBJECT_SCHEMA=? AND blocked.OBJECT_NAME='book'
                        """, Long.class, contenderId, holderId, database);
                if (waits != null && waits > 0) {
                    System.out.println("IT4 observed real book lock wait: holder=" + holderId + ", contender=" + contenderId);
                    return;
                }
            }
            if (contender.isDone()) fail("Competing write completed while generator transaction remained paused; no book mutex");
            new CountDownLatch(1).await(25, TimeUnit.MILLISECONDS);
        }
        fail("No actual book lock wait was observed within the controlled interleaving");
    }

    private void dumpFixtureLockWaits() {
        System.out.println("IT4 timeout diagnostics: database=" + database + ", roleConnections=" + probe.connections
                + ", actualMapperStatements=" + probe.statements);
        try {
            JdbcTemplate observer = new JdbcTemplate(dataSource);
            observer.setQueryTimeout(3);
            List<Map<String, Object>> waits = observer.queryForList("""
                    SELECT waiting.PROCESSLIST_ID AS requestingConnection, holding.PROCESSLIST_ID AS blockingConnection,
                           waiting.PROCESSLIST_INFO AS requestingSql, holding.PROCESSLIST_INFO AS blockingSql,
                           requested.OBJECT_SCHEMA AS requestingSchema, requested.OBJECT_NAME AS requestingObject,
                           requested.INDEX_NAME AS requestingIndex, requested.LOCK_TYPE AS requestingType,
                           requested.LOCK_MODE AS requestingMode, requested.LOCK_DATA AS requestingData,
                           blocked.OBJECT_SCHEMA AS blockingSchema, blocked.OBJECT_NAME AS blockingObject,
                           blocked.INDEX_NAME AS blockingIndex, blocked.LOCK_TYPE AS blockingType,
                           blocked.LOCK_MODE AS blockingMode, blocked.LOCK_DATA AS blockingData
                    FROM performance_schema.data_lock_waits w
                    JOIN performance_schema.data_locks requested
                      ON requested.ENGINE=w.ENGINE AND requested.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID
                    JOIN performance_schema.data_locks blocked
                      ON blocked.ENGINE=w.ENGINE AND blocked.ENGINE_LOCK_ID=w.BLOCKING_ENGINE_LOCK_ID
                    JOIN performance_schema.threads waiting ON waiting.THREAD_ID=w.REQUESTING_THREAD_ID
                    JOIN performance_schema.threads holding ON holding.THREAD_ID=w.BLOCKING_THREAD_ID
                    WHERE requested.OBJECT_SCHEMA=? AND blocked.OBJECT_SCHEMA=?
                    """, database, database);
            System.out.println("IT4 real fixture lock waits: " + waits);
        } catch (Throwable diagnosticsFailure) {
            // Diagnostics must never replace the original deterministic timeout.
            System.out.println("IT4 lock diagnostics unavailable: " + diagnosticsFailure.getClass().getName() + ": " + diagnosticsFailure.getMessage());
        }
        Long contenderId = probe.connections.get("contender");
        if (contenderId != null) {
            try {
                JdbcTemplate observer = new JdbcTemplate(dataSource);
                observer.setQueryTimeout(3);
                // This numeric ID is read from the actual mapper connection
                // whose catalog was already checked against this fixture DB.
                List<Map<String, Object>> plan = observer.queryForList("EXPLAIN FOR CONNECTION " + contenderId);
                System.out.println("IT4 actual blocking contender plan: connection=" + contenderId + ", plan=" + plan);
            } catch (Throwable explainFailure) {
                System.out.println("IT4 contender plan unavailable: " + explainFailure.getClass().getName() + ": " + explainFailure.getMessage());
            }
        }
    }

    private void assertFirstStatementIsBookLock(String role) {
        List<String> executed = probe.statements.get(role);
        assertNotNull(executed, "Expected actual SQL trace for " + role);
        assertTrue(executed.get(0).endsWith(".lockActiveBookId"), "First database action must be the production book lock: " + executed.get(0));
    }

    private long count(String table, String filter) {
        assertTrue(Set.of("employee_salary", "voucher", "voucher_item", "voucher_word").contains(table));
        return Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE book_id=? AND " + filter, Long.class, book));
    }

    private void assertAccountingRows(long headers, long items, long words) {
        assertEquals(headers, count("voucher", "1=1"), "Voucher headers");
        assertEquals(items, count("voucher_item", "1=1"), "Voucher detail rows");
        assertEquals(words, count("voucher_word", "1=1"), "Voucher number reservations");
    }

    private String link(String salaryId) { return jdbc.queryForObject("SELECT accrual_voucher_id FROM employee_salary WHERE id=?", String.class, salaryId); }
    private String deletion(String salaryId) { return jdbc.queryForObject("SELECT deleted FROM employee_salary WHERE id=?", String.class, salaryId); }
    private BigDecimal amount(String salaryId) { return jdbc.queryForObject("SELECT pay_amount FROM employee_salary WHERE id=?", BigDecimal.class, salaryId); }

    private static GenerateVoucherDto generate(String bookId, String salaryId, int type) {
        GenerateVoucherDto dto = new GenerateVoucherDto(); dto.setBookId(bookId); dto.setId(salaryId); dto.setVoucherType(type); return dto;
    }
    private static SalaryDetailChangeDto change(String bookId, String salaryId, String gross, String net) {
        SalaryDetailChangeDto dto = new SalaryDetailChangeDto(); dto.setBookId(bookId); dto.setId(salaryId);
        dto.setPayAmount(new BigDecimal(gross)); dto.setTotalAmount(new BigDecimal(net)); dto.setPersonalTax(new BigDecimal("20")); return dto;
    }
    private static ListIdsDto ids(String bookId, String... salaryIds) {
        ListIdsDto dto = new ListIdsDto(); dto.setBookId(bookId); dto.setListIds(List.of(salaryIds)); return dto;
    }

    private enum Fault { NONE, ZERO, SQL_EXCEPTION, SAVE_RETURN_FAILURE }

    @Intercepts({
            @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
            @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
    })
    private class SqlProbe implements Interceptor {
        private final Map<String, Long> connections = new ConcurrentHashMap<>();
        private final Map<String, List<String>> statements = new ConcurrentHashMap<>();
        private final Map<String, String> expectedIsolations = new ConcurrentHashMap<>();
        private volatile Fault fault = Fault.NONE;
        private volatile boolean headerInserted;
        private volatile boolean zeroObserved;
        private volatile boolean saveFailureBoundaryObserved;
        private volatile boolean seedCashFlowAfterHeader;
        private volatile boolean cashFlowSeedObserved;
        private volatile String pauseRole;
        private volatile String pauseStatement;
        private volatile boolean pauseDelete;
        private volatile CountDownLatch paused = new CountDownLatch(1);
        private volatile CountDownLatch resume = new CountDownLatch(1);
        private final AtomicBoolean consumed = new AtomicBoolean();

        void reset() { connections.clear(); statements.clear(); expectedIsolations.clear(); fault = Fault.NONE; headerInserted = false; zeroObserved = false; saveFailureBoundaryObserved = false; seedCashFlowAfterHeader = false; cashFlowSeedObserved = false; consumed.set(false); pauseRole = null; pauseStatement = null; pauseDelete = false; paused = new CountDownLatch(1); resume = new CountDownLatch(1); }
        void pause(String role, String statement) { pauseRole = role; pauseStatement = statement; }
        void pauseSalaryDeletion(String role) { pauseRole = role; pauseDelete = true; }
        void awaitPaused() throws InterruptedException { assertTrue(paused.await(15, TimeUnit.SECONDS), "Holder did not reach actual SQL pause"); }
        void release() { resume.countDown(); }

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            MappedStatement mapped = (MappedStatement) invocation.getArgs()[0];
            Executor executor = (Executor) invocation.getTarget();
            Connection connection = executor.getTransaction().getConnection();
            assertEquals(database, connection.getCatalog(), "Every mapper uses only the dedicated fixture database");
            String sql = mapped.getBoundSql(invocation.getArgs()[1]).getSql();
            Matcher qualifiedTable = Pattern.compile("(?i)\\b(?:FROM|JOIN|UPDATE|INTO)\\s+`?([A-Za-z0-9_]+)`?\\s*\\.\\s*`?[A-Za-z0-9_]+`?").matcher(sql);
            while (qualifiedTable.find()) {
                assertEquals(database, qualifiedTable.group(1), "Mapper cannot access a qualified table in another database");
            }
            String role = ROLE.get();
            if (role != null) {
                assertFalse(connection.getAutoCommit(), "All tested service SQL must run in an actual database transaction");
                long connectionId;
                try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT CONNECTION_ID()")) {
                    assertTrue(result.next()); connectionId = result.getLong(1);
                }
                String isolation;
                try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT @@transaction_isolation")) {
                    assertTrue(result.next()); isolation = result.getString(1);
                }
                assertEquals(expectedIsolations.getOrDefault(role, declaredSalaryIsolation), isolation,
                        "Read isolation on the actual service connection, including joined outer transactions");
                Long priorConnection = connections.putIfAbsent(role, connectionId);
                if (priorConnection == null) {
                    try (Statement statement = connection.createStatement()) { statement.execute("SET SESSION innodb_lock_wait_timeout=15"); }
                    System.out.println("IT4 observed actual transaction isolation: role=" + role + ", JDBC=" + isolation);
                } else {
                    assertEquals(priorConnection.longValue(), connectionId,
                            "Salary, voucher word/header and real MyBatis batch items must share one transaction connection");
                }
                statements.computeIfAbsent(role, ignored -> new CopyOnWriteArrayList<>()).add(mapped.getId());
            }
            Object result = invocation.proceed();
            if (mapped.getId().equals(VOUCHER_INSERT) && role != null) {
                headerInserted = true;
                if (seedCashFlowAfterHeader) {
                    assertEquals("holder", role, "Cashflow fixture hook is exclusive to the paused holder in outer-RR unlink");
                    seedRealGeneratedItemCashFlow(connection);
                }
                if (fault == Fault.ZERO) {
                    // A real same-transaction fixture change makes the real MP
                    // logical-delete UPDATE predicate return zero rows.
                    try (var statement = connection.prepareStatement("UPDATE employee_salary SET deleted='y' WHERE id=? AND book_id=?")) {
                        statement.setString(1, salary); statement.setString(2, book); assertEquals(1, statement.executeUpdate());
                    }
                } else if (fault == Fault.SQL_EXCEPTION) {
                    try (Statement statement = connection.createStatement()) { statement.execute("SET @payroll_it4_fail_link=1"); }
                }
            }
            if (fault == Fault.ZERO && headerInserted && mapped.getId().startsWith(EmployeeSalaryMapper.class.getName()) && result instanceof Number value && value.intValue() == 0) zeroObserved = true;
            boolean selected = mapped.getId().equals(pauseStatement)
                    || (pauseDelete && mapped.getId().startsWith(EmployeeSalaryMapper.class.getName())
                    && sql.matches("(?is)^\\s*UPDATE\\s+`?employee_salary`?\\s+SET\\b.*?\\bdeleted\\s*=\\s*'y'.*"));
            if (Objects.equals(role, pauseRole) && selected && consumed.compareAndSet(false, true)) {
                paused.countDown();
                assertTrue(resume.await(20, TimeUnit.SECONDS), "Controlled SQL pause was not released");
            }
            return result;
        }

        private void seedRealGeneratedItemCashFlow(Connection connection) throws SQLException {
            String itemId;
            BigDecimal amount;
            try (var statement = connection.prepareStatement("""
                    SELECT i.id,i.book_id,i.debit_amount,i.credit_amount
                    FROM voucher_item i JOIN voucher v ON v.id=i.voucher_id
                    WHERE v.book_id=? AND i.book_id=? AND v.deleted='n' AND i.deleted='n'
                    ORDER BY i.id
                    """)) {
                statement.setString(1, book); statement.setString(2, book);
                try (ResultSet rows = statement.executeQuery()) {
                    assertTrue(rows.next(), "Cashflow fixture must reference a row inserted by the real voucher save");
                    itemId = rows.getString("id");
                    assertNotNull(itemId);
                    assertEquals(book, rows.getString("book_id"));
                    BigDecimal debit = rows.getBigDecimal("debit_amount");
                    BigDecimal credit = rows.getBigDecimal("credit_amount");
                    amount = debit != null && debit.signum() > 0 ? debit : credit;
                    assertNotNull(amount);
                    assertTrue(amount.signum() > 0, "Use the actual generated item's positive amount");
                }
            }
            String cashFlowId = book + "cf";
            try (var statement = connection.prepareStatement("INSERT INTO voucher_item_cash_flow "
                    + "(id,book_id,voucher_item_id,cash_flow_item_code,cash_flow_balance,cash_flow_item_type) VALUES (?,?,?,?,?,0)")) {
                statement.setString(1, cashFlowId); statement.setString(2, book); statement.setString(3, itemId);
                statement.setString(4, "IT4.current.items"); statement.setBigDecimal(5, amount);
                assertEquals(1, statement.executeUpdate());
            }
            try (var statement = connection.prepareStatement("SELECT COUNT(*) FROM voucher_item_cash_flow WHERE id=? AND book_id=? AND voucher_item_id=?")) {
                statement.setString(1, cashFlowId); statement.setString(2, book); statement.setString(3, itemId);
                try (ResultSet rows = statement.executeQuery()) {
                    assertTrue(rows.next()); assertEquals(1L, rows.getLong(1));
                }
            }
            cashFlowSeedObserved = true;
        }
    }
}

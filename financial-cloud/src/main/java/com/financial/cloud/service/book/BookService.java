package com.financial.cloud.service.book;


import lombok.RequiredArgsConstructor;
import com.financial.cloud.service.standard.StandardSubjectCashFlowService;
import com.financial.cloud.service.config.ConfigCashFlowBalanceService;
import com.financial.cloud.service.config.ConfigInsuranceFundService;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.statement.StatementIncomeService;
import com.financial.cloud.service.statement.StatementBalanceSheetService;
import com.financial.cloud.domain.voucher.Voucher;
import com.financial.cloud.service.voucher.VoucherService;
import com.financial.cloud.service.voucher.VoucherTemplateService;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.dto.book.BookChangeDto;
import com.financial.cloud.dto.book.BookPageDto;
import com.financial.cloud.dto.book.BookVo;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.enums.error.BookBusinessExceptionEnum;
import com.financial.cloud.enums.book.BookStatusEnum;
import com.financial.cloud.enums.error.UsersBusinessCode;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.service.book.BookService;
import com.financial.cloud.domain.permissions.PermissionBook;
import com.financial.cloud.domain.idm.RoleMember;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.dto.book.BookSetupVo;
import com.financial.cloud.dto.book.OnboardingStatusVo;
import com.financial.cloud.service.permissions.PermissionBookService;
import com.financial.cloud.service.idm.RoleMemberService;
import com.financial.cloud.context.WebContext;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@RequiredArgsConstructor
@Service
public class BookService extends ServiceImpl<BookMapper, Book>{
    private final IdentifierGenerator identifierGenerator;

    private final BookMapper bookMapper;

    private final BookSubjectService bookSubjectService;

    private final ConfigCashFlowBalanceService configCashFlowBalanceService;

    private final StatementIncomeService statementIncomeService;

    private final StatementBalanceSheetService statementBalanceSheetService;

    private final ConfigSysService configSysService;

    private final VoucherService voucherService;

    private final VoucherTemplateService voucherTemplateService;

    private final StandardSubjectCashFlowService standardSubjectCashFlowService;

    private final PermissionBookService permissionBookService;

    private final RoleMemberService roleMemberService;

    private final ConfigInsuranceFundService configInsuranceFundService;

    public Message<Page<Book>> pageList(BookPageDto dto, String userId) {
        dto.setUserId(userId);
        Page<Book> page = bookMapper.pageList(dto.build(), dto);
        return new Message<>(Message.SUCCESS, page);
    }
    @Transactional
    public Message<String> save(BookChangeDto dto, UserInfo currentUser) {

        //校验账套名称是否重复
        checkIfTheNameExists(dto, false);

        dto.setId(identifierGenerator.nextId(dto).toString());

        // 账套配置参数初始化
        configSysService.initBooksConfig(dto.getId(),dto.getEnableDate().toString());

        // 社保公积金最低比例默认配置
        configInsuranceFundService.ensureDefaults(dto.getId());

        //账套科目
        bookSubjectService.initBookSubject(dto);

        //新增现金流量余额配置
        configCashFlowBalanceService.configCashFlowBalance(dto);

        //新增账套利润表配置
        statementIncomeService.initIncomeStatement(dto);

        //新增账套资产负债表配置
        statementBalanceSheetService.initBalanceSheet(dto);

        //新增默认科目和现金流量的关系
        standardSubjectCashFlowService.saveTemplateRelationships(dto.getId());
        
        //新增凭证模板
        voucherTemplateService.insertBookTemplate(dto.getId(), dto.getStandardId());

        //新增账套
        Book newBook = new Book();
        BeanUtil.copyProperties(dto, newBook);
        boolean saveResult = super.save(newBook);
        if (!saveResult) {
            return new Message<>(Message.FAIL, "新增失败");
        }
        // 顶栏账套列表来自 permission_book；创建人默认获得访问权 + 账套管理员角色
        if (currentUser != null && currentUser.getId() != null) {
            permissionBookService.save(new PermissionBook(currentUser.getId(), dto.getId()));
            RoleMember adminMember = new RoleMember(
                    ProductRoles.ADMINISTRATORS, currentUser.getId(), "USER", dto.getId());
            adminMember.setId(WebContext.genId());
            roleMemberService.save(adminMember);
        }
        return new Message<>(Message.SUCCESS, "新增成功");
    }
    @Transactional
    public Message<String> update(BookChangeDto dto, UserInfo currentUser) {
        requireBookAdministrator(currentUser, dto.getId());
        checkIfTheNameExists(dto, true);

        //新增现金流量余额配置
        configCashFlowBalanceService.configCashFlowBalance(dto);

        //更新账套
        Book booksUpdate = new Book();
        BeanUtil.copyProperties(dto, booksUpdate);
        boolean result = super.updateById(booksUpdate);
        return result ? new Message<>(Message.SUCCESS, "修改成功") : new Message<>(Message.FAIL, "修改失败");
    }

    private void checkIfTheNameExists(BookChangeDto dto, boolean isEdit) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getName, dto.getName());
        if (isEdit) {
            wrapper.ne(Book::getId, dto.getId());
        }
        List<Book> list = super.list(wrapper);
        if (ObjectUtils.isNotEmpty(list)) {
            throw new BusinessException(BookBusinessExceptionEnum.DUPLICATE_SETNAME_EXIST);
        }
    }
    @Transactional
    public Message<String> delete(ListIdsDto dto, UserInfo currentUser) {
        List<String> bookIds = dto.getListIds();
        for (String bookId : bookIds) {
            requireBookAdministrator(currentUser, bookId);
        }

        //校验是否为活跃状态
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getStatus, 1);
        wrapper.in(Book::getId, bookIds);
        List<Book> books = bookMapper.selectList(wrapper);
        if (ObjectUtils.isNotEmpty(books)) {
            throw new BusinessException(BookBusinessExceptionEnum.DISABLE_BEFORE_DELETE);
        }

        //封存账套不允许删除（归档留存语义）
        LambdaQueryWrapper<Book> sealedWrapper = new LambdaQueryWrapper<>();
        sealedWrapper.eq(Book::getStatus, BookStatusEnum.SEALED.getValue());
        sealedWrapper.in(Book::getId, bookIds);
        if (bookMapper.selectCount(sealedWrapper) > 0) {
            throw new BusinessException(BookBusinessExceptionEnum.SEALED_BOOK_DELETE);
        }

        // 有凭证的账套禁止硬删，引导封存留存
        for (String bookId : bookIds) {
            long voucherCount = voucherService.count(new LambdaQueryWrapper<Voucher>()
                    .eq(Voucher::getBookId, bookId));
            if (voucherCount > 0) {
                throw new BusinessException(BookBusinessExceptionEnum.BOOK_HAS_DATA_DELETE);
            }
        }

        //删除关联科目
        bookSubjectService.deleteByBookIds(bookIds);

        //删除现金流量余额配置
        configCashFlowBalanceService.deleteByBookIds(bookIds);

        //删除现金流量和科目的默认关系
        standardSubjectCashFlowService.deleteByBookIds(bookIds);

        //删除利润表配置及数据
    	statementIncomeService.deleteByBookIds(bookIds);

    	//删除资产负债表配置和数据
    	statementBalanceSheetService.deleteByBookIds(bookIds);

    	//删除科目余额表数据
    	statementBalanceSheetService.deleteByBookIds(bookIds);

    	//删除凭证模板及相关条目配置
    	voucherTemplateService.deleteByBookIds(bookIds);

        //删除凭证及相关条目
    	voucherService.deleteByBookIds(bookIds);

        //删除账套数据
        boolean result = super.removeByIds(bookIds);

        return result ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
    }

    /**
     * 封存账套（归档只读）：仅账套管理员可操作；封存后一切业务写操作被 BookSealGuard 拦截。
     */
    @Transactional
    public Message<String> seal(String bookId, UserInfo currentUser) {
        requireBookAdministrator(currentUser, bookId);
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            return new Message<>(Message.FAIL, "账套不存在");
        }
        Book update = new Book();
        update.setId(bookId);
        update.setStatus(BookStatusEnum.SEALED.getValue());
        boolean result = super.updateById(update);
        return result ? new Message<>(Message.SUCCESS, "封存成功") : new Message<>(Message.FAIL, "封存失败");
    }

    /**
     * 解除封存，恢复为启用状态。
     */
    @Transactional
    public Message<String> unseal(String bookId, UserInfo currentUser) {
        requireBookAdministrator(currentUser, bookId);
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            return new Message<>(Message.FAIL, "账套不存在");
        }
        Book update = new Book();
        update.setId(bookId);
        update.setStatus(BookStatusEnum.ACTIVE.getValue());
        boolean result = super.updateById(update);
        return result ? new Message<>(Message.SUCCESS, "已解除封存") : new Message<>(Message.FAIL, "解除封存失败");
    }

    /**
     * Caller must hold ROLE_ADMINISTRATORS for the target book.
     */
    public void requireBookAdministrator(UserInfo currentUser, String bookId) {
        if (currentUser == null || currentUser.getId() == null || currentUser.getId().isBlank()) {
            throw new BusinessException(UsersBusinessCode.PERMISSION_DENIED);
        }
        if (bookId == null || bookId.isBlank()) {
            throw new BusinessException(UsersBusinessCode.BOOK_REQUIRED);
        }
        if (permissionBookService.count(new LambdaQueryWrapper<PermissionBook>()
                .eq(PermissionBook::getUserId, currentUser.getId())
                .eq(PermissionBook::getBookId, bookId)) == 0) {
            throw new BusinessException(UsersBusinessCode.PERMISSION_DENIED);
        }
        long count = roleMemberService.count(new LambdaQueryWrapper<RoleMember>()
                .eq(RoleMember::getMemberId, currentUser.getId())
                .eq(RoleMember::getBookId, bookId)
                .eq(RoleMember::getType, "USER")
                .eq(RoleMember::getRoleId, ProductRoles.ADMINISTRATORS));
        if (count == 0) {
            throw new BusinessException(UsersBusinessCode.PERMISSION_DENIED);
        }
    }

    public boolean isBookAdministrator(UserInfo currentUser, String bookId) {
        if (currentUser == null || currentUser.getId() == null || currentUser.getId().isBlank()
                || bookId == null || bookId.isBlank()) {
            return false;
        }
        return permissionBookService.count(new LambdaQueryWrapper<PermissionBook>()
                .eq(PermissionBook::getUserId, currentUser.getId())
                .eq(PermissionBook::getBookId, bookId)) > 0
                && roleMemberService.count(new LambdaQueryWrapper<RoleMember>()
                .eq(RoleMember::getMemberId, currentUser.getId())
                .eq(RoleMember::getBookId, bookId)
                .eq(RoleMember::getType, "USER")
                .eq(RoleMember::getRoleId, ProductRoles.ADMINISTRATORS)) > 0;
    }
    public List<BookVo> listBooks(String userId) {
        return bookMapper.listBooks(userId);
    }

    public OnboardingStatusVo onboardingStatus(String userId) {
        return new OnboardingStatusVo(listBooks(userId).isEmpty());
    }

    @Transactional
    public Message<BookSetupVo> setup(BookChangeDto dto, UserInfo currentUser) {
        if (!listBooks(currentUser.getId()).isEmpty()) {
            return new Message<>(Message.FAIL, "账套已存在，无需初始化");
        }
        Message<String> saveResult = save(dto, currentUser);
        if (saveResult.getCode() != Message.SUCCESS) {
            return new Message<>(saveResult.getCode(), saveResult.getMessage());
        }
        return Message.ok(new BookSetupVo(dto.getId()));
    }

}

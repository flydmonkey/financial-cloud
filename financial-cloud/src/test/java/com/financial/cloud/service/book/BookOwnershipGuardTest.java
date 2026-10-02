package com.financial.cloud.service.book;

import com.financial.cloud.authn.support.AuthorizationUtils;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.config.ConfigCashFlowBalance;
import com.financial.cloud.domain.statement.StatementRules;
import com.financial.cloud.dto.config.ConfigCashFlowChangeDto;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.dto.journal.JournalEntryDto;
import com.financial.cloud.dto.book.SubjectPageDto;
import com.financial.cloud.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BookOwnershipGuardTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final BookOwnershipGuard guard = new BookOwnershipGuard(jdbc);
    private final UserInfo user = user();

    private static UserInfo user() {
        UserInfo u = new UserInfo(); u.setId("user"); u.setBookId("A"); return u;
    }
    private MockedStatic<AuthorizationUtils> session() {
        MockedStatic<AuthorizationUtils> auth = mockStatic(AuthorizationUtils.class);
        auth.when(AuthorizationUtils::getUserInfo).thenReturn(user);
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq("user"), eq("A"))).thenReturn(1L);
        return auth;
    }
    @SuppressWarnings("unchecked")
    private void owner(String id, String book) {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(id))).thenReturn(List.of(book));
    }
    @Test void deniesMissingMembership() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq("user"), eq("B"))).thenReturn(0L);
        assertThrows(BusinessException.class, () -> guard.requireAccess(user, "B"));
    }
    @Test void rejectsForeignDetail() {
        try (var ignored = session()) {
            owner("foreign", "B");
            assertThrows(BusinessException.class, () -> guard.checkRequest("voucher", "foreign"));
        }
    }
    @Test void rejectsMixedBatchBeforeMutation() {
        try (var ignored = session()) {
            owner("own", "A"); owner("foreign", "B");
            ListIdsDto dto = new ListIdsDto(); dto.setListIds(List.of("own", "foreign"));
            assertThrows(BusinessException.class, () -> guard.checkRequest("fixed_asset", dto));
        }
    }
    @Test void rejectsNestedCashFlowRecord() {
        try (var ignored = session()) {
            owner("foreign", "B");
            ConfigCashFlowBalance row = new ConfigCashFlowBalance(); row.setId("foreign");
            ConfigCashFlowChangeDto dto = new ConfigCashFlowChangeDto(); dto.setCashFlowItemDtos(List.of(row));
            assertThrows(BusinessException.class, () -> guard.checkRequest("config_cash_flow_balance", dto));
        }
    }
    @Test void rejectsRuleWithoutClientBookId() {
        try (var ignored = session()) {
            owner("foreign", "B");
            StatementRules row = new StatementRules(); row.setId("foreign");
            assertThrows(BusinessException.class, () -> guard.checkRequest("statement_rules", List.of(row)));
        }
    }
    @Test void rejectsForeignAccountReference() {
        try (var ignored = session()) {
            owner("foreign", "B");
            JournalEntryDto dto = new JournalEntryDto(); dto.setAccId("foreign");
            assertThrows(BusinessException.class, () -> guard.checkRequest("journal_entry", dto));
        }
    }
    @Test void bindsMissingQueryScope() {
        try (var ignored = session()) {
            SubjectPageDto dto = new SubjectPageDto();
            guard.checkRequest("book_subject", dto);
            assertEquals("A", dto.getBookId());
        }
    }
    @Test void rejectsForgedQueryScope() {
        try (var ignored = session()) {
            SubjectPageDto dto = new SubjectPageDto(); dto.setBookId("B");
            assertThrows(BusinessException.class, () -> guard.checkRequest("book_subject", dto));
        }
    }
    @Test void disallowsUnregisteredSqlIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> guard.checkRequest("voucher; DELETE FROM book", "id"));
    }
    @Test void rejectsStoredForeignAccountEvenWhenRequestOmitsReference() {
        try (var ignored = session()) {
            owner("own", "A"); owner("foreign", "B");
            when(jdbc.queryForList("SELECT * FROM journal_entry WHERE id=?", "own"))
                    .thenReturn(List.of(java.util.Map.of("acc_id", "foreign")));
            assertThrows(BusinessException.class, () -> guard.checkRequest("journal_entry", "own"));
        }
    }
}

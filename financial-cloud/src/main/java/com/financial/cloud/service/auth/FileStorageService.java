package com.financial.cloud.service.auth;


import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.domain.auth.FileStorage;
import com.financial.cloud.repository.auth.FileStorageMapper;
import com.financial.cloud.service.auth.FileStorageService;

import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class FileStorageService  extends ServiceImpl<FileStorageMapper,FileStorage>{
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final com.financial.cloud.service.book.BookOwnershipGuard bookOwnershipGuard;

    public FileStorage requireReadable(String id) {
        com.financial.cloud.domain.idm.UserInfo user = com.financial.cloud.authn.support.AuthorizationUtils.getUserInfo();
        FileStorage file = getById(id);
        if (user == null || file == null) denied();
        java.util.List<String> books = jdbc.query("SELECT book_id FROM voucher_attachment WHERE file_id=? UNION ALL SELECT book_id FROM expense_claim_attachment WHERE file_id=?",
                (rs, row) -> rs.getString(1), id, id);
        if (!books.isEmpty()) {
            bookOwnershipGuard.requireAccess(user, user.getBookId());
            for (String book : books) if (!java.util.Objects.equals(book, user.getBookId())) denied();
        } else if (!java.util.Objects.equals(user.getUsername(), file.getCreatedBy())
                && !java.util.Objects.equals(user.getId(), file.getCreatedBy())) {
            denied();
        }
        return file;
    }

    public void requireDeletable(String id) {
        FileStorage file = requireReadable(id);
        // Linked files are deleted through their business attachment service only.
        Long linked = jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM voucher_attachment WHERE file_id=?) + (SELECT COUNT(*) FROM expense_claim_attachment WHERE file_id=?)", Long.class, id, id);
        if (linked > 0) denied();
        com.financial.cloud.domain.idm.UserInfo user = com.financial.cloud.authn.support.AuthorizationUtils.getUserInfo();
        if (!java.util.Objects.equals(user.getUsername(), file.getCreatedBy()) && !java.util.Objects.equals(user.getId(), file.getCreatedBy())) denied();
    }

    private void denied() {
        throw new com.financial.cloud.exception.BusinessException(com.financial.cloud.enums.error.UsersBusinessCode.PERMISSION_DENIED);
    }

	private final FileStorageMapper fileStorageMapper;

	public FileStorageMapper getMapper() {
		return fileStorageMapper;
	}

}

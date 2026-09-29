package com.financial.cloud.service.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.financial.cloud.domain.auth.FileStorage;
import com.financial.cloud.domain.expense.ExpenseClaim;
import com.financial.cloud.domain.expense.ExpenseClaimAttachment;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.expense.ExpenseClaimAttachmentMapper;
import com.financial.cloud.repository.expense.ExpenseClaimMapper;
import com.financial.cloud.service.auth.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 报销单票据附件：上传绑定报销单、列表、下载、删除。
 * 二进制存 file_storage（category='expense'），本服务只管关联与状态守卫：
 * 仅暂存/已拒绝的报销单可增删附件，下载不限状态。
 */
@Service
@RequiredArgsConstructor
public class ExpenseClaimAttachmentService {

    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf", "image/png", "image/jpeg", "image/jpg", "image/webp", "application/ofd");

    private final ExpenseClaimAttachmentMapper attachmentMapper;
    private final ExpenseClaimMapper expenseClaimMapper;
    private final FileStorageService fileStorageService;

    public List<ExpenseClaimAttachment> listByClaim(String claimId, String bookId) {
        return attachmentMapper.selectList(Wrappers.<ExpenseClaimAttachment>lambdaQuery()
                .eq(ExpenseClaimAttachment::getClaimId, claimId)
                .eq(ExpenseClaimAttachment::getBookId, bookId)
                .orderByAsc(ExpenseClaimAttachment::getSortIndex)
                .orderByAsc(ExpenseClaimAttachment::getCreatedDate));
    }

    @Transactional
    public ExpenseClaimAttachment upload(MultipartFile file, String claimId, UserInfo operator) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的票据文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(400, "票据不能超过 10MB");
        }
        String contentType = StringUtils.defaultString(file.getContentType(), "").toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(400, "仅支持 PDF / 图片（PNG、JPG、WEBP）/ OFD 票据");
        }
        ExpenseClaim claim = requireClaim(claimId, operator.getBookId());
        if (!ExpenseClaim.STATUS_DRAFT.equals(claim.getClaimStatus())
                && !ExpenseClaim.STATUS_REJECTED.equals(claim.getClaimStatus())) {
            throw new BusinessException(400, "仅暂存或已拒绝的报销单可上传票据");
        }

        FileStorage storage = new FileStorage();
        storage.setFileName(file.getOriginalFilename());
        storage.setContentType(contentType);
        storage.setContentSize(file.getSize());
        storage.setCategory("expense");
        storage.setCreatedBy(operator.getUsername());
        try {
            storage.setDataStored(file.getBytes());
        } catch (IOException e) {
            throw new BusinessException(500, "票据读取失败：" + e.getMessage());
        }
        fileStorageService.save(storage);

        Long sort = attachmentMapper.selectCount(Wrappers.<ExpenseClaimAttachment>lambdaQuery()
                .eq(ExpenseClaimAttachment::getClaimId, claimId));
        ExpenseClaimAttachment attachment = ExpenseClaimAttachment.builder()
                .bookId(operator.getBookId())
                .claimId(claimId)
                .fileId(storage.getId())
                .fileName(file.getOriginalFilename())
                .contentSize(file.getSize())
                .contentType(contentType)
                .sortIndex(sort.intValue())
                .build();
        attachmentMapper.insert(attachment);
        return attachment;
    }

    /** 下载：校验附件属于当前账套后返回文件存储记录（含二进制） */
    public FileStorage download(String attachmentId, String bookId) {
        ExpenseClaimAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null || !bookId.equals(attachment.getBookId())) {
            throw new BusinessException(404, "附件不存在或无权访问");
        }
        FileStorage storage = fileStorageService.getById(attachment.getFileId());
        if (storage == null || storage.getDataStored() == null) {
            throw new BusinessException(404, "附件文件已丢失");
        }
        return storage;
    }

    @Transactional
    public void deleteAttachment(String attachmentId, String bookId) {
        ExpenseClaimAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null || !bookId.equals(attachment.getBookId())) {
            throw new BusinessException(404, "附件不存在或无权访问");
        }
        ExpenseClaim claim = requireClaim(attachment.getClaimId(), bookId);
        if (!ExpenseClaim.STATUS_DRAFT.equals(claim.getClaimStatus())
                && !ExpenseClaim.STATUS_REJECTED.equals(claim.getClaimStatus())) {
            throw new BusinessException(400, "仅暂存或已拒绝的报销单可删除票据");
        }
        attachmentMapper.deleteById(attachmentId);
        fileStorageService.removeById(attachment.getFileId());
    }

    private ExpenseClaim requireClaim(String claimId, String bookId) {
        ExpenseClaim claim = expenseClaimMapper.selectById(claimId);
        if (claim == null || !bookId.equals(claim.getBookId())) {
            throw new BusinessException(404, "报销单不存在或不属于当前账套");
        }
        return claim;
    }
}

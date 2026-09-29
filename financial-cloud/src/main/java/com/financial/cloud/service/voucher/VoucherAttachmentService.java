package com.financial.cloud.service.voucher;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.domain.auth.FileStorage;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.voucher.Voucher;
import com.financial.cloud.domain.voucher.VoucherAttachment;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.voucher.VoucherAttachmentMapper;
import com.financial.cloud.repository.voucher.VoucherMapper;
import com.financial.cloud.service.auth.FileStorageService;
import com.financial.cloud.service.book.BookSealGuard;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.util.DateUtils;
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
 * 凭证附件：上传绑定凭证、列表、下载、删除。
 * 二进制存 file_storage（category='voucher'），本服务只管关联与权限。
 * 对应 docs/product/20-gap-analysis.md §3.2。
 */
@Service
@RequiredArgsConstructor
public class VoucherAttachmentService extends ServiceImpl<VoucherAttachmentMapper, VoucherAttachment> {

    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf", "image/png", "image/jpeg", "image/jpg", "image/webp", "application/ofd");

    private final VoucherMapper voucherMapper;
    private final FileStorageService fileStorageService;
    private final ConfigSysService configSysService;
    private final BookSealGuard bookSealGuard;

    public List<VoucherAttachment> listByVoucher(String voucherId, String bookId) {
        return list(Wrappers.<VoucherAttachment>lambdaQuery()
                .eq(VoucherAttachment::getVoucherId, voucherId)
                .eq(VoucherAttachment::getBookId, bookId)
                .orderByAsc(VoucherAttachment::getSortIndex)
                .orderByAsc(VoucherAttachment::getCreatedDate));
    }

    @Transactional
    public VoucherAttachment upload(MultipartFile file, String voucherId, UserInfo operator) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的附件文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(400, "附件不能超过 10MB");
        }
        String contentType = StringUtils.defaultString(file.getContentType(), "").toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(400, "仅支持 PDF / 图片（PNG、JPG、WEBP）/ OFD 附件");
        }
        Voucher voucher = requireBookVoucher(voucherId, operator.getBookId());
        bookSealGuard.assertWritable(operator.getBookId());
        requireOpenPeriod(voucher);

        FileStorage storage = new FileStorage();
        storage.setFileName(file.getOriginalFilename());
        storage.setContentType(contentType);
        storage.setContentSize(file.getSize());
        storage.setCategory("voucher");
        storage.setCreatedBy(operator.getUsername());
        try {
            storage.setDataStored(file.getBytes());
        } catch (IOException e) {
            throw new BusinessException(500, "附件读取失败：" + e.getMessage());
        }
        fileStorageService.save(storage);

        VoucherAttachment attachment = new VoucherAttachment();
        attachment.setBookId(operator.getBookId());
        attachment.setVoucherId(voucherId);
        attachment.setFileId(storage.getId());
        attachment.setFileName(file.getOriginalFilename());
        attachment.setContentSize(file.getSize());
        attachment.setContentType(contentType);
        attachment.setSortIndex((int) count(Wrappers.<VoucherAttachment>lambdaQuery()
                .eq(VoucherAttachment::getVoucherId, voucherId)));
        save(attachment);
        return attachment;
    }

    /** 下载：校验附件属于当前账套后返回文件存储记录（含二进制）。 */
    public FileStorage download(String attachmentId, String bookId) {
        VoucherAttachment attachment = getById(attachmentId);
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
        VoucherAttachment attachment = getById(attachmentId);
        if (attachment == null || !bookId.equals(attachment.getBookId())) {
            throw new BusinessException(404, "附件不存在或无权访问");
        }
        Voucher voucher = requireBookVoucher(attachment.getVoucherId(), bookId);
        bookSealGuard.assertWritable(bookId);
        requireOpenPeriod(voucher);
        removeById(attachmentId);
        fileStorageService.removeById(attachment.getFileId());
    }

    private Voucher requireBookVoucher(String voucherId, String bookId) {
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null || !bookId.equals(voucher.getBookId())) {
            throw new BusinessException(404, "凭证不存在或不属于当前账套");
        }
        return voucher;
    }

    /** 已结账期间不允许变更附件（与凭证期间锁同口径） */
    private void requireOpenPeriod(Voucher voucher) {
        if (voucher.getVoucherDate() == null) {
            return;
        }
        String currentTerm = configSysService.getCurrentTerm(voucher.getBookId());
        String voucherTerm = DateUtils.format(voucher.getVoucherDate(), DateUtils.FORMAT_DATE_YYYY_MM);
        if (StringUtils.isNotBlank(currentTerm) && currentTerm.compareTo(voucherTerm) > 0) {
            throw new BusinessException(400, "已结账期间不允许变更附件（当前开放账期 " + currentTerm + "）");
        }
    }
}

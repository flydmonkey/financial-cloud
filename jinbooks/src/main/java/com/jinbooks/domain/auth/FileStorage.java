package com.jinbooks.domain.auth;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@NoArgsConstructor
@TableName("file_storage")
public class FileStorage {

    @TableId(type = IdType.ASSIGN_ID)
    String id;

	@JsonIgnore
    byte[] dataStored ;

    @JsonIgnore
    @TableField(exist = false)
    MultipartFile uploadFile;

    String fileName;

    String contentType;

    long contentSize;

    String category;

    @TableField(exist = false)
    String imageBase64;

    /**
     * åå»ºè?
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    protected String createdBy;

    /**
     * åå»ºæ¶é´
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    protected Date createdDate;
}

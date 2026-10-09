package com.rentnest.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_record")
public class FileRecord {
    public static final String VISIBILITY_PUBLIC = "PUBLIC";
    public static final String VISIBILITY_PRIVATE = "PRIVATE";

    public static final String STORAGE_MINIO = "MINIO";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String uuid;
    private String originalName;
    private String mimeType;
    private Long sizeBytes;
    private String sha256;
    private String storageType;
    private String storagePath;
    private String visibility;
    private Long ownerId;
    private LocalDateTime createdAt;
}

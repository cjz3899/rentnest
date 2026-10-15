package com.rentnest.file.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileResp {
    /** file_record.id，业务表关联用（如 house_media.file_id） */
    private final Long id;
    private final String uuid;
    private final String originalName;
    private final String mimeType;
    private final Long sizeBytes;
    private final String visibility;
    private final String url;
}

package com.rentnest.file.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileResp {
    private final String uuid;
    private final String originalName;
    private final String mimeType;
    private final Long sizeBytes;
    private final String visibility;
    private final String url;
}

package com.rentnest.rental.dto;

import lombok.Data;

@Data
public class HouseMediaResp {
    /** 媒体记录ID */
    private Long id;
    /** 文件ID（关联 file_record.id） */
    private Long fileId;
    /** 文件 UUID */
    private String uuid;
    /** 媒体类型：IMAGE / VIDEO */
    private String mediaType;
    /** 排序值，越小越靠前 */
    private Integer sortOrder;
    /** 访问 URL */
    private String url;
}
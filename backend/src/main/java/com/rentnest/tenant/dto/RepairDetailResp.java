package com.rentnest.tenant.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RepairDetailResp {
    private Long id;
    private Long userId;
    private String userNickname;
    private Long houseId;
    private String houseTitle;
    private String location;
    private String description;
    private String contactPhone;
    private String supplement;
    private String status;
    private String handlerRemark;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
    /** 故障图片 URL 列表 */
    private List<String> mediaUrls;
}

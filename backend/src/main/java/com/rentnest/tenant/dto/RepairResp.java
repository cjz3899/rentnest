package com.rentnest.tenant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RepairResp {
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
}

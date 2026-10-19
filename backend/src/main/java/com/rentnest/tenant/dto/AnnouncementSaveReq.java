package com.rentnest.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnnouncementSaveReq {
    @NotBlank(message = "标题不能为空")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    /** NOTICE 公告 / FAQ 常见问题 */
    @NotBlank(message = "类型不能为空")
    private String type;

    /** PUBLISHED / UNPUBLISHED */
    @NotBlank(message = "状态不能为空")
    private String status;

    private Boolean isPinned;
}

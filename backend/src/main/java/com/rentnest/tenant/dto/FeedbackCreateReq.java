package com.rentnest.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class FeedbackCreateReq {
    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 1000)
    private String content;

    @Size(max = 500)
    private String problemDesc;

    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String contactPhone;

    /** 图片附件：文件上传接口返回的 file_record.id 列表 */
    private List<Long> fileIds;
}

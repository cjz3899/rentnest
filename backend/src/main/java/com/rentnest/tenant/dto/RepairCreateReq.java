package com.rentnest.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RepairCreateReq {
    /** 关联房源，可空 */
    private Long houseId;

    @NotBlank(message = "问题位置不能为空")
    @Size(max = 100)
    private String location;

    @NotBlank(message = "问题描述不能为空")
    @Size(max = 1000)
    private String description;

    @NotBlank(message = "联系方式不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String contactPhone;

    @Size(max = 500)
    private String supplement;

    /** 故障图片：文件上传接口返回的 file_record.id 列表 */
    private List<Long> fileIds;
}

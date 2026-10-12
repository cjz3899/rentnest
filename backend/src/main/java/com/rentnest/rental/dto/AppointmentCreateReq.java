package com.rentnest.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AppointmentCreateReq {
    /** 房源ID */
    @NotNull(message = "请选择房源")
    private Long houseId;

    /** 联系人姓名 */
    @NotBlank(message = "姓名不能为空")
    @Size(max = 50)
    private String contactName;

    /** 联系方式（11 位手机号） */
    @NotBlank(message = "联系方式不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String contactPhone;

    /** 预计入住时间 */
    private LocalDate expectedMoveInDate;

    /** 租赁周期，如 12个月 */
    @Size(max = 50)
    private String leaseTerm;

    /** 其他备注 */
    @Size(max = 500)
    private String remark;
}
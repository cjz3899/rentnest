package com.rentnest.pservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class OrderCreateReq {
    @NotNull(message = "请选择商品")
    private Long productId;

    @NotNull(message = "请填写数量")
    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 999, message = "单次下单数量不能超过 999")
    private Integer quantity;

    @NotBlank(message = "收货人不能为空")
    @Size(max = 50)
    private String receiverName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String receiverPhone;

    @NotBlank(message = "配送地址不能为空")
    @Size(max = 200)
    private String deliveryAddress;

    @Size(max = 500)
    private String remark;
}

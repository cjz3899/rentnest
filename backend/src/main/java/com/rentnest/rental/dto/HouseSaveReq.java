package com.rentnest.rental.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class HouseSaveReq {
    /** 房间名称 */
    @NotBlank(message = "房间名称不能为空")
    @Size(max = 100)
    private String title;

    /** 楼栋编号 */
    @NotBlank(message = "楼栋不能为空")
    @Size(max = 50)
    private String building;

    /** 所在楼层 */
    @Size(max = 20)
    private String floorNo;

    /** 朝向 */
    @Size(max = 20)
    private String orientation;

    /** 房型，如 一室一厅 */
    @NotBlank(message = "房型不能为空")
    @Size(max = 50)
    private String layout;

    /** 面积（㎡） */
    @NotNull(message = "面积不能为空")
    @DecimalMin(value = "1.0", message = "面积必须大于 0")
    private BigDecimal area;

    /** 月租金（元） */
    @NotNull(message = "租金不能为空")
    @DecimalMin(value = "0.01", message = "租金必须大于 0")
    private BigDecimal rent;

    /** 押金（元） */
    @DecimalMin(value = "0", message = "押金不能为负")
    private BigDecimal deposit;

    /** 房源介绍 */
    private String description;
    /** 租赁周期说明 */
    private String leaseTerm;
    /** 入住要求 */
    private String moveInRequirement;
    /** 注意事项 */
    private String notes;

    /** 配套设施列表 */
    private List<String> facilities;

    /** 媒体资源列表（图片/视频） */
    @Valid
    private List<MediaReq> media;

    @Data
    public static class MediaReq {
        /** 文件ID（关联 file_record.id） */
        @NotNull(message = "文件 id 不能为空")
        private Long fileId;
        /** 媒体类型：IMAGE / VIDEO */
        @NotBlank(message = "媒体类型不能为空")
        private String mediaType;
    }
}
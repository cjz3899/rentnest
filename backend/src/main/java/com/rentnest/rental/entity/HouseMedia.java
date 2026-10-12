package com.rentnest.rental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("house_media")
public class HouseMedia {
    /** 媒体类型：图片 */
    public static final String TYPE_IMAGE = "IMAGE";
    /** 媒体类型：视频 */
    public static final String TYPE_VIDEO = "VIDEO";

    /** 主键ID（雪花ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 房源ID */
    private Long houseId;
    /** 文件ID（关联 file_record.id） */
    private Long fileId;
    /** 媒体类型：IMAGE / VIDEO */
    private String mediaType;
    /** 排序值，越小越靠前 */
    private Integer sortOrder;
}
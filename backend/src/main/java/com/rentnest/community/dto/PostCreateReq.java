package com.rentnest.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PostCreateReq {
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过 200 字")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(max = 5000, message = "内容不能超过 5000 字")
    private String content;

    /** 帖子图片：文件上传接口返回的 file_record.id 列表 */
    private List<Long> fileIds;
}

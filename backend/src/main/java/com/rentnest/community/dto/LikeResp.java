package com.rentnest.community.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LikeResp {
    private final boolean liked;
    private final Integer likeCount;
}

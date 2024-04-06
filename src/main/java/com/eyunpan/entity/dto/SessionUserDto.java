package com.eyunpan.entity.dto;

import lombok.Data;

@Data
public class SessionUserDto {
    private String nickName;
    private String userId;
    private Boolean isAdmin;
    private String avatar;
}

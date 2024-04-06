package com.eyunpan.entity.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户空间信息
 */
@Data
public class UserSpaceDto implements Serializable {
    private Long useSpace;
    private Long totalSpace;

}

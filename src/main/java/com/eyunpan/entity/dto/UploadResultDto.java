package com.eyunpan.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * 上传文件时的响应
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class UploadResultDto implements Serializable {
    private String fileId;
    private String status;
}

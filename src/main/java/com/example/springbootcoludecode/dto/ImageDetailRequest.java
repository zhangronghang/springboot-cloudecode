package com.example.springbootcoludecode.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "图片详情查询请求")
public class ImageDetailRequest {
    @ApiModelProperty(value = "图片记录 ID", required = true, example = "66a1b2c3d4e5f67890123456")
    private String id;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}

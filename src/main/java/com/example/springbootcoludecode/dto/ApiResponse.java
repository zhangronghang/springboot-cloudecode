package com.example.springbootcoludecode.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "统一接口响应")
public class ApiResponse {
    @ApiModelProperty(value = "状态码：200 成功、400 参数错误、500 服务端错误", example = "200")
    private int code;
    @ApiModelProperty(value = "响应消息", example = "success")
    private String message;
    @ApiModelProperty(value = "业务数据；失败时为 null")
    private Object data;

    public ApiResponse() {}

    public ApiResponse(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static ApiResponse success(Object data) {
        return new ApiResponse(200, "success", data);
    }

    public static ApiResponse error(int code, String message) {
        return new ApiResponse(code, message, null);
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}

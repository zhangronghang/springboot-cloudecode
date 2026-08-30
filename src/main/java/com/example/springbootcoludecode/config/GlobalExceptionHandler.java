package com.example.springbootcoludecode.config;

import com.example.springbootcoludecode.dto.ApiResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ApiResponse.error(400, "上传文件大小超过限制");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse handleIllegalArgument(IllegalArgumentException e) {
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ApiResponse handleMissingRequestValue(Exception e) {
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse handleValidation(MethodArgumentNotValidException e) {
        return ApiResponse.error(400, e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse handleException(Exception e) {
        return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
    }
}

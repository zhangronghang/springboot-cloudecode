package com.example.springbootcoludecode.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

public class ImagePageRequest {
    @NotBlank(message = "informationId 不能为空")
    private String informationId;
    @Min(value = 1, message = "page 必须大于 0")
    private Integer page = 1;
    @Min(value = 1, message = "size 必须大于 0")
    private Integer size = 10;

    public String getInformationId() { return informationId; }
    public void setInformationId(String informationId) { this.informationId = informationId; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}

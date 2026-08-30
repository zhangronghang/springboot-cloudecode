package com.example.springbootcoludecode.dto;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

public class ImageBatchDeleteRequest {
    @NotBlank(message = "informationId 不能为空")
    private String informationId;
    @NotEmpty(message = "imageIds 不能为空")
    private List<String> imageIds;

    public String getInformationId() { return informationId; }
    public void setInformationId(String informationId) { this.informationId = informationId; }
    public List<String> getImageIds() { return imageIds; }
    public void setImageIds(List<String> imageIds) { this.imageIds = imageIds; }
}

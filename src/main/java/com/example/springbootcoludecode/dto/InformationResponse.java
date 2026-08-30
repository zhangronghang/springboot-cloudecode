package com.example.springbootcoludecode.dto;

public class InformationResponse {
    private String id, title, description, tags, uploader, provinceCode, cityCode, districtCode, createTime, uploadTime;
    private int imageCount;
    private PublicImageResponse coverImage;
    public String getId() { return id; } public void setId(String value) { id = value; }
    public String getTitle() { return title; } public void setTitle(String value) { title = value; }
    public String getDescription() { return description; } public void setDescription(String value) { description = value; }
    public String getTags() { return tags; } public void setTags(String value) { tags = value; }
    public String getUploader() { return uploader; } public void setUploader(String value) { uploader = value; }
    public String getProvinceCode() { return provinceCode; } public void setProvinceCode(String value) { provinceCode = value; }
    public String getCityCode() { return cityCode; } public void setCityCode(String value) { cityCode = value; }
    public String getDistrictCode() { return districtCode; } public void setDistrictCode(String value) { districtCode = value; }
    public String getCreateTime() { return createTime; } public void setCreateTime(String value) { createTime = value; }
    public String getUploadTime() { return uploadTime; } public void setUploadTime(String value) { uploadTime = value; }
    public int getImageCount() { return imageCount; } public void setImageCount(int value) { imageCount = value; }
    public PublicImageResponse getCoverImage() { return coverImage; } public void setCoverImage(PublicImageResponse value) { coverImage = value; }
}

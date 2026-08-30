package com.example.springbootcoludecode.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "image_metadata")
public class ImageMetadata {
    @Id
    private String id;
    private String title;
    private String description;
    private String tags;
    private String uploader;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String createTime;
    private String uploadTime;
    private List<ImageItem> images = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getUploader() { return uploader; }
    public void setUploader(String uploader) { this.uploader = uploader; }
    public String getProvinceCode() { return provinceCode; }
    public void setProvinceCode(String provinceCode) { this.provinceCode = provinceCode; }
    public String getCityCode() { return cityCode; }
    public void setCityCode(String cityCode) { this.cityCode = cityCode; }
    public String getDistrictCode() { return districtCode; }
    public void setDistrictCode(String districtCode) { this.districtCode = districtCode; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getUploadTime() { return uploadTime; }
    public void setUploadTime(String uploadTime) { this.uploadTime = uploadTime; }
    public List<ImageItem> getImages() { return images; }
    public void setImages(List<ImageItem> images) { this.images = images == null ? new ArrayList<>() : images; }
}

package com.example.springbootcoludecode.dto;

public class ImageUpdateRequest {
    private String id;
    private String title;
    private String description;
    private String tags;
    private String uploader;

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
}

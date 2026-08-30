package com.example.springbootcoludecode.dto;

public class PublicImageResponse {
    private String imageId;
    private String fileName;
    private long fileSize;
    private String contentType;
    private int width;
    private int height;
    private String createTime;
    private String thumbnailUrl;
    private String originalUrl;
    public String getImageId() { return imageId; } public void setImageId(String value) { imageId = value; }
    public String getFileName() { return fileName; } public void setFileName(String value) { fileName = value; }
    public long getFileSize() { return fileSize; } public void setFileSize(long value) { fileSize = value; }
    public String getContentType() { return contentType; } public void setContentType(String value) { contentType = value; }
    public int getWidth() { return width; } public void setWidth(int value) { width = value; }
    public int getHeight() { return height; } public void setHeight(int value) { height = value; }
    public String getCreateTime() { return createTime; } public void setCreateTime(String value) { createTime = value; }
    public String getThumbnailUrl() { return thumbnailUrl; } public void setThumbnailUrl(String value) { thumbnailUrl = value; }
    public String getOriginalUrl() { return originalUrl; } public void setOriginalUrl(String value) { originalUrl = value; }
}

package com.example.springbootcoludecode.entity;

public class ImageItem {
    private String imageId;
    private String originalGridFsFileId;
    private String thumbnailGridFsFileId;
    private String fileName;
    private long fileSize;
    private String contentType;
    private int width;
    private int height;
    private String createTime;

    public String getImageId() { return imageId; }
    public void setImageId(String imageId) { this.imageId = imageId; }
    public String getOriginalGridFsFileId() { return originalGridFsFileId; }
    public void setOriginalGridFsFileId(String originalGridFsFileId) { this.originalGridFsFileId = originalGridFsFileId; }
    public String getThumbnailGridFsFileId() { return thumbnailGridFsFileId; }
    public void setThumbnailGridFsFileId(String thumbnailGridFsFileId) { this.thumbnailGridFsFileId = thumbnailGridFsFileId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}

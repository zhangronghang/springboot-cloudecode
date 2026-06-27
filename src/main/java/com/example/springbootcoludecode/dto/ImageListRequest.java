package com.example.springbootcoludecode.dto;

public class ImageListRequest {
    private int page = 1;
    private int size = 10;
    private String tag;
    private String uploader;

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getUploader() { return uploader; }
    public void setUploader(String uploader) { this.uploader = uploader; }
}

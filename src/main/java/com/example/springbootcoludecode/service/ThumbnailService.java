package com.example.springbootcoludecode.service;

import org.springframework.web.multipart.MultipartFile;

public interface ThumbnailService {
    ProcessedImage process(MultipartFile file);

    class ProcessedImage {
        private final byte[] originalBytes, thumbnailBytes;
        private final String contentType;
        private final int width, height;
        public ProcessedImage(byte[] originalBytes, byte[] thumbnailBytes, String contentType, int width, int height) { this.originalBytes = originalBytes; this.thumbnailBytes = thumbnailBytes; this.contentType = contentType; this.width = width; this.height = height; }
        public byte[] getOriginalBytes() { return originalBytes; }
        public byte[] getThumbnailBytes() { return thumbnailBytes; }
        public String getContentType() { return contentType; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }
}

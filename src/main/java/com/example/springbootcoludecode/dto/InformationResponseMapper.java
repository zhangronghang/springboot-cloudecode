package com.example.springbootcoludecode.dto;

import com.example.springbootcoludecode.entity.ImageItem;
import com.example.springbootcoludecode.entity.ImageMetadata;

public final class InformationResponseMapper {
    private InformationResponseMapper() { }
    public static InformationResponse toInformation(ImageMetadata source) {
        InformationResponse target = new InformationResponse();
        target.setId(source.getId()); target.setTitle(source.getTitle()); target.setDescription(source.getDescription()); target.setTags(source.getTags()); target.setUploader(source.getUploader()); target.setProvinceCode(source.getProvinceCode()); target.setCityCode(source.getCityCode()); target.setDistrictCode(source.getDistrictCode()); target.setCreateTime(source.getCreateTime()); target.setUploadTime(source.getUploadTime());
        int count = source.getImages() == null ? 0 : source.getImages().size(); target.setImageCount(count);
        if (count > 0) target.setCoverImage(toImage(source.getId(), source.getImages().get(0)));
        return target;
    }
    public static PublicImageResponse toImage(String informationId, ImageItem source) {
        PublicImageResponse target = new PublicImageResponse();
        target.setImageId(source.getImageId()); target.setFileName(source.getFileName()); target.setFileSize(source.getFileSize()); target.setContentType(source.getContentType()); target.setWidth(source.getWidth()); target.setHeight(source.getHeight()); target.setCreateTime(source.getCreateTime());
        String base = "/api/information/" + informationId + "/images/" + source.getImageId();
        target.setThumbnailUrl(base + "/thumbnail"); target.setOriginalUrl(base + "/original");
        return target;
    }
}

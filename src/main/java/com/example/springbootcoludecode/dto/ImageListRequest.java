package com.example.springbootcoludecode.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "图片列表查询条件")
public class ImageListRequest {
    @ApiModelProperty(value = "页码，从 1 开始", example = "1")
    private int page = 1;
    @ApiModelProperty(value = "每页数量", example = "10")
    private int size = 10;
    @ApiModelProperty(value = "标签关键词，模糊匹配", example = "风景")
    private String tag;
    @ApiModelProperty(value = "上传者，精确匹配", example = "zhangsan")
    private String uploader;
    @ApiModelProperty(value = "省级行政区划代码，精确匹配", example = "110000")
    private String provinceCode;
    @ApiModelProperty(value = "市级行政区划代码，非空时精确匹配", example = "110100")
    private String cityCode;
    @ApiModelProperty(value = "区县级行政区划代码，精确匹配", example = "110101")
    private String districtCode;

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getUploader() { return uploader; }
    public void setUploader(String uploader) { this.uploader = uploader; }
    public String getProvinceCode() { return provinceCode; }
    public void setProvinceCode(String provinceCode) { this.provinceCode = provinceCode; }
    public String getCityCode() { return cityCode; }
    public void setCityCode(String cityCode) { this.cityCode = cityCode; }
    public String getDistrictCode() { return districtCode; }
    public void setDistrictCode(String districtCode) { this.districtCode = districtCode; }
}

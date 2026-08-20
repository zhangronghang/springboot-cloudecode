package com.example.springbootcoludecode.controller;

import com.example.springbootcoludecode.dto.*;
import com.example.springbootcoludecode.service.ImageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
@Api(tags = "图片管理")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @PostMapping("/upload")
    @ApiOperation(value = "上传图片", notes = "以 multipart/form-data 提交图片文件和备注信息。")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 200, message = "上传成功"),
            @io.swagger.annotations.ApiResponse(code = 400, message = "图片文件或标题为空")
    })
    public ApiResponse upload(@ApiParam(value = "图片文件", required = true) @RequestParam("file") MultipartFile file,
                              @ApiParam(value = "图片标题", required = true) @RequestParam("title") String title,
                              @ApiParam("图片描述") @RequestParam(value = "description", required = false) String description,
                              @ApiParam(value = "标签，使用英文逗号分隔", example = "风景,旅行") @RequestParam(value = "tags", required = false) String tags,
                              @ApiParam(value = "上传者", example = "zhangsan") @RequestParam(value = "uploader", required = false) String uploader) {
        return imageService.upload(file, title, description, tags, uploader);
    }

    @PostMapping("/list")
    @ApiOperation(value = "分页查询图片", notes = "支持按标签模糊匹配和上传者精确匹配，按上传时间倒序返回。")
    public ApiResponse list(@RequestBody ImageListRequest request) {
        return imageService.list(request);
    }

    @PostMapping("/detail")
    @ApiOperation(value = "查询图片详情", notes = "返回图片元数据和图片内容的 Base64 编码。")
    public ApiResponse detail(@RequestBody ImageDetailRequest request) {
        return imageService.detail(request);
    }

    @PostMapping("/update")
    @ApiOperation(value = "更新图片", notes = "使用 multipart/form-data 提交；除 id 外其余参数均可选，仅更新已提供的字段。")
    public ApiResponse update(@ApiParam(value = "图片记录 ID", required = true) @RequestParam("id") String id,
                              @ApiParam("替换后的图片文件") @RequestParam(value = "file", required = false) MultipartFile file,
                              @ApiParam("图片标题") @RequestParam(value = "title", required = false) String title,
                              @ApiParam("图片描述") @RequestParam(value = "description", required = false) String description,
                              @ApiParam(value = "标签，使用英文逗号分隔", example = "风景,旅行") @RequestParam(value = "tags", required = false) String tags,
                              @ApiParam(value = "上传者", example = "zhangsan") @RequestParam(value = "uploader", required = false) String uploader) {
        return imageService.update(id, file, title, description, tags, uploader);
    }

    @PostMapping("/delete")
    @ApiOperation(value = "删除图片", notes = "同时删除图片文件和元数据记录。")
    public ApiResponse delete(@RequestBody ImageDeleteRequest request) {
        return imageService.delete(request);
    }
}

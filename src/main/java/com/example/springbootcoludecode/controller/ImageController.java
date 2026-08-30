package com.example.springbootcoludecode.controller;

import com.example.springbootcoludecode.dto.*;
import com.example.springbootcoludecode.service.ImageService;
import com.example.springbootcoludecode.service.InformationImageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/information")
@Api(tags = "信息管理")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @Autowired
    private InformationImageService informationImageService;

    @PostMapping("/upload")
    @ApiOperation(value = "创建足迹并上传首张图片", notes = "以 multipart/form-data 提交图片文件和备注信息；创建时必须且只能上传一张 JPEG 或 PNG 图片。单文件最大 50MB，请求最大 55MB；provinceCode、cityCode、districtCode 均可选，缺失或空白保存为空字符串。每个足迹最多保存 50 张图片。")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 200, message = "上传成功"),
            @io.swagger.annotations.ApiResponse(code = 400, message = "图片文件或标题为空")
    })
    public ApiResponse upload(@ApiParam(value = "图片文件，只能上传一张", required = true) @RequestParam("file") MultipartFile[] files,
                              @ApiParam(value = "图片标题", required = true) @RequestParam("title") String title,
                              @ApiParam(value = "省级行政区划代码，可选；缺失或空白保存为空字符串", example = "110000") @RequestParam(value = "provinceCode", required = false) String provinceCode,
                              @ApiParam(value = "市级行政区划代码，可选；缺失或空白保存为空字符串", example = "110100") @RequestParam(value = "cityCode", required = false) String cityCode,
                              @ApiParam(value = "区县级行政区划代码，可选；缺失或空白保存为空字符串", example = "110101") @RequestParam(value = "districtCode", required = false) String districtCode,
                              @ApiParam("图片描述") @RequestParam(value = "description", required = false) String description,
                              @ApiParam(value = "标签，使用英文逗号分隔", example = "风景,旅行") @RequestParam(value = "tags", required = false) String tags,
                              @ApiParam(value = "上传者", example = "zhangsan") @RequestParam(value = "uploader", required = false) String uploader) {
        if (files == null || files.length != 1 || files[0] == null || files[0].isEmpty()) {
            return ApiResponse.error(400, "创建足迹时必须且只能上传一张图片");
        }
        return imageService.upload(files[0], title, provinceCode, cityCode, districtCode, description, tags, uploader);
    }

    @PostMapping("/list")
    @ApiOperation(value = "分页查询信息图片", notes = "支持按标签模糊匹配，以及上传者、省级、市级行政区划代码精确匹配和区县级行政区划代码精确匹配，按上传时间倒序返回。")
    public ApiResponse list(@RequestBody ImageListRequest request) {
        return imageService.list(request);
    }

    @PostMapping("/detail")
    @ApiOperation(value = "查询足迹元数据详情", notes = "只返回公开足迹元数据和 imageCount；图片请使用图片分页接口查询。")
    public ApiResponse detail(@RequestBody ImageDetailRequest request) {
        return imageService.detail(request);
    }

    @PostMapping("/update")
    @ApiOperation(value = "更新足迹元数据", notes = "使用 multipart/form-data 提交；除 id 外其余参数均可选，仅更新已提供的元数据字段，不接收或替换图片；三个行政区划代码不允许修改。")
    public ApiResponse update(@ApiParam(value = "图片记录 ID", required = true) @RequestParam("id") String id,
                              @ApiParam("图片标题") @RequestParam(value = "title", required = false) String title,
                              @ApiParam("图片描述") @RequestParam(value = "description", required = false) String description,
                              @ApiParam(value = "标签，使用英文逗号分隔", example = "风景,旅行") @RequestParam(value = "tags", required = false) String tags,
                              @ApiParam(value = "上传者", example = "zhangsan") @RequestParam(value = "uploader", required = false) String uploader) {
        return imageService.update(id, title, description, tags, uploader);
    }

    @PostMapping("/delete")
    @ApiOperation(value = "删除足迹", notes = "删除足迹元数据，并尝试清理其 images 数组中全部原图与缩略图文件。")
    public ApiResponse delete(@RequestBody ImageDeleteRequest request) {
        return imageService.delete(request);
    }

    @PostMapping("/image/add")
    @ApiOperation(value = "新增足迹图片", notes = "向指定足迹每次只能新增一张 JPEG 或 PNG 图片。单文件最大 50MB、请求最大 55MB，单个足迹最多 50 张图片。")
    public ApiResponse addImage(@RequestParam("informationId") String informationId,
                                @RequestParam("file") MultipartFile[] files) {
        if (files == null || files.length != 1 || files[0] == null || files[0].isEmpty()) {
            return ApiResponse.error(400, "每次只能新增一张图片");
        }
        return informationImageService.addImage(informationId, files[0]);
    }

    @PostMapping("/image/delete")
    @ApiOperation(value = "批量删除足迹图片", notes = "按稳定 imageId 快速删除同一足迹中的一张或多张图片，不直接接收 GridFS ID。")
    public ApiResponse deleteImages(@Valid @RequestBody ImageBatchDeleteRequest request) {
        return informationImageService.deleteImages(request);
    }

    @PostMapping("/image/list")
    @ApiOperation(value = "分页查询足迹图片", notes = "按图片添加顺序分页返回缩略图地址和原图永久访问地址；没有图片时返回 code=200 和消息“该足迹没有图片”。")
    public ApiResponse listImages(@Valid @RequestBody ImagePageRequest request) {
        return informationImageService.listImages(request);
    }
}

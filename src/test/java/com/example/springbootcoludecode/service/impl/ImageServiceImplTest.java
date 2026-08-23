package com.example.springbootcoludecode.service.impl;

import com.example.springbootcoludecode.dto.*;
import com.example.springbootcoludecode.entity.ImageMetadata;
import com.example.springbootcoludecode.service.ImageService;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ImageServiceImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private GridFsTemplate gridFsTemplate;

    @InjectMocks
    private ImageServiceImpl imageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void uploadContract_shouldAcceptAdministrativeCodesAndExposeNewMetadataFields() {
        Method uploadMethod = Arrays.stream(ImageService.class.getMethods())
                .filter(method -> method.getName().equals("upload"))
                .filter(method -> method.getParameterCount() == 8)
                .findFirst()
                .orElse(null);
        Set<String> propertyNames = Arrays.stream(ImageMetadata.class.getMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
        Set<String> listPropertyNames = Arrays.stream(ImageListRequest.class.getMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertNotNull(uploadMethod, "上传服务必须接收 provinceCode、cityCode 和 districtCode");
        assertTrue(propertyNames.contains("getProvinceCode"));
        assertTrue(propertyNames.contains("getCityCode"));
        assertTrue(propertyNames.contains("getDistrictCode"));
        assertTrue(propertyNames.contains("getCreateTime"));
        assertTrue(listPropertyNames.contains("getProvinceCode"));
        assertTrue(listPropertyNames.contains("getCityCode"));
        assertTrue(listPropertyNames.contains("getDistrictCode"));
    }

    @Test
    void upload_shouldStoreFileAndSaveMetadata() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSize()).thenReturn(1024L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));

        org.bson.types.ObjectId gridFsId = new org.bson.types.ObjectId();
        when(gridFsTemplate.store(any(InputStream.class), anyString(), anyString())).thenReturn(gridFsId);

        ApiResponse response = imageService.upload(file, "测试标题", "  province-A  ", "  city-A  ", "  district-B  ", "测试描述", "风景,旅游", "zhangsan");

        assertEquals(200, response.getCode());
        assertTrue(response.getData() instanceof ImageMetadata);
        ImageMetadata meta = (ImageMetadata) response.getData();
        assertEquals("测试标题", meta.getTitle());
        assertEquals("测试描述", meta.getDescription());
        assertEquals("风景,旅游", meta.getTags());
        assertEquals("zhangsan", meta.getUploader());
        assertEquals("province-A", meta.getProvinceCode());
        assertEquals("city-A", meta.getCityCode());
        assertEquals("district-B", meta.getDistrictCode());
        assertEquals("1024", meta.getFileSize());
        assertEquals("test.jpg", meta.getFileName());
        assertEquals(gridFsId.toString(), meta.getGridFsFileId());
        assertEquals(meta.getCreateTime(), meta.getUploadTime());
        assertTrue(meta.getCreateTime().matches("\\d{14}"));
        assertNotNull(meta.getId());

        verify(mongoTemplate).save(any(ImageMetadata.class));
    }

    @Test
    void upload_shouldRejectBlankCityAndDistrictCodes() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1});
        when(gridFsTemplate.store(any(InputStream.class), anyString(), anyString()))
                .thenReturn(new org.bson.types.ObjectId());

        ApiResponse missingCity = imageService.upload(file, "标题", "11", " ", "1101", null, null, null);
        ApiResponse missingDistrict = imageService.upload(file, "标题", "11", "1101", " ", null, null, null);
        ApiResponse nullCity = imageService.upload(file, "标题", "11", null, "1101", null, null, null);
        ApiResponse nullDistrict = imageService.upload(file, "标题", "11", "1101", null, null, null, null);

        assertEquals(400, missingCity.getCode());
        assertEquals("市级行政区划代码不能为空", missingCity.getMessage());
        assertEquals(400, missingDistrict.getCode());
        assertEquals("区县级行政区划代码不能为空", missingDistrict.getMessage());
        assertEquals("市级行政区划代码不能为空", nullCity.getMessage());
        assertEquals("区县级行政区划代码不能为空", nullDistrict.getMessage());
        verifyNoInteractions(gridFsTemplate, mongoTemplate);
    }

    @Test
    void upload_shouldStoreEmptyProvinceAndAcceptNonStandardCodes() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1});
        when(gridFsTemplate.store(any(InputStream.class), anyString(), anyString()))
                .thenReturn(new org.bson.types.ObjectId());

        ApiResponse response = imageService.upload(file, "标题", "   ", " city-X ", " district-Y ", null, null, null);

        assertEquals(200, response.getCode());
        ImageMetadata metadata = (ImageMetadata) response.getData();
        assertEquals("", metadata.getProvinceCode());
        assertEquals("city-X", metadata.getCityCode());
        assertEquals("district-Y", metadata.getDistrictCode());
    }

    @Test
    void list_shouldReturnPaginatedResults() {
        ImageListRequest request = new ImageListRequest();
        request.setPage(1);
        request.setSize(10);

        ImageMetadata meta = new ImageMetadata();
        meta.setId("507f1f77bcf86cd799439011");
        meta.setTitle("测试");
        meta.setUploader("zhangsan");
        meta.setUploadTime("2026-06-27 10:30:00");

        when(mongoTemplate.count(any(Query.class), eq(ImageMetadata.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(ImageMetadata.class))).thenReturn(Collections.singletonList(meta));

        ApiResponse response = imageService.list(request);

        assertEquals(200, response.getCode());
        assertTrue(response.getData() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertEquals(1L, data.get("total"));
        assertEquals(1, data.get("page"));
        assertEquals(10, data.get("size"));
        assertNotNull(data.get("records"));
    }

    @Test
    void list_shouldFilterByTagAndUploader() {
        ImageListRequest request = new ImageListRequest();
        request.setPage(1);
        request.setSize(5);
        request.setTag("风景");
        request.setUploader("zhangsan");

        when(mongoTemplate.count(any(Query.class), eq(ImageMetadata.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(ImageMetadata.class))).thenReturn(Collections.emptyList());

        ApiResponse response = imageService.list(request);

        assertEquals(200, response.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertEquals(0L, data.get("total"));
    }

    @Test
    void list_shouldCombineExactAdministrativeCodeFiltersWithExistingFilters() throws Exception {
        ImageListRequest request = new ImageListRequest();
        request.setTag("风景");
        request.setUploader("zhangsan");
        request.setProvinceCode("province-A");
        request.setCityCode(" city-A ");
        request.setDistrictCode("district-B");

        when(mongoTemplate.count(any(Query.class), eq(ImageMetadata.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(ImageMetadata.class))).thenReturn(Collections.emptyList());

        imageService.list(request);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).count(queryCaptor.capture(), eq(ImageMetadata.class));
        assertEquals("province-A", queryCaptor.getValue().getQueryObject().getString("provinceCode"));
        assertEquals("city-A", queryCaptor.getValue().getQueryObject().getString("cityCode"));
        assertEquals("district-B", queryCaptor.getValue().getQueryObject().getString("districtCode"));
        assertEquals("zhangsan", queryCaptor.getValue().getQueryObject().getString("uploader"));
        assertTrue(queryCaptor.getValue().getQueryObject().containsKey("tags"));
    }

    @Test
    void list_shouldIgnoreBlankCityCodeFilter() throws Exception {
        ImageListRequest request = new ImageListRequest();
        request.setCityCode("   ");
        when(mongoTemplate.count(any(Query.class), eq(ImageMetadata.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(ImageMetadata.class))).thenReturn(Collections.emptyList());

        imageService.list(request);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).count(queryCaptor.capture(), eq(ImageMetadata.class));
        assertFalse(queryCaptor.getValue().getQueryObject().containsKey("cityCode"));
    }

    @Test
    void detail_shouldReturnMetadataAndBase64() throws Exception {
        ImageDetailRequest request = new ImageDetailRequest();
        request.setId("507f1f77bcf86cd799439011");

        ImageMetadata meta = new ImageMetadata();
        meta.setId("507f1f77bcf86cd799439011");
        meta.setTitle("测试图片");
        meta.setGridFsFileId(null); // 无图片文件的场景
        when(mongoTemplate.findById("507f1f77bcf86cd799439011", ImageMetadata.class)).thenReturn(meta);

        ApiResponse response = imageService.detail(request);

        assertEquals(200, response.getCode());
        assertTrue(response.getData() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        ImageMetadata returnedMetadata = (ImageMetadata) data.get("metadata");
        assertEquals("测试图片", returnedMetadata.getTitle());
        assertNull(returnedMetadata.getCityCode());
    }

    @Test
    void detail_shouldReturnMetadataWithNullBase64WhenGridFsFileNotFound() {
        ImageDetailRequest request = new ImageDetailRequest();
        request.setId("507f1f77bcf86cd799439011");

        ImageMetadata meta = new ImageMetadata();
        meta.setId("507f1f77bcf86cd799439011");
        meta.setTitle("测试");
        meta.setGridFsFileId("507f1f77bcf86cd799439022");
        when(mongoTemplate.findById("507f1f77bcf86cd799439011", ImageMetadata.class)).thenReturn(meta);
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(null);

        ApiResponse response = imageService.detail(request);

        assertEquals(200, response.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertNotNull(data.get("metadata"));
        assertNull(data.get("imageBase64"));
    }

    @Test
    void detail_shouldReturn404WhenNotFound() {
        ImageDetailRequest request = new ImageDetailRequest();
        request.setId("nonexistent");
        when(mongoTemplate.findById("nonexistent", ImageMetadata.class)).thenReturn(null);

        ApiResponse response = imageService.detail(request);

        assertEquals(400, response.getCode());
        assertEquals("记录不存在", response.getMessage());
    }

    @Test
    void update_shouldUpdateFields() throws Exception {
        ImageMetadata existing = new ImageMetadata();
        existing.setId("507f1f77bcf86cd799439011");
        existing.setTitle("旧标题");
        existing.setDescription("旧描述");
        existing.setTags("旧标签");
        existing.setProvinceCode("province-A");
        existing.setCityCode("city-A");
        existing.setDistrictCode("district-B");
        existing.setCreateTime("20260101120000");
        existing.setUploadTime("20000101120000");
        when(mongoTemplate.findById("507f1f77bcf86cd799439011", ImageMetadata.class)).thenReturn(existing);

        ApiResponse response = imageService.update("507f1f77bcf86cd799439011", null, "新标题", "新描述", null, null);

        assertEquals(200, response.getCode());
        ImageMetadata updated = (ImageMetadata) response.getData();
        assertEquals("新标题", updated.getTitle());
        assertEquals("新描述", updated.getDescription());
        assertEquals("旧标签", updated.getTags());
        assertEquals("province-A", updated.getProvinceCode());
        assertEquals("city-A", updated.getCityCode());
        assertEquals("district-B", updated.getDistrictCode());
        assertEquals("20260101120000", updated.getCreateTime());
        assertTrue(updated.getUploadTime().matches("\\d{14}"));
        assertNotEquals("20000101120000", updated.getUploadTime());
        verify(mongoTemplate).save(existing);
    }

    @Test
    void update_shouldSupportMetadataWithoutCityCode() throws Exception {
        ImageMetadata existing = new ImageMetadata();
        existing.setId("507f1f77bcf86cd799439011");
        existing.setTitle("旧标题");
        existing.setCreateTime("20260101120000");
        existing.setUploadTime("20000101120000");
        when(mongoTemplate.findById("507f1f77bcf86cd799439011", ImageMetadata.class)).thenReturn(existing);

        ApiResponse response = imageService.update("507f1f77bcf86cd799439011", null, "新标题", null, null, null);

        assertEquals(200, response.getCode());
        assertNull(((ImageMetadata) response.getData()).getCityCode());
        verify(mongoTemplate).save(existing);
    }

    @Test
    void update_shouldReturn404WhenNotFound() {
        when(mongoTemplate.findById("nonexistent", ImageMetadata.class)).thenReturn(null);

        ApiResponse response = imageService.update("nonexistent", null, "新标题", null, null, null);

        assertEquals(400, response.getCode());
        assertEquals("记录不存在", response.getMessage());
        verify(mongoTemplate, never()).save(any());
    }

    @Test
    void update_shouldReplaceImageFile() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("new.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSize()).thenReturn(2048L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[2048]));

        org.bson.types.ObjectId newGridFsId = new org.bson.types.ObjectId();
        when(gridFsTemplate.store(any(InputStream.class), anyString(), anyString())).thenReturn(newGridFsId);

        ImageMetadata existing = new ImageMetadata();
        existing.setId("507f1f77bcf86cd799439011");
        existing.setGridFsFileId("507f1f77bcf86cd799439022");
        existing.setCreateTime("20260101120000");
        existing.setUploadTime("20000101120000");
        when(mongoTemplate.findById("507f1f77bcf86cd799439011", ImageMetadata.class)).thenReturn(existing);

        ApiResponse response = imageService.update("507f1f77bcf86cd799439011", file, "新标题", null, null, null);

        assertEquals(200, response.getCode());
        verify(gridFsTemplate).delete(any(Query.class)); // 旧 GridFS 文件被删除
        ImageMetadata updated = (ImageMetadata) response.getData();
        assertEquals(newGridFsId.toString(), updated.getGridFsFileId());
        assertEquals("new.jpg", updated.getFileName());
        assertEquals("2048", updated.getFileSize());
        assertEquals("新标题", updated.getTitle());
        assertEquals("20260101120000", updated.getCreateTime());
        assertTrue(updated.getUploadTime().matches("\\d{14}"));
        assertNotEquals("20000101120000", updated.getUploadTime());
        verify(mongoTemplate).save(existing);
    }

    @Test
    void delete_shouldRemoveFileAndMetadata() {
        ImageDeleteRequest request = new ImageDeleteRequest();
        request.setId("507f1f77bcf86cd799439011");

        ImageMetadata existing = new ImageMetadata();
        existing.setId("507f1f77bcf86cd799439011");
        existing.setGridFsFileId("507f1f77bcf86cd799439022");
        when(mongoTemplate.findById("507f1f77bcf86cd799439011", ImageMetadata.class)).thenReturn(existing);

        ApiResponse response = imageService.delete(request);

        assertEquals(200, response.getCode());
        verify(gridFsTemplate).delete(any(Query.class));
        verify(mongoTemplate).remove(existing);
    }

    @Test
    void delete_shouldReturn404WhenNotFound() {
        ImageDeleteRequest request = new ImageDeleteRequest();
        request.setId("nonexistent");
        when(mongoTemplate.findById("nonexistent", ImageMetadata.class)).thenReturn(null);

        ApiResponse response = imageService.delete(request);

        assertEquals(400, response.getCode());
        assertEquals("记录不存在", response.getMessage());
        verify(gridFsTemplate, never()).delete(any(Query.class));
    }

}

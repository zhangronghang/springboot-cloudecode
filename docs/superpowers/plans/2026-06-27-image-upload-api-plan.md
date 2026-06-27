# 图片上传与备注管理接口 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现图片上传与备注管理的 5 个 POST 接口（upload/list/detail/update/delete），图片存 MongoDB GridFS，备注存 MongoDB collection。

**Architecture:** Controller → Service → MongoTemplate + GridFsTemplate。统一响应格式 `{code, message, data}`。全部接口使用 POST 方法。

**Tech Stack:** Spring Boot 2.3.6, Spring Data MongoDB (MongoTemplate + GridFsTemplate), Java 8, JUnit 5 + Mockito

## Global Constraints

- Java 8（无 `var`，无 `var` 关键字）
- 所有自定义字段均为 String 类型
- 所有接口均为 POST 方法
- 统一响应格式: `{code: int, message: String, data: Object}`
- 包路径: `com.example.springbootcoludecode`
- MongoDB 数据库名: `claude_code`
- GridFS collection 名: `image_metadata`

---

### Task 1: 脚手架 — DTO、Entity、Service 接口

**Files:**
- Create: `src/main/java/com/example/springbootcoludecode/dto/ApiResponse.java`
- Create: `src/main/java/com/example/springbootcoludecode/dto/ImageListRequest.java`
- Create: `src/main/java/com/example/springbootcoludecode/dto/ImageDetailRequest.java`
- Create: `src/main/java/com/example/springbootcoludecode/dto/ImageUpdateRequest.java`
- Create: `src/main/java/com/example/springbootcoludecode/dto/ImageDeleteRequest.java`
- Create: `src/main/java/com/example/springbootcoludecode/entity/ImageMetadata.java`
- Create: `src/main/java/com/example/springbootcoludecode/service/ImageService.java`

**Interfaces:**
- Consumes: nothing
- Produces:
  - `ApiResponse.success(Object data)` — 静态工厂，code=200, message="success"
  - `ApiResponse.error(int code, String message)` — 静态工厂
  - `ImageListRequest` — page(int), size(int), tag(String), uploader(String) 带 getter/setter
  - `ImageDetailRequest` — id(String) 带 getter/setter
  - `ImageUpdateRequest` — id, title, description, tags, uploader (all String) 带 getter/setter
  - `ImageDeleteRequest` — id(String) 带 getter/setter
  - `ImageMetadata` — @Document(collection="image_metadata"), 字段: id, title, description, tags, uploader, uploadTime, fileSize, fileName, gridFsFileId (全部 String, id 为 @Id)
  - `ImageService` 接口 — 5 个方法签名 (见下方代码)

- [ ] **Step 1: 创建 ApiResponse.java**

```java
package com.example.springbootcoludecode.dto;

public class ApiResponse {
    private int code;
    private String message;
    private Object data;

    public ApiResponse() {}

    public ApiResponse(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static ApiResponse success(Object data) {
        return new ApiResponse(200, "success", data);
    }

    public static ApiResponse error(int code, String message) {
        return new ApiResponse(code, message, null);
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}
```

- [ ] **Step 2: 创建 ImageListRequest.java**

```java
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
```

- [ ] **Step 3: 创建 ImageDetailRequest.java**

```java
package com.example.springbootcoludecode.dto;

public class ImageDetailRequest {
    private String id;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
```

- [ ] **Step 4: 创建 ImageUpdateRequest.java**

```java
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
```

- [ ] **Step 5: 创建 ImageDeleteRequest.java**

```java
package com.example.springbootcoludecode.dto;

public class ImageDeleteRequest {
    private String id;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
```

- [ ] **Step 6: 创建 ImageMetadata.java**

```java
package com.example.springbootcoludecode.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "image_metadata")
public class ImageMetadata {
    @Id
    private String id;
    private String title;
    private String description;
    private String tags;
    private String uploader;
    private String uploadTime;
    private String fileSize;
    private String fileName;
    private String gridFsFileId;

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
    public String getUploadTime() { return uploadTime; }
    public void setUploadTime(String uploadTime) { this.uploadTime = uploadTime; }
    public String getFileSize() { return fileSize; }
    public void setFileSize(String fileSize) { this.fileSize = fileSize; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getGridFsFileId() { return gridFsFileId; }
    public void setGridFsFileId(String gridFsFileId) { this.gridFsFileId = gridFsFileId; }
}
```

- [ ] **Step 7: 创建 ImageService.java**

```java
package com.example.springbootcoludecode.service;

import com.example.springbootcoludecode.dto.*;
import org.springframework.web.multipart.MultipartFile;

public interface ImageService {
    ApiResponse upload(MultipartFile file, String title, String description, String tags, String uploader);
    ApiResponse list(ImageListRequest request);
    ApiResponse detail(ImageDetailRequest request);
    ApiResponse update(ImageUpdateRequest request);
    ApiResponse delete(ImageDeleteRequest request);
}
```

- [ ] **Step 8: 编译验证脚手架**

```bash
./mvnw compile
```

预期: BUILD SUCCESS

- [ ] **Step 9: 提交**

```bash
git add src/main/java/com/example/springbootcoludecode/dto/ src/main/java/com/example/springbootcoludecode/entity/ src/main/java/com/example/springbootcoludecode/service/ImageService.java
git commit -m "feat: 添加图片管理脚手架 — DTO、Entity、Service 接口"
```

---

### Task 2: ImageServiceImpl.upload — 上传图片及备注

**Files:**
- Create: `src/main/java/com/example/springbootcoludecode/service/impl/ImageServiceImpl.java`
- Create: `src/test/java/com/example/springbootcoludecode/service/impl/ImageServiceImplTest.java`

**Interfaces:**
- Consumes: `ApiResponse`, `ImageMetadata`, `ImageService`
- Produces: `ImageServiceImpl.upload(MultipartFile, String, String, String, String)` → ApiResponse

- [ ] **Step 1: 编写 upload 方法测试**

```java
package com.example.springbootcoludecode.service.impl;

import com.example.springbootcoludecode.dto.*;
import com.example.springbootcoludecode.entity.ImageMetadata;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    void upload_shouldStoreFileAndSaveMetadata() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSize()).thenReturn(1024L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));

        org.bson.types.ObjectId gridFsId = new org.bson.types.ObjectId();
        when(gridFsTemplate.store(any(InputStream.class), anyString(), anyString())).thenReturn(gridFsId);

        ApiResponse response = imageService.upload(file, "测试标题", "测试描述", "风景,旅游", "zhangsan");

        assertEquals(200, response.getCode());
        assertTrue(response.getData() instanceof ImageMetadata);
        ImageMetadata meta = (ImageMetadata) response.getData();
        assertEquals("测试标题", meta.getTitle());
        assertEquals("测试描述", meta.getDescription());
        assertEquals("风景,旅游", meta.getTags());
        assertEquals("zhangsan", meta.getUploader());
        assertEquals("1024", meta.getFileSize());
        assertEquals("test.jpg", meta.getFileName());
        assertEquals(gridFsId.toString(), meta.getGridFsFileId());
        assertNotNull(meta.getUploadTime());
        assertNotNull(meta.getId());

        verify(mongoTemplate).save(any(ImageMetadata.class));
    }
}
```

- [ ] **Step 2: 运行测试，验证失败**

```bash
./mvnw test -Dtest=ImageServiceImplTest#upload_shouldStoreFileAndSaveMetadata
```

预期: 编译失败（ImageServiceImpl 尚不存在）

- [ ] **Step 3: 创建 ImageServiceImpl 骨架 + upload 实现**

```java
package com.example.springbootcoludecode.service.impl;

import com.example.springbootcoludecode.dto.*;
import com.example.springbootcoludecode.entity.ImageMetadata;
import com.example.springbootcoludecode.service.ImageService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ImageServiceImpl implements ImageService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Override
    public ApiResponse upload(MultipartFile file, String title, String description, String tags, String uploader) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "图片文件不能为空");
        }
        if (title == null || title.trim().isEmpty()) {
            return ApiResponse.error(400, "标题不能为空");
        }
        try {
            ObjectId gridFsFileId = gridFsTemplate.store(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType()
            );

            ImageMetadata meta = new ImageMetadata();
            meta.setTitle(title.trim());
            meta.setDescription(description != null ? description.trim() : "");
            meta.setTags(tags != null ? tags.trim() : "");
            meta.setUploader(uploader != null ? uploader.trim() : "");
            meta.setUploadTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            meta.setFileSize(String.valueOf(file.getSize()));
            meta.setFileName(file.getOriginalFilename());
            meta.setGridFsFileId(gridFsFileId.toString());

            mongoTemplate.save(meta);

            return ApiResponse.success(meta);
        } catch (IOException e) {
            return ApiResponse.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse list(ImageListRequest request) {
        return ApiResponse.error(500, "未实现");
    }

    @Override
    public ApiResponse detail(ImageDetailRequest request) {
        return ApiResponse.error(500, "未实现");
    }

    @Override
    public ApiResponse update(ImageUpdateRequest request) {
        return ApiResponse.error(500, "未实现");
    }

    @Override
    public ApiResponse delete(ImageDeleteRequest request) {
        return ApiResponse.error(500, "未实现");
    }
}
```

- [ ] **Step 4: 运行测试，验证通过**

```bash
./mvnw test -Dtest=ImageServiceImplTest#upload_shouldStoreFileAndSaveMetadata
```

预期: Tests run: 1, Failures: 0, BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/example/springbootcoludecode/service/impl/ImageServiceImpl.java src/test/java/com/example/springbootcoludecode/service/impl/ImageServiceImplTest.java
git commit -m "feat: 实现 upload 方法 — GridFS 存储 + MongoDB 备注保存"
```

---

### Task 3: ImageServiceImpl.list — 分页查询列表

**Files:**
- Modify: `src/main/java/com/example/springbootcoludecode/service/impl/ImageServiceImpl.java`
- Modify: `src/test/java/com/example/springbootcoludecode/service/impl/ImageServiceImplTest.java`

**Interfaces:**
- Consumes: `ImageListRequest` (page, size, tag, uploader)
- Produces: `list()` 返回 `ApiResponse`，data 为 `{total, page, size, records}`

- [ ] **Step 1: 编写 list 方法测试**

在 `ImageServiceImplTest.java` 中添加:

```java
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
    when(mongoTemplate.find(any(Query.class), eq(ImageMetadata.class))).thenReturn(java.util.Collections.singletonList(meta));

    ApiResponse response = imageService.list(request);

    assertEquals(200, response.getCode());
    assertTrue(response.getData() instanceof java.util.Map);
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.getData();
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
    when(mongoTemplate.find(any(Query.class), eq(ImageMetadata.class))).thenReturn(java.util.Collections.emptyList());

    ApiResponse response = imageService.list(request);

    assertEquals(200, response.getCode());
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.getData();
    assertEquals(0L, data.get("total"));
}
```

- [ ] **Step 2: 运行测试，验证失败**

```bash
./mvnw test -Dtest=ImageServiceImplTest#list_shouldReturnPaginatedResults
```

预期: FAIL（实际返回 "未实现" 的 500 错误）

- [ ] **Step 3: 实现 list 方法**

将 `ImageServiceImpl.java` 中的 `list()` 替换为:

```java
@Override
public ApiResponse list(ImageListRequest request) {
    int page = request.getPage() > 0 ? request.getPage() : 1;
    int size = request.getSize() > 0 ? request.getSize() : 10;

    Query query = new Query();
    if (request.getTag() != null && !request.getTag().trim().isEmpty()) {
        query.addCriteria(Criteria.where("tags").regex(request.getTag().trim()));
    }
    if (request.getUploader() != null && !request.getUploader().trim().isEmpty()) {
        query.addCriteria(Criteria.where("uploader").is(request.getUploader().trim()));
    }

    long total = mongoTemplate.count(query, ImageMetadata.class);
    query.with(org.springframework.data.domain.PageRequest.of(page - 1, size));
    query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "uploadTime"));
    java.util.List<ImageMetadata> records = mongoTemplate.find(query, ImageMetadata.class);

    java.util.Map<String, Object> data = new java.util.HashMap<>();
    data.put("total", total);
    data.put("page", page);
    data.put("size", size);
    data.put("records", records);

    return ApiResponse.success(data);
}
```

同时在文件顶部添加 import:
```java
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
```

- [ ] **Step 4: 运行测试，验证通过**

```bash
./mvnw test -Dtest=ImageServiceImplTest#list_shouldReturnPaginatedResults,ImageServiceImplTest#list_shouldFilterByTagAndUploader
```

预期: 2 tests passed, BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/example/springbootcoludecode/service/impl/ImageServiceImpl.java src/test/java/com/example/springbootcoludecode/service/impl/ImageServiceImplTest.java
git commit -m "feat: 实现 list 方法 — 分页查询 + 标签/上传者筛选"
```

---

### Task 4: ImageServiceImpl.detail — 查询单条详情 + 图片 base64

**Files:**
- Modify: `src/main/java/com/example/springbootcoludecode/service/impl/ImageServiceImpl.java`
- Modify: `src/test/java/com/example/springbootcoludecode/service/impl/ImageServiceImplTest.java`

**Interfaces:**
- Consumes: `ImageDetailRequest` (id)
- Produces: `detail()` 返回 `ApiResponse`，data 为 `{metadata: ImageMetadata, imageBase64: String}`

- [ ] **Step 1: 编写 detail 方法测试**

在 `ImageServiceImplTest.java` 中添加:

```java
@Test
void detail_shouldReturnMetadataAndBase64() throws Exception {
    ImageDetailRequest request = new ImageDetailRequest();
    request.setId("507f1f77bcf86cd799439011");

    ImageMetadata meta = new ImageMetadata();
    meta.setId("507f1f77bcf86cd799439011");
    meta.setTitle("测试图片");
    meta.setGridFsFileId("507f1f77bcf86cd799439022");
    when(mongoTemplate.findById("507f1f77bcf86cd799439011", ImageMetadata.class)).thenReturn(meta);

    com.mongodb.client.gridfs.model.GridFSFile gridFsFile = mock(com.mongodb.client.gridfs.model.GridFSFile.class);
    when(gridFsTemplate.findOne(any(Query.class))).thenReturn(gridFsFile);

    org.springframework.data.mongodb.gridfs.GridFsResource resource = mock(org.springframework.data.mongodb.gridfs.GridFsResource.class);
    when(resource.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("test-image-content".getBytes()));
    when(gridFsTemplate.getResource(gridFsFile)).thenReturn(resource);

    ApiResponse response = imageService.detail(request);

    assertEquals(200, response.getCode());
    assertTrue(response.getData() instanceof java.util.Map);
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.getData();
    assertEquals("测试图片", ((ImageMetadata) data.get("metadata")).getTitle());
    assertNotNull(data.get("imageBase64"));
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
```

- [ ] **Step 2: 运行测试，验证失败**

```bash
./mvnw test -Dtest=ImageServiceImplTest#detail_shouldReturnMetadataAndBase64
```

预期: FAIL

- [ ] **Step 3: 实现 detail 方法**

将 `ImageServiceImpl.java` 中的 `detail()` 替换为:

```java
@Override
public ApiResponse detail(ImageDetailRequest request) {
    if (request.getId() == null || request.getId().trim().isEmpty()) {
        return ApiResponse.error(400, "ID 不能为空");
    }
    ImageMetadata meta = mongoTemplate.findById(request.getId().trim(), ImageMetadata.class);
    if (meta == null) {
        return ApiResponse.error(400, "记录不存在");
    }

    String imageBase64 = null;
    if (meta.getGridFsFileId() != null) {
        try {
            org.bson.types.ObjectId fileId = new org.bson.types.ObjectId(meta.getGridFsFileId());
            Query gridFsQuery = new Query(Criteria.where("_id").is(fileId));
            com.mongodb.client.gridfs.model.GridFSFile gridFsFile = gridFsTemplate.findOne(gridFsQuery);
            if (gridFsFile != null) {
                org.springframework.data.mongodb.gridfs.GridFsResource resource = gridFsTemplate.getResource(gridFsFile);
                byte[] bytes = org.springframework.util.StreamUtils.copyToByteArray(resource.getInputStream());
                imageBase64 = java.util.Base64.getEncoder().encodeToString(bytes);
            }
        } catch (Exception e) {
            // 图片读取失败不影响元数据返回
        }
    }

    java.util.Map<String, Object> data = new java.util.HashMap<>();
    data.put("metadata", meta);
    data.put("imageBase64", imageBase64);
    return ApiResponse.success(data);
}
```

- [ ] **Step 4: 运行测试，验证通过**

```bash
./mvnw test -Dtest=ImageServiceImplTest#detail_shouldReturnMetadataAndBase64,ImageServiceImplTest#detail_shouldReturn404WhenNotFound
```

预期: 2 tests passed, BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/example/springbootcoludecode/service/impl/ImageServiceImpl.java src/test/java/com/example/springbootcoludecode/service/impl/ImageServiceImplTest.java
git commit -m "feat: 实现 detail 方法 — 查询详情 + 图片 base64"
```

---

### Task 5: ImageServiceImpl.update — 更新备注信息

**Files:**
- Modify: `src/main/java/com/example/springbootcoludecode/service/impl/ImageServiceImpl.java`
- Modify: `src/test/java/com/example/springbootcoludecode/service/impl/ImageServiceImplTest.java`

**Interfaces:**
- Consumes: `ImageUpdateRequest` (id, title?, description?, tags?, uploader?)
- Produces: `update()` 返回更新后的 ImageMetadata

- [ ] **Step 1: 编写 update 方法测试**

在 `ImageServiceImplTest.java` 中添加:

```java
@Test
void update_shouldUpdateFields() {
    ImageUpdateRequest request = new ImageUpdateRequest();
    request.setId("507f1f77bcf86cd799439011");
    request.setTitle("新标题");
    request.setDescription("新描述");

    ImageMetadata existing = new ImageMetadata();
    existing.setId("507f1f77bcf86cd799439011");
    existing.setTitle("旧标题");
    existing.setDescription("旧描述");
    existing.setTags("旧标签");
    when(mongoTemplate.findById("507f1f77bcf86cd799439011", ImageMetadata.class)).thenReturn(existing);

    ApiResponse response = imageService.update(request);

    assertEquals(200, response.getCode());
    ImageMetadata updated = (ImageMetadata) response.getData();
    assertEquals("新标题", updated.getTitle());
    assertEquals("新描述", updated.getDescription());
    assertEquals("旧标签", updated.getTags()); // 未传的字段保持不变
    verify(mongoTemplate).save(existing);
}

@Test
void update_shouldReturn404WhenNotFound() {
    ImageUpdateRequest request = new ImageUpdateRequest();
    request.setId("nonexistent");
    request.setTitle("新标题");
    when(mongoTemplate.findById("nonexistent", ImageMetadata.class)).thenReturn(null);

    ApiResponse response = imageService.update(request);

    assertEquals(400, response.getCode());
    assertEquals("记录不存在", response.getMessage());
    verify(mongoTemplate, never()).save(any());
}
```

- [ ] **Step 2: 运行测试，验证失败**

```bash
./mvnw test -Dtest=ImageServiceImplTest#update_shouldUpdateFields
```

预期: FAIL

- [ ] **Step 3: 实现 update 方法**

将 `ImageServiceImpl.java` 中的 `update()` 替换为:

```java
@Override
public ApiResponse update(ImageUpdateRequest request) {
    if (request.getId() == null || request.getId().trim().isEmpty()) {
        return ApiResponse.error(400, "ID 不能为空");
    }
    ImageMetadata meta = mongoTemplate.findById(request.getId().trim(), ImageMetadata.class);
    if (meta == null) {
        return ApiResponse.error(400, "记录不存在");
    }
    if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
        meta.setTitle(request.getTitle().trim());
    }
    if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
        meta.setDescription(request.getDescription().trim());
    }
    if (request.getTags() != null && !request.getTags().trim().isEmpty()) {
        meta.setTags(request.getTags().trim());
    }
    if (request.getUploader() != null && !request.getUploader().trim().isEmpty()) {
        meta.setUploader(request.getUploader().trim());
    }
    mongoTemplate.save(meta);
    return ApiResponse.success(meta);
}
```

- [ ] **Step 4: 运行测试，验证通过**

```bash
./mvnw test -Dtest=ImageServiceImplTest#update_shouldUpdateFields,ImageServiceImplTest#update_shouldReturn404WhenNotFound
```

预期: 2 tests passed, BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/example/springbootcoludecode/service/impl/ImageServiceImpl.java src/test/java/com/example/springbootcoludecode/service/impl/ImageServiceImplTest.java
git commit -m "feat: 实现 update 方法 — 更新备注字段"
```

---

### Task 6: ImageServiceImpl.delete — 删除图片及备注

**Files:**
- Modify: `src/main/java/com/example/springbootcoludecode/service/impl/ImageServiceImpl.java`
- Modify: `src/test/java/com/example/springbootcoludecode/service/impl/ImageServiceImplTest.java`

**Interfaces:**
- Consumes: `ImageDeleteRequest` (id)
- Produces: `delete()` 删除 GridFS 文件 + 备注文档

- [ ] **Step 1: 编写 delete 方法测试**

在 `ImageServiceImplTest.java` 中添加:

```java
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
```

- [ ] **Step 2: 运行测试，验证失败**

```bash
./mvnw test -Dtest=ImageServiceImplTest#delete_shouldRemoveFileAndMetadata
```

预期: FAIL

- [ ] **Step 3: 实现 delete 方法**

将 `ImageServiceImpl.java` 中的 `delete()` 替换为:

```java
@Override
public ApiResponse delete(ImageDeleteRequest request) {
    if (request.getId() == null || request.getId().trim().isEmpty()) {
        return ApiResponse.error(400, "ID 不能为空");
    }
    ImageMetadata meta = mongoTemplate.findById(request.getId().trim(), ImageMetadata.class);
    if (meta == null) {
        return ApiResponse.error(400, "记录不存在");
    }
    // 删除 GridFS 文件
    if (meta.getGridFsFileId() != null) {
        try {
            org.bson.types.ObjectId fileId = new org.bson.types.ObjectId(meta.getGridFsFileId());
            gridFsTemplate.delete(new Query(Criteria.where("_id").is(fileId)));
        } catch (Exception e) {
            // GridFS 删除失败也继续删除元数据
        }
    }
    // 删除元数据
    mongoTemplate.remove(meta);
    return ApiResponse.success(null);
}
```

- [ ] **Step 4: 运行测试，验证通过**

```bash
./mvnw test -Dtest=ImageServiceImplTest#delete_shouldRemoveFileAndMetadata,ImageServiceImplTest#delete_shouldReturn404WhenNotFound
```

预期: 2 tests passed, BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/example/springbootcoludecode/service/impl/ImageServiceImpl.java src/test/java/com/example/springbootcoludecode/service/impl/ImageServiceImplTest.java
git commit -m "feat: 实现 delete 方法 — 删除 GridFS 文件 + 备注文档"
```

---

### Task 7: GlobalExceptionHandler — 全局异常处理

**Files:**
- Create: `src/main/java/com/example/springbootcoludecode/config/GlobalExceptionHandler.java`

**Interfaces:**
- Consumes: nothing
- Produces: 全局 `@ControllerAdvice`，捕获通用异常返回统一 ApiResponse

- [ ] **Step 1: 创建 GlobalExceptionHandler.java**

```java
package com.example.springbootcoludecode.config;

import com.example.springbootcoludecode.dto.ApiResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ApiResponse.error(400, "上传文件大小超过限制");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse handleIllegalArgument(IllegalArgumentException e) {
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse handleException(Exception e) {
        return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./mvnw compile
```

预期: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/example/springbootcoludecode/config/GlobalExceptionHandler.java
git commit -m "feat: 添加全局异常处理器"
```

---

### Task 8: ImageController — REST 接口层

**Files:**
- Create: `src/main/java/com/example/springbootcoludecode/controller/ImageController.java`

**Interfaces:**
- Consumes: `ImageService`（全部 5 个方法）
- Produces: 5 个 POST 端点

- [ ] **Step 1: 创建 ImageController.java**

```java
package com.example.springbootcoludecode.controller;

import com.example.springbootcoludecode.dto.*;
import com.example.springbootcoludecode.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @PostMapping("/upload")
    public ApiResponse upload(@RequestParam("file") MultipartFile file,
                              @RequestParam("title") String title,
                              @RequestParam(value = "description", required = false) String description,
                              @RequestParam(value = "tags", required = false) String tags,
                              @RequestParam(value = "uploader", required = false) String uploader) {
        return imageService.upload(file, title, description, tags, uploader);
    }

    @PostMapping("/list")
    public ApiResponse list(@RequestBody ImageListRequest request) {
        return imageService.list(request);
    }

    @PostMapping("/detail")
    public ApiResponse detail(@RequestBody ImageDetailRequest request) {
        return imageService.detail(request);
    }

    @PostMapping("/update")
    public ApiResponse update(@RequestBody ImageUpdateRequest request) {
        return imageService.update(request);
    }

    @PostMapping("/delete")
    public ApiResponse delete(@RequestBody ImageDeleteRequest request) {
        return imageService.delete(request);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./mvnw compile
```

预期: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/example/springbootcoludecode/controller/ImageController.java
git commit -m "feat: 添加 ImageController — 5 个 POST 接口"
```

---

### Task 9: 整体编译与验证

**Files:**
- Modify: `src/main/resources/application.yaml`（如需调整文件上传大小限制）

- [ ] **Step 1: 添加文件上传大小配置**

在 `application.yaml` 的 `spring` 节点下添加:

```yaml
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
```

完整添加位置在 `spring:` 缩进内的顶层（与 `datasource` 同级），添加内容:

```yaml
spring:
  application:
    name: springboot-cloudecode
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
```

- [ ] **Step 2: 编译整个项目**

```bash
./mvnw clean compile
```

预期: BUILD SUCCESS

- [ ] **Step 3: 运行所有测试**

```bash
./mvnw test
```

预期: All tests pass (包括已有的 SpringbootColudecodeApplicationTests 和新测试)

- [ ] **Step 4: 提交**

```bash
git add src/main/resources/application.yaml
git commit -m "feat: 配置文件上传大小限制为 50MB"
```

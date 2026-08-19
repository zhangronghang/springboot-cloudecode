# CLAUDE.md

此文件为 Claude Code (claude.ai/code) 在此仓库中工作时提供指导。

> **语言要求**: 输出内容时必须使用中文。

## 构建与开发命令

- **编译**: `./mvnw compile`
- **运行全部测试**: `./mvnw test`
- **运行单个测试类**: `./mvnw test -Dtest=SpringbootColudecodeApplicationTests`
- **运行单个测试方法**: `./mvnw test -Dtest=SpringbootColudecodeApplicationTests#contextLoads`
- **打包**: `./mvnw package`
- **启动应用**: `./mvnw spring-boot:run`
- **清理构建产物**: `./mvnw clean`
- **跳过测试进行编译**: `./mvnw compile -DskipTests`

使用 Maven Wrapper（`mvnw` / `mvnw.cmd`）——无需在系统上安装 Maven。

## 架构

早期阶段的 Spring Boot 2.3.6 项目。使用 Java 8。

**技术栈**: Spring Boot 2.3.6、Spring MVC (spring-boot-starter-web)、MyBatis 2.1.4、Druid 1.2.15、MySQL、MongoDB (spring-boot-starter-data-mongodb)、Lombok。

**入口点**: `SpringbootColudecodeApplication`（位于 `src/main/java/com/example/springbootcoludecode/`）——标准的 `@SpringBootApplication` 主类。

**配置**: `src/main/resources/application.yaml`：
- 服务器端口：`8081`
- 应用名称：`springboot-cloudecode`
- 文件上传限制：单文件最大 50MB，请求最大 50MB（`spring.servlet.multipart.max-file-size` / `max-request-size`）
- 数据源：Druid 连接池，连接 `127.0.0.1:3306/claude_code`，用户名/密码均为 `root`
- MongoDB：连接 `127.0.0.1:27017`，数据库 `claude_code`，无认证
- MyBatis：mapper XML 文件位于 `classpath:mapper/*.xml`，类型别名包为 `com.example.springbootcoludecode`
- 已注释的配置：Eureka 注册中心、Feign/Hystrix

**数据库**: 使用 Druid 连接池（MySQL），配置了监控统计过滤器（stat、wall、slf4j）和 prepared statement 缓存。同时使用 Spring Data MongoDB 连接本地 MongoDB 实例。启动时若 MySQL 或 MongoDB 不可用，Spring 上下文加载会失败。

> **注意**: 虽然配置了 MyBatis + MySQL，但目前 `src/main/resources/mapper/` 目录下无任何 mapper XML 文件。当前功能模块的数据操作全部通过 `MongoTemplate` 和 `GridFsTemplate` 完成，未使用 MyBatis。

### 分层架构

项目遵循标准的三层架构：

```
controller (ImageController)
    ↓
service 接口 (ImageService) → service/impl (ImageServiceImpl)
    ↓
MongoTemplate / GridFsTemplate（直接操作 MongoDB）
```

- **Controller 层**: `@RestController`，路径前缀 `/api/images`，接收请求参数并委托给 Service
- **Service 层**: 接口定义在 `service/` 包，实现类在 `service/impl/` 包。直接注入 `MongoTemplate` 和 `GridFsTemplate`，不经过 DAO/Repository 层
- **DTO 层**: 查询与删除类 API 操作有独立的 Request DTO（`ImageListRequest`、`ImageDetailRequest`、`ImageDeleteRequest`），统一响应格式 `ApiResponse`（code + message + data）；upload/update 直接使用 `@RequestParam` 接收 multipart/form-data（upload 的 file/title 必填，update 的 file 可选）
- **Entity 层**: `ImageMetadata` 使用 `@Document` 映射到 MongoDB collection `image_metadata`。**注意此实体使用手动编写的 getter/setter，未使用 Lombok `@Data`**

### API 设计约定

- **统一使用 POST**: 所有接口（包括查询类操作如 list/detail）均使用 `@PostMapping`，而非 GET。请求参数通过 `@RequestBody`（JSON）或 `@RequestParam`（表单字段）传递
- **统一响应格式**: 所有接口返回 `ApiResponse`，包含 `code`（200 成功，400 参数错误，500 服务器错误）、`message`、`data`
- **全局异常处理**: `GlobalExceptionHandler`（`@ControllerAdvice`）统一处理 `MaxUploadSizeExceededException`、`IllegalArgumentException` 和通用 `Exception`，返回 `ApiResponse` 格式

### 图片存储架构（核心功能模块）

当前已实现的功能：图片上传与备注管理。

- **文件存储**: 使用 MongoDB **GridFS** 存储图片文件二进制数据（通过 `GridFsTemplate`）。上传时文件以流式写入 GridFS，返回 `ObjectId` 作为文件标识
- **元数据存储**: 图片的标题、描述、标签、上传者、上传时间、文件大小、文件名、GridFS 文件 ID 等信息存储在 MongoDB collection `image_metadata` 中（通过 `MongoTemplate`）
- **查询与分页**: 列表查询支持按 tag（模糊匹配）和 uploader（精确匹配）过滤，按 uploadTime 降序排列，分页参数 page/size
- **图片读取**: detail 接口将 GridFS 中的文件读取为 byte[] 后 Base64 编码返回，与元数据一同包含在响应中
- **删除**: 同时删除 GridFS 文件和 MongoDB 元数据记录

### 测试策略

- **集成测试**: `SpringbootColudecodeApplicationTests` 使用 `@SpringBootTest` 加载完整应用上下文
- **单元测试**: `ImageServiceImplTest` 使用 **纯 Mockito**（`@Mock` + `@InjectMocks`），不启动 Spring 上下文。mock `MongoTemplate` 和 `GridFsTemplate`，验证 Service 层逻辑。新服务的单元测试应遵循此模式
- **测试依赖**: 仅 `spring-boot-starter-test`（不含 webflux 响应式测试工具）

**Lombok**: 在 `maven-compiler-plugin` 中为 `default-compile` 和 `default-testCompile` 两个执行阶段显式配置了 `<annotationProcessorPaths>`。如果 IDE 报 Lombok 相关错误，请确保已在 IDE 设置中启用注解处理。注意：`ImageMetadata` 实体类未使用 Lombok，其 getter/setter 为手动编写。

**包结构**: 目前所有代码位于 `com.example.springbootcoludecode` 包下。MyBatis 类型别名已配置为指向此包，实体类应放在此包或其子包下。Mapper XML 文件应放在 `src/main/resources/mapper/` 目录中。

### 参考资料

- **接口规格**: `openspec/specs/image-upload/spec.md` — 图片上传模块的权威行为契约，包含统一响应格式、各接口（upload/list/detail/update/delete）的 requirement 与 scenario、元数据字段定义。修改或扩展图片相关接口前优先参考此 spec，保持接口行为一致。
- **开发工作流**: 本仓库使用 OpenSpec 进行 spec-driven 开发，`openspec/specs/` 保存主规格，`openspec/changes/` 保存变更提案。新功能开发按 `/openspec-propose` → `/openspec-apply-change` → `/openspec-archive-change` 流程推进。

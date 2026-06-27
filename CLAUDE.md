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
- 服务器端口：`8080`
- 应用名称：`springboot-cloudecode`
- 数据源：Druid 连接池，连接 `127.0.0.1:3306/claude_code`，用户名/密码均为 `root`
- MongoDB：连接 `127.0.0.1:27017`，数据库 `claude_code`，无认证
- MyBatis：mapper XML 文件位于 `classpath:mapper/*.xml`，类型别名包为 `com.example.springbootcoludecode`
- 已注释的配置：Eureka 注册中心、Feign/Hystrix

**数据库**: 使用 Druid 连接池（MySQL），配置了监控统计过滤器（stat、wall、slf4j）和 prepared statement 缓存。同时使用 Spring Data MongoDB 连接本地 MongoDB 实例。启动时若 MySQL 或 MongoDB 不可用，Spring 上下文加载会失败。

**测试**: 使用 JUnit 5 配合 `@SpringBootTest`（完整应用上下文）。依赖为 `spring-boot-starter-test`（不包括 `spring-boot-starter-webflux` 响应式测试工具）。

**Lombok**: 在 `maven-compiler-plugin` 中为 `default-compile` 和 `default-testCompile` 两个执行阶段显式配置了 `<annotationProcessorPaths>`。如果 IDE 报 Lombok 相关错误，请确保已在 IDE 设置中启用注解处理。

**包结构**: 目前所有代码位于 `com.example.springbootcoludecode` 包下。MyBatis 类型别名已配置为指向此包，实体类应放在此包或其子包下。Mapper XML 文件应放在 `src/main/resources/mapper/` 目录中。

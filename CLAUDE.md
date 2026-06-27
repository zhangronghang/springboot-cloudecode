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

早期阶段的 Spring Boot 4.1.0 项目，由 Spring Initializr 生成。使用 Java 17。

**技术栈**: Spring Boot 4.1.0、Spring MVC (webmvc)、MySQL、Lombok。

**入口点**: `SpringbootColudecodeApplication`（位于 `src/main/java/com/example/springbootcoludecode/`）——标准的 `@SpringBootApplication` 主类。

**配置**: `src/main/resources/application.yaml`——目前仅设置了 `spring.application.name` 为 `springboot-colude-code`（注意：与 Maven artifactId `springboot-coludecode` 不一致）。所有 Spring Boot 约定均适用，自动配置处于激活状态。

**数据库**: MySQL 连接器已在 classpath 中（runtime 作用域），但尚未配置 JPA/JDBC starter 或数据源。除非配置数据源属性或排除自动配置，否则 Spring Boot 启动会失败——可在 `application.yaml` 中添加 `spring.autoconfigure.exclude: org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration` 来推迟数据源配置。

**测试**: 使用 JUnit 5 配合 `@SpringBootTest`（完整应用上下文）。依赖为 `spring-boot-starter-webmvc-test`（而非完整的 `spring-boot-starter-test`）——这提供了 Spring MVC 测试切片支持（`@WebMvcTest`、`MockMvc`），但不包含 `spring-boot-starter-data-jpa` 的测试工具。

**Lombok**: 在 `maven-compiler-plugin` 中为 `default-compile` 和 `default-testCompile` 两个执行阶段显式配置了 `<annotationProcessorPaths>`。如果 IDE 报 Lombok 相关错误，请确保已在 IDE 设置中启用注解处理。
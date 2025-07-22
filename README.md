# RaceSafe

RaceSafe 是一个 Fabric Minecraft 模组，旨在通过与后端服务器安全通信来增强竞技游戏体验。它可以用于报告游戏数据、提交截图以供验证等。

## 主要功能

- **安全通信**: 所有与服务器的通信都使用带有时间戳和 HMAC-SHA256 签名的自定义协议进行保护。
- **数据报告**: 提供向服务器发送 JSON 格式数据的通用端点。
- **截图上传**: 支持将游戏截图和相关元数据以 `multipart/form-data` 格式上传。

## 从源代码构建

你需要安装 Java 21 或更高版本的 JDK。

1.  **克隆仓库**:
    ```bash
    git clone <your-repository-url>
    cd RaceSafe
    ```

2.  **构建项目**:
    -   在 macOS/Linux 上:
        ```bash
        ./gradlew build
        ```
    -   在 Windows 上:
        ```bash
        gradlew.bat build
        ```
    构建成功后，生成的 `.jar` 文件将位于 `build/libs/` 目录下。

## 配置

在构建过程中，你可以通过设置环境变量来配置客户端：

-   `RACESAFE_SERVER_URL`: 后端 API 服务器的基地址。如果未设置，将默认为 `http://localhost:8000`。
-   `RACESAFE_SECRET`: 用于生成 HMAC 签名的共享密钥。如果未设置，将使用一个默认的测试密钥。

**示例:**
```bash
export RACESAFE_SERVER_URL="https://api.yourserver.com"
export RACESAFE_SECRET="your-very-secret-key"
./gradlew build
```

## API 文档

本客户端与后端服务器的详细通信协议、端点和数据格式，请参阅 [API_DOCUMENTATION.md](API_DOCUMENTATION.md)。

## 许可证

本项目采用 [CC0 1.0 Universal](LICENSE) 许可证，基本上属于公共领域。

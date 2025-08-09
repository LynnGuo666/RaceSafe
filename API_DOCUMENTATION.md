# API 请求文档

本文档描述了客户端与服务器之间的 API 通信协议，基于对 `ApiClient.java` 的分析。

---

### **1. 通用概念**

#### **1.1 HTTP 版本**

所有请求都强制使用 **HTTP/1.1**。

#### **1.2 服务器基地址 (Base URL)**

所有相对路径的端点（如 `/api/...`）都是基于一个从配置文件中读取的 `serverUrl`。在开发环境中，这个地址默认为 `http://localhost:8000`。

#### **1.3 认证 (Authentication)**

所有发往服务器的请求都必须包含认证信息。认证采用基于 HMAC-SHA256 的自定义签名方案。

请求必须包含以下两个 HTTP Header：

*   `x-mod-timestamp`: 请求发出的 UTC 时间，格式为 ISO 8601 (例如: `2025-07-22T10:00:00.123Z`)。
*   `Authorization`: 认证凭证字符串。

`Authorization` Header 的格式如下：
`MOD-HMAC-SHA256 <accessKey>:<signature>`

*   `<accessKey>`: 你的访问密钥。
*   `<signature>`: 根据请求内容计算出的签名。

**签名生成 (`signature`)**

签名是以下四部分内容通过换行符 (`
`) 连接后，使用 `<secretKey>` 进行 HMAC-SHA256 加密，最后进行 Base64 编码得到的结果。

**待签名的字符串 (`stringToSign`) 格式:**
```
HTTP请求方法

请求路径

x-mod-timestamp的值

请求体的MD5哈希
```

1.  **HTTP 请求方法**: `GET` 或 `POST`。
2.  **请求路径**: URL 的路径部分 (不包含域名和查询参数)。例如, 对于 `http://localhost:8000/api/v1/data?id=1`，路径是 `/api/v1/data`。
3.  **timestamp**: `x-mod-timestamp` Header 的完整值。
4.  **请求体的 MD5 哈希**:
    *   对于 `POST` 并且 `Content-Type` 为 `application/json` 的请求，此值为 JSON 请求体的 MD5 哈希字符串。
    *   对于 `GET` 请求或 `multipart/form-data` 类型的 `POST` 请求，此值为空字符串 `""`。

---

### **2. API 端点 (Endpoints)**

##### **2.1 通用 POST 请求**

用于发送 JSON 数据。

*   **方法**: `POST`
*   **端点**: 由调用方指定，例如 `/api/v1/race-safe/client/report`
*   **描述**: 向服务器提交一份 JSON 格式的数据报告。
*   **请求头 (Headers)**:
    *   `Content-Type: application/json`
    *   `x-mod-timestamp: <timestamp>`
    *   `Authorization: MOD-HMAC-SHA256 <accessKey>:<signature>`
*   **请求体 (Request Body)**:
    *   一个 JSON 格式的字符串。
    ```json
    {
      "key1": "value1",
      "key2": "value2"
    }
    ```
*   **响应 (Response)**:
    *   服务器返回一个字符串，通常是 JSON 格式，表示操作结果。具体格式取决于服务器实现。

##### **2.2 通用 GET 请求**

用于从服务器获取数据。

*   **方法**: `GET`
*   **端点**: 由调用方指定，例如 `/api/v1/race-safe/some-data`
*   **描述**: 从服务器获取资源或数据。
*   **请求头 (Headers)**:
    *   `x-mod-timestamp: <timestamp>`
    *   `Authorization: MOD-HMAC-SHA256 <accessKey>:<signature>`
*   **请求体 (Request Body)**:
    *   无
*   **响应 (Response)**:
    *   服务器返回一个字符串，通常是 JSON 格式。具体格式取决于服务器实现。

##### **2.3 发送截图 (Multipart POST)**

用于上传截图和相关的元数据。

*   **方法**: `POST`
*   **端点**: 一个完整的 URL，由服务器预先提供 (例如，通过其他 API 调用获取)。
*   **描述**: 上传一张截图 (`.png`) 和一份元数据 (`.json`)。
*   **请求头 (Headers)**:
    *   `Content-Type: multipart/form-data; boundary=<boundary_string>`
    *   `x-mod-timestamp: <timestamp>`
    *   `Authorization: MOD-HMAC-SHA256 <accessKey>:<signature>`
*   **请求体 (Request Body)**:
    *   一个 `multipart/form-data` 格式的请求，包含两个部分：
        1.  **metadata**:
            *   `Content-Disposition: form-data; name="metadata"`
            *   `Content-Type: application/json; charset=UTF-8`
            *   内容是一个 JSON 字符串。
        2.  **screenshot**:
            *   `Content-Disposition: form-data; name="screenshot"; filename="screenshot.png"`
            *   `Content-Type: image/png`
            *   内容是截图的原始二进制数据。
*   **响应 (Response)**:
    *   服务器返回一个字符串，通常是 JSON 格式，表示上传结果。具体格式取决于服务器实现。

---

**注意**: 这份文档是基于客户端代码的逆向分析。服务器端的具体实现可能会有细微差别或更多未在客户端代码中体现的功能。

---

### 3. 报告与任务流程说明（RaceSafe 特定）

#### 3.1 报告（Report）

- **端点**: `POST /api/v1/race-safe/client/report`
- **内容**: 由客户端通过 `DataCollector.collectInitialReport()` 生成的 JSON，包含以下字段：
  - `schemaVersion`、`timestamp`
  - `user`（用户名、UUID）
  - `game`（Minecraft 版本、Fabric Loader 版本）
  - `mods`（已加载模组列表）
  - `resourcePacks`（可用/启用资源包列表）
- **发送时机**:
  - 加入任意世界后，立即发送一次“初始报告”。
  - 进入世界后，客户端会每隔 **30 秒** 自动发送一次“周期报告”（仅当仍在世界中时）。
  - 手动执行客户端命令 `racesafe check` 会立即提交一份报告。

> 说明：“报告”发送是定时行为（每30秒一次），与任务轮询无关。

#### 3.2 任务轮询与截图上传（独立链路）

- **任务轮询**: 客户端在收到服务器返回的 `taskUrl` 后，会每 **30 秒** 对该 `taskUrl` 发起 `GET` 请求，以获取任务。
- **任务类型**:
  - `REQUEST_SCREENSHOT`: 触发一次截图采集与上传。
  - `NO_OP`: 无操作。
- **截图上传**:
  - 由服务器返回的完整 `submissionUrl` 接收，客户端以 `multipart/form-data` 方式提交：
    - 字段 `metadata`（JSON，包含 `taskId`、`user`、`timestamp`）
    - 字段 `screenshot`（PNG 二进制）
  - 该上传请求使用与其他请求相同的签名机制（multipart 的 MD5 参与为空字符串）。

> 重要区分：任务轮询（每30秒）与截图上传仅在收到 `REQUEST_SCREENSHOT` 时发生，且不属于“报告”上报链路。

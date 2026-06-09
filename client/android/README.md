# Android 客户端

本项目为电商 RAG Agent 的 Android 客户端，使用 Kotlin + Jetpack Compose 开发。

## 环境要求

- Android Studio（最新稳定版）
- Android SDK（API 30+ 推荐）
- Android Emulator 或真机

## 快速开始

### 1. 打开项目

1. 启动 Android Studio
2. `File` → `Open` → 选择 `client/android` 目录
3. 等待 Gradle Sync 完成

### 2. 配置后端地址

打开 `app/src/main/java/com/ecommerce/rag/client/config/AppConfig.kt`：

```kotlin
// Android Emulator 访问宿主机后端（本地部署推荐）
const val BASE_URL = "http://10.0.2.2:8080"

// 真机测试时使用宿主机局域网 IP
// const val BASE_URL = "http://192.168.x.x:8080"

// 公网后端（APK 内置地址）
// const val BASE_URL = "http://<PUBLIC_IP>:8080"

// 生产环境（HTTPS + 域名）
// const val BASE_URL = "https://api.your-domain.com"
```

> **重要**：Android Emulator 访问宿主机后端**必须**使用 `10.0.2.2`，`localhost` 在 Emulator 内指向的是 Emulator 自身。

### 3. 启动 Emulator

1. 打开 Device Manager
2. 启动 Android Emulator（推荐 API 30+）
3. 点击 Android Studio 的 `Run` 按钮（绿色三角形）

### 4. 体验 App

- App 启动后会自动执行 demo 登录（用户名 `demo` / 密码 `demo123`）
- 进入商品浏览页，可浏览商品列表
- 长按/点击悬浮机器人打开聊天框，与 AI 导购对话

## 功能说明

| 功能 | 说明 |
|------|------|
| 商品浏览 | 本地加载商品数据，支持分类浏览 |
| 商品详情 | 查看商品详细信息 |
| AI 导购对话 | 通过 SSE 流式接口与后端交互 |
| 商品卡片 | 对话中展示推荐商品卡片 |
| 语音输入 | 长按语音按钮说话，自动识别发送 |
| TTS 朗读 | AI 回复后自动语音朗读 |
| 购物车 | 查看、添加、修改、删除商品 |
| 对话式加购 | 在聊天中说"把第一款加入购物车" |

## 权限说明

| 权限 | 用途 |
|------|------|
| `RECORD_AUDIO` | 语音输入（ASR）需要麦克风权限 |
| `INTERNET` | 网络请求 |

麦克风权限在首次长按"按住说话"时动态申请。

## 常见问题

### 连接失败

- 确认后端已启动：`curl http://localhost:8080/api/health`
- 确认 `AppConfig.BASE_URL` 为 `http://10.0.2.2:8080`
- 确认 `AndroidManifest.xml` 中 `usesCleartextTraffic="true"`
- 检查宿主机防火墙是否放行 8080 端口

### 购物车 401

- 确认已登录：App 启动后应自动 demo 登录
- 确认 token 未过期
- 检查后端 `DEMO_USERNAME` 和 `DEMO_PASSWORD` 配置

### TTS 不播放

- 确认 TTS 后端接口可用
- 确认 `TTS_PUBLIC_BASE_URL` 配置正确（不要用 localhost）
- 检查 Emulator/真机音量是否开启
- TTS 失败时前端会显示一次小错误，不影响文字回答

### ASR 不可用

- 确认设备支持语音识别：`SpeechRecognizer.isRecognitionAvailable()`
- 确认已授予麦克风权限
- 某些 Emulator 可能不支持 ASR，建议用真机测试

## 目录结构

```
client/android/
├── app/
│   ├── src/main/java/com/ecommerce/rag/client/
│   │   ├── config/           # 配置（AppConfig.kt）
│   │   ├── data/remote/      # API 客户端
│   │   ├── ui/               # Jetpack Compose UI
│   │   └── viewmodel/        # ViewModel
│   └── src/main/assets/      # 本地商品数据、图片
└── build.gradle.kts          # 构建配置
```

## Android BASE_URL 配置说明

根据使用场景选择对应的 `BASE_URL`：

| 场景 | BASE_URL | 说明 |
|------|----------|------|
| 本地 Emulator | `http://10.0.2.2:8080` | Emulator 访问宿主机后端 |
| 真机 + 本地后端 | `http://192.168.x.x:8080` | 手机和电脑在同一局域网 |
| 公网后端 + APK | `http://<PUBLIC_IP>:8080` | APK 内置地址，直接安装即可 |
| 域名 + HTTPS | `https://api.example.com` | 生产环境 |

> 如果提供 APK，则 APK 已经内置公网 `BASE_URL`，评委无需修改。
> 如果评委用 Android Studio 运行，需要手动确认 `AppConfig.BASE_URL` 与后端地址一致。

## 注意事项

- `usesCleartextTraffic="true"` 仅用于本地开发和 HTTP 公网演示，生产环境必须使用 HTTPS
- 商品列表页使用本地 `assets/products.json`，不调用后端接口
- 商品详情页优先调用后端 `GET /api/products/{id}`，失败时回退本地数据

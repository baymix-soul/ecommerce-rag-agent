# Git 规则

## 分支策略

| 分支 | 用途 | 说明 |
|------|------|------|
| `main` | 稳定演示分支 | 始终保持可演示状态，不直接提交 |
| `dev` | 日常开发分支 | 所有 feature/fix 从此拉出，合并回此 |
| `feature/backend-mvp` | 后端 MVP 功能 | Spring Boot 骨架、商品加载、健康检查 |
| `feature/rag-pipeline` | RAG 管线 | Embedding、向量检索、Prompt 构造 |
| `feature/streaming-api` | 流式 API | SSE 对话接口 |
| `feature/android-client` | Android 客户端 | Kotlin + Jetpack Compose |
| `feature/product-card` | 商品卡片渲染 | UI 组件 |
| `feature/cart` | 购物车/多轮增强 | 排除已推荐、购物车功能 |

## 提交规范

### Commit Message 格式

```
<type>(<scope>): <subject>
```

**Type 定义**:

| Type | 含义 | 示例 |
|------|------|------|
| `init` | 项目初始化 | `init: 项目上下文与规则文件填充` |
| `docs` | 文档变更 | `docs(rag): 更新 RAG 设计文档` |
| `feat(server)` | 后端新功能 | `feat(server): 添加商品检索 API` |
| `feat(rag)` | RAG 模块新功能 | `feat(rag): 实现 Prompt 模板引擎` |
| `feat(api)` | API 相关 | `feat(api): 实现 SSE 对话流接口` |
| `feat(client)` | Android 客户端新功能 | `feat(client): 实现对话界面 Composable` |
| `fix` | 修复 Bug | `fix(rag): 修复检索结果为空时的空指针` |
| `refactor` | 重构 | `refactor(server): 提取 Embedding 抽象层` |
| `test` | 测试相关 | `test(rag): 添加检索模块单元测试` |
| `chore` | 杂项（依赖更新等） | `chore: 升级 Spring Boot 版本` |

## 禁止提交的内容

1. **`.env`** — 包含真实凭据的环境变量文件
2. **真实 API Key / Token / 密码** — 任何形式的硬编码或配置
3. `__pycache__/`、`*.pyc` — Python 编译缓存（如有 Python 工具）
4. `target/`、`build/`、`.gradle/` — Java / Gradle 构建产物
5. `.vscode/`、`.idea/` — IDE 个人配置
6. `node_modules/` — Node.js 依赖（如有）
7. 大文件（> 10MB）：模型文件、数据集等

## 必须提交的内容

- **`.env.example`** — 环境变量模板，必须保留，所有值使用占位符
- **`data/*.json`** — 商品 mock 数据（不包含真实商业数据）

## 提交前检查清单

每次提交前逐项确认：

- [ ] `.env` 不在 Git 暂存区
- [ ] `.env.example` 中没有真实 API Key
- [ ] 代码中无硬编码的 API Key
- [ ] 提交范围小而聚焦，不混入无关改动
- [ ] Commit message 符合 `<type>(<scope>): <subject>` 格式
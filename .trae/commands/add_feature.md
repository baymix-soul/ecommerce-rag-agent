# add_feature — 添加新功能

## 用途

在现有项目中添加一个新功能模块或功能点。

## Prompt 模板

```
你现在作为本项目的 AI 编程助手，请帮我添加以下功能：

功能名称：
功能描述：
涉及模块：
验收标准：

在执行前请：
1. 先读取 AGENTS.md 了解项目上下文和当前阶段
2. 读取 .trae/rules/coding_rules.md 了解编码规范
3. 读取 .trae/rules/rag_rules.md（如涉及 RAG 模块）
4. 读取 .trae/memory/decisions.md 确认已有技术决策
5. 读取 .trae/memory/todo.md 确认当前进度

请完成以下操作：
1. 分析功能需求，拆解为小步实现任务
2. 更新 .trae/memory/todo.md 添加新任务
3. 小步实现功能，每步完成后验证：
   - 如果项目有编译命令，运行编译检查
   - 如果项目有测试命令，运行相关测试
4. 更新 .trae/memory/changelog.md 记录变更
5. 列出修改的文件清单

注意：
- 每次只修改必要的文件，避免无关改动
- 新功能优先实现最小可运行版本
- 遵循 .trae/rules/ 下的所有规则
- 不写入真实 API Key 或凭据
```
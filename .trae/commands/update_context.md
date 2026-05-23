# update_context — 更新项目上下文

## 用途

在重大变更后同步更新项目上下文文件，确保 `.trae/memory/` 和 `docs/` 中的文档与代码保持一致。

## Prompt 模板

```
你现在作为本项目的 AI 编程助手，请帮我更新项目上下文：

变更范围：
变更描述：
影响的模块：

在执行前请：
1. 先读取 AGENTS.md 了解当前项目状态
2. 读取需要更新的目标文件了解当前内容

请完成以下操作：

1. 更新 AGENTS.md（如有需要）：
   - 如目录结构变化，更新结构说明
   - 如技术栈变化，更新技术栈表
   - 如开发阶段推进，更新当前阶段

2. 更新 .trae/memory/decisions.md：
   - 记录新的技术决策（按 D-{序号} 格式）
   - 标注已废弃的决策（status: superseded）

3. 更新 .trae/memory/todo.md：
   - 标记已完成的任务
   - 添加新发现的任务
   - 调整任务优先级

4. 更新 .trae/memory/changelog.md：
   - 按时间倒序记录所有重要变更
   - 关联决策编号

5. 更新 docs/ 下受影响的文档：
   - architecture.md — 架构变更
   - api_spec.md — API 变更
   - data_schema.md — 数据模型变更
   - rag_design.md — RAG 设计变更
   - demo_plan.md — 演示计划变更

注意：
- 不更新与本次变更无关的文档
- 文档描述与代码实现必须一致
- 更新粒度以"一次有意义的功能变更"为单位
- 不写入真实 API Key 或凭据
```
# 变更日志

> 按时间倒序记录项目的所有重要变更。

## 格式

```
## [YYYY-MM-DD] 变更标题
- **类型**: init / feat / fix / docs / refactor / chore
- **描述**: 变更内容简述
- **影响范围**: 涉及的文件/模块
- **关联决策**: D-{序号}（如有）
```

---

## [2026-05-21] 项目上下文与规则文件填充
- **类型**: init
- **描述**: 基于最新技术栈（Java 17/21 + Spring Boot 3 + Android Kotlin）完成项目上下文和规则文件的完整填充
  - 写入 AGENTS.md 作为项目总上下文入口
  - 重写 5 个规则文件：global_rules, permission_rules, coding_rules, rag_rules, git_rules
  - 重写 5 个命令文件为 Prompt 模板：init_project, add_feature, fix_bug, review_code, update_context
  - 重写 3 个记忆文件：decisions（8 条决策）, changelog, todo
  - 补充 5 个文档文件待完成
- **影响范围**: AGENTS.md, .trae/rules/*, .trae/commands/*, .trae/memory/*
- **关联决策**: D-001 ~ D-008

## [2026-05-21] 项目目录初始化（第一轮）
- **类型**: init
- **描述**: 创建项目基础目录结构，搭建 .trae/ 规则体系、命令定义、记忆文件和 docs/ 设计文档框架。技术栈为 Python/FastAPI（已被本轮覆盖更新）
- **影响范围**: 全项目
- **关联决策**: 已被 D-001 ~ D-008 覆盖更新
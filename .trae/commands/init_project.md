# init_project — 初始化项目模块

## 用途

从零开始初始化一个新的项目模块或功能域。

## Prompt 模板

```
你现在作为本项目的 AI 编程助手，请帮我初始化以下模块：

模块名称：
模块目标：
技术约束：
依赖的已有模块：

在执行前请：
1. 先读取 AGENTS.md 了解项目上下文
2. 读取 .trae/rules/coding_rules.md 了解编码规范
3. 读取 .trae/memory/decisions.md 确认技术决策
4. 读取 .trae/memory/todo.md 确认当前进度

请完成以下操作：
1. 创建模块目录结构（遵循 coding_rules.md 中的项目结构约定）
2. 创建必需的配置文件和入口文件
3. 创建模块的骨架代码（仅声明类和方法，不实现业务逻辑）
4. 更新 .trae/memory/todo.md 添加该模块的后续任务
5. 更新 .trae/memory/changelog.md 记录本次变更
6. 更新 docs/ 中受影响的文档

注意：
- 不编写完整业务逻辑，只搭建骨架
- 不引入未在 AGENTS.md 中规划的依赖
- 遵循 .trae/rules/ 下的所有规则
```
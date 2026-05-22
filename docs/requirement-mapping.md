# PDF 需求映射

需求来源：`challenge_1_病虫害问答助手.pdf`

## 背景目标

PDF 要求构建一个基于 RAG 技术的农业病虫害智能问答助手，帮助用户快速获得病虫害识别与防治信息。本项目实现为 Java 本地 Web 应用，采用本地知识库检索增强生成，保证可实际运行。

## 功能对应

| PDF 要求 | 实现位置 |
| --- | --- |
| 管理包含病虫害知识的文档库 | `src/main/resources/knowledge/pest_knowledge.tsv` |
| 覆盖常见农作物病虫害信息 | 知识库覆盖粮食、经济、蔬菜、果树四类作物 |
| 包含名称、症状、发生规律、防治方法、推荐用药 | `PestKnowledge` 领域对象和 TSV 13 个字段 |
| 自然语言提问 | `POST /api/chat` 和前端输入框 |
| 理解用户问题意图 | `QuestionAnalyzer.detectIntent` |
| 从知识库检索相关内容 | `KnowledgeSearchService.search` |
| 结合检索内容生成准确回答 | `PestQaService.buildAnswer` |
| 支持多轮对话上下文 | `PestQaService.withContext` 结合上一轮来源 |
| 描述异常症状时推测病虫害类型 | `症状识别` 意图分支 |
| 提供进一步确认问题 | `confirmingQuestions` 和 `followUpQuestions` |
| 简洁友好的问答界面 | `src/main/resources/static/index.html` |
| 显示回答来源或参考文档 | 前端 `source-card` 来源卡片 |
| 保存和查看历史记录 | `ChatHistoryService` 与 `/api/history` |
| 知识库无法回答时明确告知用户 | `PestQaService.fallback` |
| 合理响应时间与高效检索 | 内存知识库和加权关键词检索 |

## 知识库字段

每条病虫害包含：

- 病虫害名称
- 别名
- 分类地位
- 作物类别
- 危害作物类型
- 危害症状描述
- 发生规律与条件
- 传播途径
- 农业防治方法
- 化学防治药剂及使用方法
- 生物防治方法
- 预防建议

## 评分点覆盖

1. 知识库完整性：内置 17 条常见病虫害数据，覆盖 PDF 指定作物类别。
2. 问答智能性：实现意图识别、加权检索、来源排序和基于检索内容的回答组织。
3. 用户体验：提供浏览器交互界面、快捷样例、知识库筛选、来源卡片和历史回放。
4. 回答质量：回答包含判断依据、发生规律、防治方案、预防建议和用药提示。

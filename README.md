# 农业病虫害智能问答助手

本项目根据 `challenge_1_病虫害问答助手.pdf` 实现，是一个基于 Java 技术栈的本地 RAG 风格病虫害问答系统。系统内置病虫害知识库，支持自然语言提问、症状辅助识别、防治建议生成、来源展示和会话历史记录。

## 技术栈

- Java 21
- Maven 标准工程
- JDK HttpServer + Virtual Threads
- 原生 HTML/CSS/JavaScript
- 本地 TSV 知识库
- 关键词、作物、症状加权召回的轻量 RAG 检索流程

项目没有强依赖外部服务，适合在无网络环境中直接编译运行。

## 功能清单

- 知识库构建：覆盖水稻、小麦、玉米、大豆、棉花、油菜、甘薯、白菜、番茄、黄瓜、辣椒、苹果、梨、桃、柑橘等作物常见病虫害。
- 智能问答：根据用户问题识别咨询意图，并检索相关知识条目生成回答。
- 症状识别辅助：用户描述异常症状时，系统推测可能病虫害并给出确认线索。
- 防治建议：输出农业防治、化学防治、生物防治和预防建议。
- 来源展示：每次回答展示匹配知识条目、作物类别、匹配分和摘要。
- 历史记录：按浏览器会话保存问答历史，支持清空和回放。
- 知识库浏览：可按作物类别筛选并快速发起问题。

## 快速启动

```powershell
mvn clean package
java -jar target/agro-pest-qa-1.0.0.jar
```

启动后访问：

```text
http://localhost:8080
```

也可以指定端口：

```powershell
java -jar target/agro-pest-qa-1.0.0.jar 9090
```

## API

### POST /api/chat

请求：

```json
{
  "sessionId": "demo-session",
  "question": "水稻叶片有梭形灰白病斑，穗颈发黑，可能是什么病？"
}
```

返回字段：

- `answer`：生成的问答内容
- `intent`：识别出的用户意图
- `confidence`：置信度
- `sources`：检索来源
- `followUpQuestions`：后续追问建议

### GET /api/history?sessionId=demo-session

获取指定会话历史。

### DELETE /api/history?sessionId=demo-session

清空指定会话历史。

### GET /api/knowledge

获取知识库概要。

## 项目结构

```text
src/main/java/com/ohcp/agroqa
├── AgroQaApplication.java          # 启动入口
├── api                             # HTTP 接口和静态资源服务
├── domain                          # 领域对象
├── repository                      # 知识库加载
├── service                         # 检索、问答、历史服务
└── util                            # JSON 工具

src/main/resources
├── knowledge/pest_knowledge.tsv    # 病虫害知识库
└── static                          # 前端页面

docs
└── requirement-mapping.md          # PDF 需求映射
```

## RAG 流程说明

1. 问题解析：归一化用户输入，提取作物、病虫害名称、别名、症状和防治关键词。
2. 知识召回：在本地知识库中按名称、别名、作物、症状、防治字段加权匹配。
3. 排序筛选：按匹配分排序，选择 Top-K 作为参考来源。
4. 答案生成：根据意图组织症状、发生规律、传播方式、防治方案和确认追问。
5. 结果追溯：把检索来源和摘要返回前端展示。

## 注意事项

药剂信息仅作知识库参考，实际生产中应以当地登记作物、防治对象、农药标签剂量、安全间隔期和农技部门指导为准。

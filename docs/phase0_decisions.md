# Phase 0 启动决策记录（G0）

## 运行时决策
- need_ingest: true（简历 PDF 由 knowledge-ingest-engineer 摄入）
- need_research: true（G1 通过后调度 research-analyst）
- need_cloud_baseline_check: false（用户确认跳过；部署按低成本方案：单台轻量云服务器 + Docker Compose）

## 项目定位（用户确认）
- 业务方向：校园互助生活平台——二手交易 + 失物招领 + 跑腿拼单 + AI 助手服务
- 用户背景：大三在校生季祥浩，微服务架构，贴合生活、不高大上
- 简历技能覆盖要求：Spring Cloud（Nacos/Gateway/OpenFeign/Sentinel）、Dubbo RPC 对比、Spring AI/LangChain4j、MySQL/Redis/MongoDB/MyBatis-Plus、Java 并发/JVM/设计模式、幂等与失败重试

## 模板映射与 Owner
| 成员 | 模板 | 输出 | Gate |
|------|------|------|------|
| knowledge-ingest-engineer | templates/material_digest.md | .workbuddy/output/material_digest.md | G1 |
| research-analyst | templates/research_report.md | .workbuddy/output/research_report.md | G2 |
| business-architect | templates/高层架构设计.md | .workbuddy/output/高层架构设计.md | G3 |
| system-architect | templates/系统设计.md | .workbuddy/output/系统设计.md | G4 |
| product-story-designer | templates/UserStory.md | .workbuddy/output/UserStory.md | G4 |
| platform-architect | templates/部署设计.md | .workbuddy/output/部署设计.md | G5 |
| security-architect | templates/安全设计.md | .workbuddy/output/安全设计.md | G5 |

## G1 审核裁决（2026-09-14）
- 用户意见原文："可以不用mongoDB，poi，flowable"
- 技术栈基线调整：新项目不引入 MongoDB、poi-tl Word 导出、Flowable 工作流；存储基线 = MySQL + MyBatis-Plus + Redis（+Vue 前端）；SQLite 仅作为简历历史事实，不进入新项目候选栈。
- 该裁决对下游所有阶段生效：业务架构（G3）起不得在 In-Scope 方案中引入上述三项。

## G3 阶段中间确认裁决（2026-09-14，经中间确认）
- 论题一（拼单 MVP 形态）：方案 B —— 跑腿单 + 顺路拼单备注轻量形态，完整拼团撮合移入完整版。
- 论题二（支付边界）：方案 B —— 模拟支付 + 站内余额/积分体系，不接真实支付（学生主体商户资质风险）。

## 术语表（v1，随阶段演进）
- 平台名：校园互助生活平台（待业务架构阶段定名）
- 核心域：用户域、二手交易域、失物招领域、跑腿拼单域、AI 助手域、消息通知域
- 技术词：MCP、Function Calling、Outbox、Nacos、Sentinel、OpenFeign、Dubbo

## 团队
- team: aicoding-arch-campus-platform
- lead: team-lead（齐构成）
- 成员已全部入队待命：business/system/platform/security/product-story + knowledge-ingest/research

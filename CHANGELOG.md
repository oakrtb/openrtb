# Changelog

## 0.2.0 — 2026-09-07

- **Breaking（SDK API）**：包/模块 `inspect` → `view`（热路径读模型）并精简入口。Java：`com.oakrtb.sdk.view`（`RequestViews`/`ResponseViews`，原 `RequestInspect`/`ResponseInspect`）；步骤 `shared`（原 `pinShared`/`sharedOf`）、`imps`/`bids`（原 `viewImps`/`viewBids`）；一键入口 **仅** `RequestPipeline.run` / `ResponsePipeline.run`（已移除与 Pipeline 重复的 `of` / `viewsAfterGate` 等）。Go：`github.com/oakrtb/openrtb/sdk/go/view`，入口 `RunRequest` / `RunResponse`（已移除 `ViewRequest`/`ViewResponse`）。Rust：`oakrtb_sdk::view`，入口 `run_request` / `run_response`（已移除 `view_request`/`view_response` 及 `*Result`）。`MarkupMask` / `Inventory` / LightGate / Snapshot 保留。文档：`docs/view-usage.md`（原 `inspect-usage.md`）。
- **Breaking（SDK API）**：Fit：`bidFit`/`responseFit` → `bid`/`response`（Go `Bid`/`Response`，Rust `bid`/`response`）。`ok()`/`OK()` 仅表示无 ERROR；低价/屏蔽/超时等为 WARN，政策拒投须读 `warnings()`/`has()`。
- Fit / LightGate 一致性：底价仅当响应 `cur` 与 `imp.bidfloorcur` 均非空白且相等才比价（不隐式 USD）；`CUR_NOT_ALLOWED` 与 floor 同 trim/blank 规则；Rust `fit::response` 对非 object / 非 array `seatbid` / 空 bid 列表发 `MALFORMED`；LightGate 拒绝空白 `id`/`cur`/`imp.id` 及空白 `BidRequest.cur[]` 项；Pipeline `shared()` pin-once。
- 对齐 IAB OpenRTB **2.6-202606**：`Content.realtime` / `firstbroadcast`，并更新 `livestream` 语义（排期/线性 vs VOD）。
- 补齐较早快照字段：`Content.gtax` / `genres`，`Video.poddedupe`，`Dooh.venuetype` / `venuetypetax`。
- Proto 同步：`DurFloors`、`Refresh`/`RefSettings`、`Imp.refresh`、`Geo.regionfips104` 等。
- 文档增加折扣宏 `${AUCTION_DISCOUNT_PCT}` / `${AUCTION_DISCOUNT_CPM}`，并澄清 `${AUCTION_PRICE}`。
- `VERSION` / OpenAPI / schema `$comment` 钉住 `2.6-202606`。
- Go / Java / Rust SDK：按广告形态（banner/video/audio/native）的 BidRequest / BidResponse **构建器**，支持 `BuildValidated` 与 schema 对齐。
- Schema / 构建器强校验：`BidRequest.at`、`BidRequest.cur`（≥1 币种）、`BidResponse.cur` 必填。
- OpenAPI `NoBidResponse` 示例补 `cur`；负例夹具对齐新必填；`validate.py` 校验 OpenAPI 嵌入示例。
- 全字段覆盖测试：`testdata/full/`（App/Site/Dooh 请求 + 完整响应）；Go/Java 反射校验每个 proto 字段可设值，三语言 + `make validate` 均 schema 通过。
- Java Schema：`$id` 映射到 classpath，离线校验不再请求 GitHub。
- Go / Java / Rust SDK：`view` 流水线（LightGate → SharedView + ImpView + format 位掩码），供调用方热路径使用。
- Go / Java / Rust：对称命名 `RequestPipeline` / `ResponsePipeline`（及 `RequestViews` / `ResponseViews`）；`RunRequest` / `run_request` 等入口对齐。
- BidResponse 构建器：`noBid` ↔ `addSeatBid` 互斥（后写清对方状态）；Java `ValidatedPayload` 抽到 `com.oakrtb.sdk.build` 公共类型。
- Response pipeline 的 `bids` 不再重算 shared（deadline/时刻稳定）。
- 文档：`docs/view-usage.md` 三语言 view / Pipeline 端到端使用示例。
- 文档：`spec.md` 必填与 schema 对齐（`at`/`cur`）；`view-usage.md` 增加与 OpenRTB 2.6 接入对照（204 / OakRTB profile / tmax）。
- **Breaking（SDK API）**：`inspect.Format` → `MarkupMask`（字段 `formats`→`markup`）；`match`/`bidmatch` → `fit`（`Fit` / `FitResult`）；`inspect.Channel` → `Inventory`（字段 `channel`→`inventory`）。消除与 proto `Banner.Format` / `Content.Channel` 撞名，并明确 fit≠广告匹配引擎。
- **Breaking（SDK API）**：包/模块 `validate` → `schema`。Java：`com.oakrtb.sdk.schema.Schema.request`/`response`，类型 `Report`/`Issue`（原 `Validator`、`ValidationResult`、`ValidationError`）；Go：`github.com/oakrtb/openrtb/sdk/go/schema` 的 `Request`/`Response`；Rust：`oakrtb_sdk::schema::request`/`response`。HTTP 400 body 仍为 `{"ok":...,"errors":[...]}`（`Report` 形状对齐 `validation-result.schema.json`）；Go embed 路径 `sdk/go/schema/schemas/`。
- Go / Java / Rust SDK：可选 **Fit** 层（原 Match）（`impReady` / `bid` / `response`）— 组标前形态就绪与 bid↔imp 一致性；ERROR/WARN 软结果，不进 LightGate。
- Java SDK 依赖升级到主流稳定线：Protobuf **4.36.1**、Jackson **2.22.2**、networknt json-schema-validator **2.0.7**（Jackson 2 兼容线）、JUnit **5.14.4**；`Schema` 对齐 networknt 2.x `SchemaRegistry` API。
- Go SDK：`go 1.25`、`jsonschema/v6` **v6.0.3**、`golang.org/x/text` **v0.41.0**（`protobuf` 已为 **v1.36.12**）。
- Rust SDK：`jsonschema` **0.55.1**、`prost`/`prost-build` **0.14.4**、`serde` **1.0.229**、`serde_json` **1.0.151**、`thiserror` **2.0.20**、`walkdir` **2.5.0**；`schema` 对齐 jsonschema `Registry` API。
- 一致性：去掉 schema 误挂的 `RefSettings.count`（`count` 仅属 `Refresh`）；澄清 schema / proto / OpenAPI 分层；OpenAPI 标明 JSON-only 合同与 OakRTB `at`/`cur` profile；`Content-Type` 改为可选（缺省 JSON）；proto 为 `private_auction` / `us_privacy` / `gpp_sid` 增加 `json_name`；文档修正最小 Banner 必填说明与 `mtype:0` 语义。
- SDK：权威 schema 仅 `schema/jsonschema/`；Java/Rust 构建时拷入；Go 提交 `sdk/go/schema/schemas/*.json` 副本（`make sync-schemas`），支持 `go get`。

## 0.1.0 — 2026-09-07

- 首个 OakRTB 协议定义，JSON 兼容 IAB OpenRTB 2.6。
- BidRequest / BidResponse JSON Schema（含 Native 1.2 内层校验）。
- `POST /openrtb/v2/auction` OpenAPI 合同。
- 可选 protobuf 编码 `oakrtb.v2`。
- Banner / Video / Native 样例与负例夹具。
- 样例按 `examples/bid-request|bid-response/` 分类；生成物目录统一为 `gen/`；CI 增加 protobuf 语法检查。
- Go / Java / Rust SDK：protobuf 模型生成、JSON Schema 校验、统一 `ValidationResult`（OpenAPI 400）。
- `make jar` 产出 `oakrtb-sdk` jar（含 `-all` shade 包）。
- 传输压缩支持多种算法：`gzip`（推荐）、`deflate`、`br`、`zstd`，经 `Accept-Encoding` / `Content-Encoding` 协商。

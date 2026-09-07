# Changelog

## 0.2.0 — 2026-09-07

- 对齐 IAB OpenRTB **2.6-202606**：`Content.realtime` / `firstbroadcast`，并更新 `livestream` 语义（排期/线性 vs VOD）。
- 补齐较早快照字段：`Content.gtax` / `genres`，`Video.poddedupe`，`Dooh.venuetype` / `venuetypetax`。
- Proto 同步：`DurFloors`、`Refresh`/`RefSettings`、`Imp.refresh`、`Geo.regionfips104` 等。
- 文档增加折扣宏 `${AUCTION_DISCOUNT_PCT}` / `${AUCTION_DISCOUNT_CPM}`，并澄清 `${AUCTION_PRICE}`。
- `VERSION` / OpenAPI / schema `$comment` 钉住 `2.6-202606`。
- Go / Java / Rust SDK：按广告形态（banner/video/audio/native）的 BidRequest / BidResponse **构建器**，支持 `BuildValidated` 与 schema 对齐。
- Schema / 构建器强校验：`BidRequest.at`、`BidRequest.cur`（≥1 币种）、`BidResponse.cur` 必填。
- OpenAPI `NoBidResponse` 示例补 `cur`；负例夹具对齐新必填；`validate.py` 校验 OpenAPI 嵌入示例。
- 全字段覆盖测试：`testdata/full/`（App/Site/Dooh 请求 + 完整响应）；Go/Java 反射校验每个 proto 字段可设值，三语言 + `make validate` 均 schema 通过。
- Java Validator：`$id` 映射到 classpath，离线校验不再请求 GitHub。

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

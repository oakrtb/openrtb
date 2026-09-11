# OakRTB

[![CI](https://github.com/oakrtb/openrtb/actions/workflows/ci.yml/badge.svg)](https://github.com/oakrtb/openrtb/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Release](https://img.shields.io/github/v/release/oakrtb/openrtb?include_prereleases)](https://github.com/oakrtb/openrtb/releases)

**English** · [中文](#oakrtb-竞价协议)

OakRTB is a machine-readable **OpenRTB-compatible** bidding protocol (JSON Schema, OpenAPI, optional protobuf) plus **Go / Java / Rust** SDKs for models, builders, hot-path `view` (LightGate), optional `fit`, and off-path `schema` validation.

It does **not** implement an SSP, DSP, or auction engine. It is an **independent project**, not an official IAB Tech Lab product. Field names follow IAB OpenRTB **2.6-202606** (see [NOTICE](NOTICE)).

**Current version:** `0.2.0` · wire `x-openrtb-version: 2.6`

### Install SDKs

```bash
# Go
go get github.com/oakrtb/openrtb/sdk/go@v0.2.0

# Rust
cargo add oakrtb-sdk@0.2.0   # after crates.io publish

# Java (Maven) — after Central publish
# <dependency>
#   <groupId>com.oakrtb</groupId>
#   <artifactId>oakrtb-sdk</artifactId>
#   <version>0.2.0</version>
# </dependency>
```

Until Maven Central / crates.io are live, clone this repo and depend on the `sdk/` tree, or use `make jar` for local Java artifacts under `gen/java/dist/`.

Hot path: **build → view (LightGate) → fit**; full JSON Schema stays off the hot path. See [docs/sdk.md](docs/sdk.md) and [docs/view-usage.md](docs/view-usage.md).

### Validate fixtures

```bash
python3 -m pip install -r scripts/requirements.txt
make validate && make proto-check && make sdk-test
```

### Contributing / security

- [CONTRIBUTING.md](CONTRIBUTING.md)
- [SECURITY.md](SECURITY.md)
- [CHANGELOG.md](CHANGELOG.md) · [docs/versioning.md](docs/versioning.md)

---

# OakRTB 竞价协议

https://github.com/oakrtb/openrtb

版本 **0.2.0**，JSON 线格式对齐 [IAB OpenRTB **2.6-202606**](https://github.com/InteractiveAdvertisingBureau/openrtb2.x/releases/tag/2.6-202606)（`x-openrtb-version` 仍填 `2.6`）。

本仓库定义协议（对象、字段、传输与校验），并提供 Go / Java / Rust **模型生成与统一校验 SDK**。不实现 SSP / DSP / 拍卖引擎。**非 IAB 官方实现**；对象名与字段名遵循 OpenRTB，见 [NOTICE](NOTICE)。

## 这份规范管什么

| 层 | 本仓库中的定义 | 路径 |
|---|---|---|
| 传输 | HTTP POST、状态码、多算法压缩、`x-openrtb-version` | [docs/transport.md](docs/transport.md)、[openapi/openrtb.yaml](openapi/openrtb.yaml) |
| JSON 对象 | BidRequest / BidResponse 及全部子对象 | [schema/jsonschema](schema/jsonschema)、[docs/objects.md](docs/objects.md) |
| Native | `imp.native.request` 内嵌的 Native 1.2 | [schema/jsonschema/native.schema.json](schema/jsonschema/native.schema.json) |
| 二进制 | 与 JSON 字段同名的 protobuf | [proto/oakrtb/v2/openrtb.proto](proto/oakrtb/v2/openrtb.proto) |
| SDK | Go / Java / Rust 模型 + 构建器 + view 读模型 + fit 契合检查 + `Report`（`schema` 模块） | [docs/sdk.md](docs/sdk.md)、[docs/view-usage.md](docs/view-usage.md)、[sdk/](sdk/) |
| 校验失败体 | 统一 `Report` JSON（形状见 `validation-result.schema.json`；建议作 HTTP 400） | [schema/jsonschema/validation-result.schema.json](schema/jsonschema/validation-result.schema.json) |

权威顺序：**JSON Schema 为准**。OpenAPI 描述 HTTP 面；protobuf 是可选编码。文档解释语义。

## 安装 SDK

```bash
# Go（需仓库已打 v0.2.0 tag）
go get github.com/oakrtb/openrtb/sdk/go@v0.2.0

# Rust（crates.io 发布后）
cargo add oakrtb-sdk@0.2.0

# Java：Maven Central 发布前可用本地包
make jar   # → gen/java/dist/oakrtb-sdk-0.2.0*.jar（JDK 21）
```

热路径：`build → view（LightGate）→ fit`；完整 schema 用于 `buildValidated` / 夹具 / 可选边界 400，勿与 LightGate 叠跑。

## 一次拍卖

```
Publisher ──► Exchange ──POST /openrtb/v2/auction──► Bidder
                 ▲                                      │
                 └──────── BidResponse / 204 ───────────┘
                 │
                 ├─ nurl  赢价通知
                 ├─ burl  计费通知
                 └─ lurl  丢单通知
```

- `200` + `seatbid`：出价
- `204`：不竞价
- `200` + `nbr`：不竞价并带原因
- `400`：报文无法解析，或（可选）未通过边界 schema 校验（body 为 `Report` JSON）。热路径结构门禁用 LightGate，勿与 schema 叠跑

完整语义见 [docs/spec.md](docs/spec.md)。

## 校验样例

```bash
python3 -m pip install -r scripts/requirements.txt
make validate
make proto-check   # 需要系统安装 protoc
make sdk-test      # Go / Java / Rust
make jar           # 产出 gen/java/dist/oakrtb-sdk-*.jar（需 JDK 21）
```

`examples/bid-request/` 与 `examples/bid-response/` 必须通过 schema；`testdata/full/` 为全字段填充样例；`testdata/invalid/` 必须被拒绝。SDK 用法见 [docs/sdk.md](docs/sdk.md)。贡献见 [CONTRIBUTING.md](CONTRIBUTING.md)；安全报告见 [SECURITY.md](SECURITY.md)。

## 版本

| OakRTB | 兼容 OpenRTB | 说明 |
|---|---|---|
| 0.2.0 | 2.6-202606 | 对齐最新 dated snapshot（Content 直播字段、折扣宏等）；SDK `view`/`schema`/`fit` 命名落地 |
| 0.1.0 | 2.6 | 首个可校验的对象模型与 HTTP 合同 |

新增可选字段不升主版本。删除或改变必填字段才升主版本。详见 [docs/versioning.md](docs/versioning.md)。

## 版权

代码与 schema 为 Apache-2.0。对象名与 JSON 字段名遵循 IAB Tech Lab OpenRTB 2.6（CC BY 3.0），见 [NOTICE](NOTICE)。

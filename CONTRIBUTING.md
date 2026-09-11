# Contributing to OakRTB

感谢贡献。本仓库定义 **OpenRTB 兼容的竞价协议**（JSON Schema / OpenAPI / protobuf）与 Go / Java / Rust SDK；**不**实现 SSP、DSP 或拍卖引擎。

Thanks for contributing. This repo defines an **OpenRTB-compatible bidding protocol** and SDKs. It does **not** implement an exchange or bidder.

## 开发环境 / Dev setup

- Python 3.12+（`scripts/validate.py`）
- `protoc`（`make proto-check`）
- Go **1.25+**、JDK **21**、Rust **1.85+**（跑 SDK 测试）

```bash
python3 -m pip install -r scripts/requirements.txt
make validate
make proto-check
make sdk-test          # 或 sdk-test-go / sdk-test-java / sdk-test-rust
```

改 `schema/jsonschema/` 或 `proto/` 后必须：

```bash
make sync-schemas      # 刷新 Go / Java / Rust vendored 副本
make sdk-test
```

并提交更新后的 `sdk/go/schema/schemas/`、`sdk/java/src/main/{resources,proto}/`、`sdk/rust/{schemas,proto}/`。

发包（crates.io / Maven Central）见 [docs/publishing.md](docs/publishing.md)。

## 改什么、怎么改

权威顺序见 [docs/versioning.md](docs/versioning.md)：

1. `schema/jsonschema/`（JSON 校验权威）
2. `proto/oakrtb/v2/openrtb.proto`（若字段走 protobuf）
3. `openapi/openrtb.yaml`（若改 HTTP 语义 / 示例 / 头）
4. `docs/`、`examples/`、`testdata/`
5. `CHANGELOG.md` + 必要时 `VERSION`

热路径约定：`build → view (LightGate) → fit`；完整 schema 留在 `schema` 模块 / `buildValidated` / 边界 400，**不要**与 LightGate 叠跑。

破坏性 SDK / 协议变更须在 CHANGELOG 标明 **Breaking**，并遵循 SemVer（见 `versioning.md`）。

## Pull request

- 从最新 `main` 开分支；一个 PR 聚焦一件事
- 本地 CI 等价命令：`make validate && make proto-check && make sdk-test`
- 描述里写清：动机、破坏性与否、如何验证
- 勿提交密钥、本地 IDE 杂项；`gen/` 构建产物按需更新

## 行为准则

请保持专业、尊重。严重问题可私下联系仓库维护者（见 [SECURITY.md](SECURITY.md)）。

## 许可

贡献默认按仓库 [LICENSE](LICENSE)（Apache-2.0）授权。对象名 / JSON 字段名仍遵循 IAB OpenRTB（见 [NOTICE](NOTICE)）；本项目为**独立实现，非 IAB 官方**。

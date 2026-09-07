# 版本策略

OakRTB 版本写在 `VERSION`，遵循 SemVer。

## 兼容 OpenRTB

线格式 JSON 字段名与 IAB OpenRTB 2.6 对齐。当前钉住的规范快照为 **`2.6-202606`**
（https://github.com/InteractiveAdvertisingBureau/openrtb2.x/releases/tag/2.6-202606）。

`x-openrtb-version` 仍填 `2.6`（IAB 线格式主次版本）；OakRTB 自己的 semver 写在 `VERSION`。
文档与 CHANGELOG 应同时写明所对齐的 dated snapshot。

## 什么算兼容变更

- 新增可选字段或对象
- 新增枚举值（收发双方必须忽略未知枚举）
- 文档澄清

这些只增加 `VERSION` 的 MINOR 或 PATCH，不改 HTTP 路径 `/openrtb/v2/auction`。

## 什么算破坏性变更

- 删除字段
- 把可选改为必填
- 改变字段类型或含义（例如价格单位）
- `site`/`app`/`dooh` 互斥规则变化

破坏性变更升 MAJOR，并视情况使用新路径（例如 `/openrtb/v3/auction`）。

## Schema / Proto / OpenAPI

| 产物 | 角色 |
|---|---|
| `schema/jsonschema/` | **JSON 校验权威**（required、互斥、数值范围） |
| `proto/oakrtb/v2/openrtb.proto` | 二进制编解码；注释对齐语义，**不**强制 required / oneOf |
| `openapi/openrtb.yaml` | JSON HTTP 合同（`$ref` schema）；不建模 protobuf |

改对象时同一 PR 必须同时更新：

1. `schema/jsonschema/`
2. `proto/oakrtb/v2/openrtb.proto`（若该字段走 protobuf）
3. `openapi/openrtb.yaml`（若改 HTTP 语义、示例或头）
4. `docs/objects.md` / `docs/spec.md` / `docs/transport.md`
5. `examples/bid-request|bid-response/` 与 `testdata/invalid/`
6. `CHANGELOG.md`
7. 若改了 schema：`make sync-schemas`（刷新 Go `validate/schemas/*.json`）并跑 `make sdk-test`；提交更新后的 Go 副本

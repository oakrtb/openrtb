# 版本策略

OakRTB 版本写在 `VERSION`，遵循 SemVer。

## 兼容 OpenRTB

线格式 JSON 字段名与 IAB OpenRTB 2.6 对齐。`x-openrtb-version` 填 `2.6`，表示对象模型来源，不是 OakRTB 自己的 semver。

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

## Schema 与文档

JSON Schema 是校验权威。改对象时同一 PR 必须同时更新：

1. `schema/jsonschema/`
2. `proto/oakrtb/v2/openrtb.proto`（若该字段走 protobuf）
3. `docs/objects.md` / `docs/spec.md`
4. `examples/` 与 `testdata/invalid/`
5. `CHANGELOG.md`

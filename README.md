# OakRTB 竞价协议

https://github.com/oakrtb/openrtb

版本 **0.1.0**，JSON 线格式兼容 [IAB OpenRTB 2.6](https://github.com/InteractiveAdvertisingBureau/openrtb2.x)。

本仓库只定义协议：对象、字段、传输与校验。不实现 SSP / DSP / 拍卖引擎。

## 这份规范管什么

| 层 | 本仓库中的定义 | 路径 |
|---|---|---|
| 传输 | HTTP POST、状态码、gzip、`x-openrtb-version` | [docs/transport.md](docs/transport.md)、[openapi/openrtb.yaml](openapi/openrtb.yaml) |
| JSON 对象 | BidRequest / BidResponse 及全部子对象 | [schema/jsonschema](schema/jsonschema)、[docs/objects.md](docs/objects.md) |
| Native | `imp.native.request` 内嵌的 Native 1.2 | [schema/jsonschema/native.schema.json](schema/jsonschema/native.schema.json) |
| 二进制 | 与 JSON 字段同名的 protobuf | [proto/oakrtb/v2/openrtb.proto](proto/oakrtb/v2/openrtb.proto) |

权威顺序：**JSON Schema 为准**。OpenAPI 描述 HTTP 面；protobuf 是可选编码。文档解释语义。

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
- `400`：报文无法解析

完整语义见 [docs/spec.md](docs/spec.md)。

## 校验样例

```bash
python3 -m pip install -r scripts/requirements.txt
make validate
```

`examples/` 必须通过 schema；`testdata/invalid/` 必须被拒绝。

## 版本

| OakRTB | 兼容 OpenRTB | 说明 |
|---|---|---|
| 0.1.0 | 2.6 | 首个可校验的对象模型与 HTTP 合同 |

新增可选字段不升主版本。删除或改变必填字段才升主版本。详见 [docs/versioning.md](docs/versioning.md)。

## 版权

代码与 schema 为 Apache-2.0。对象名与 JSON 字段名遵循 IAB Tech Lab OpenRTB 2.6（CC BY 3.0），见 [NOTICE](NOTICE)。

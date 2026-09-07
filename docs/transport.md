# 传输

OakRTB 竞价走 HTTP。BidRequest 必须 **POST**，以便承载较大 JSON 或二进制体。

## 端点

默认路径：`POST /openrtb/v2/auction`

完整合同见 `openapi/openrtb.yaml`。Exchange 与 Bidder 集成时可改路径，但语义不变。

## 头

| 头 | 方向 | 说明 |
|---|---|---|
| `Content-Type: application/json` | 双向 | JSON 线格式 |
| `Content-Type: application/x-protobuf` | 双向 | 可选，对应 `proto/oakrtb/v2/openrtb.proto` |
| `x-openrtb-version: 2.6` | 请求必填建议；响应可选 | 线格式主次版本；对象模型对齐快照 2.6-202606 |
| `Accept-Encoding` | Exchange → Bidder | 可接受的响应压缩算法，逗号分隔，可带 `q` 权重 |
| `Content-Encoding` | 双向 | 当前 body 使用的压缩算法（单一 token） |

缺 `Content-Type` 时按 `application/json` 处理。

## 压缩

Body 压缩与对象 schema 无关，只作用于 HTTP 实体。OakRTB **支持多种**标准 `Content-Encoding`：

| 算法 | `Content-Encoding` / `Accept-Encoding` token | 说明 |
|---|---|---|
| 无压缩 | （省略头） | 明文 body；体积极小时可用 |
| gzip | `gzip` | **推荐默认**；实现最广 |
| deflate | `deflate` | zlib/deflate（RFC 1950/1951）；勿与 raw DEFLATE 混淆 |
| Brotli | `br` | 通常比 gzip 更小；需双方均支持 |
| Zstandard | `zstd` | 高速高比；需双方均支持 |

### 协商规则

1. Exchange 在请求上发 `Accept-Encoding`，列出自己能解压的算法，例如：  
   `Accept-Encoding: gzip, br, zstd;q=0.8, deflate;q=0.5`  
2. Bidder 从该列表中选一种自己也支持的算法压缩响应，并设置对应的 `Content-Encoding`。  
3. 若请求未带 `Accept-Encoding`，响应默认 **不压缩**；若双方事先约定，也可用 `gzip`。  
4. 请求 body 也可压缩：Exchange 设置 `Content-Encoding`，Bidder 必须能解压所列算法；**未在集成约定中声明的算法不得使用**。  
5. `Content-Encoding` 一次只声明一种算法（不链式叠多个）。  
6. 压缩作用于完整 body（JSON 或 protobuf 字节），解压后再按 `Content-Type` 解析。

实现方至少支持 **identity（不压缩）+ gzip**；`br` / `zstd` / `deflate` 为可选增强。

## 状态码

| 码 | 何时 |
|---|---|
| 200 | 有 JSON 体：出价，或带 `nbr` 的不竞价 |
| 204 | 合法请求，选择不竞价，无 body |
| 400 | 体损坏、无法解析，或未通过 JSON Schema 校验 |

不要用 4xx/5xx 表示“不想买”。那会让 Exchange 把端点标为不健康。

## 校验失败响应（400）

Body 使用统一的 `ValidationResult`（见 `schema/jsonschema/validation-result.schema.json`）：

```json
{
  "ok": false,
  "errors": [
    {
      "code": "required",
      "path": "/imp",
      "message": "'imp' is a required property"
    }
  ]
}
```

| 字段 | 说明 |
|---|---|
| `ok` | 是否通过 |
| `errors[].code` | `required` / `type` / `format` / `parse` / `native` / `constraint` |
| `errors[].path` | JSON Pointer（RFC 6901） |
| `errors[].message` | 人类可读说明 |

Go / Java / Rust SDK 的 `ValidateBidRequest` / `ValidateBidResponse` 返回同一形状；调用方可直接序列化为 400 响应体。

## 时延

`BidRequest.tmax` 是 Exchange 愿意等待的 **含网络往返** 毫秒数。超时等价于 204，同一拍卖不重试。

Keep-Alive（HTTP 持久连接）是必需的性能手段。

## 安全

生产环境只用 HTTPS。

## 编码约定

- JSON 对象缺字段 = unknown（除非规范给了 default）
- 必须忽略未知字段，以便向前兼容
- 扩展统一放在 `ext`
- 响应编码必须与请求一致（JSON 对 JSON，protobuf 对 protobuf）

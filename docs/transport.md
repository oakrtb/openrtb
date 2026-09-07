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
| `x-openrtb-version: 2.6` | 请求必填建议；响应可选 | 协议版本，解析前即可读取 |
| `Accept-Encoding: gzip` | Exchange → Bidder | Bidder 可用 gzip 压缩响应 |
| `Content-Encoding: gzip` | 双向 | 声明当前 body 已压缩 |

请求也可以 gzip，但必须事先约定。缺 `Content-Type` 时按 `application/json` 处理。

## 状态码

| 码 | 何时 |
|---|---|
| 200 | 有 JSON 体：出价，或带 `nbr` 的不竞价 |
| 204 | 合法请求，选择不竞价，无 body |
| 400 | 体损坏或无法解析 |

不要用 4xx/5xx 表示“不想买”。那会让 Exchange 把端点标为不健康。

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

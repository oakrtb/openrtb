# 协议规范

OakRTB 0.2.0 定义 Exchange（供给）与 Bidder（需求）之间的实时竞价接口。JSON 字段名、对象层次与语义对齐 IAB OpenRTB **2.6-202606**，便于对接现有 DSP / SSP。

配套文档：

- [transport.md](transport.md) — HTTP、压缩、超时
- [objects.md](objects.md) — 对象与必填字段
- [inspect-usage.md](inspect-usage.md) — SDK inspect / Pipeline 使用与 2.6 接入对照
- [versioning.md](versioning.md) — 兼容策略

机器可读定义：

- `schema/jsonschema/bid-request.schema.json` — **JSON 校验权威**（必填、互斥、类型约束）
- `schema/jsonschema/bid-response.schema.json`
- `schema/jsonschema/native.schema.json`
- `proto/oakrtb/v2/openrtb.proto` — protobuf **编解码**（不能表达 schema 的 required / oneOf；缺省 0 ≠ JSON 缺字段）
- `openapi/openrtb.yaml` — **JSON HTTP** 路径/状态码/头（对象体 `$ref` schema；protobuf 见 transport）

`Bid.mtype`：JSON Schema 仅允许 `1–4`；proto 枚举含 `UNSPECIFIED=0` 表示未设。多形态 Imp 出价前须选定并写出非 0 的 `mtype`。

## 角色

| 术语 | 含义 |
|---|---|
| Exchange | 发起拍卖、广播 BidRequest、按拍卖规则选胜出者 |
| Bidder | 接收 BidRequest、在 `tmax` 内返回 BidResponse |
| Seat | Bidder 代理的广告主 / 代理商账户 |
| Imp | 一次可售广告位 |
| Deal | 买卖双方事先约定的私有交易条款 |

## 对象树

```
BidRequest
├── id, at, tmax, test, cur, wseat/bseat, bcat, badv
├── imp[]                  必填，至少一个
│   ├── banner | video | audio | native
│   └── pmp.deals[]
├── site | app | dooh      三选一
├── device, user
├── source.schain
└── regs

BidResponse
├── id                     必须等于 BidRequest.id
├── seatbid[].bid[]
│   ├── impid, price, adm
│   └── nurl / burl / lurl
└── nbr                    仅用于结构化不竞价
```

`site`、`app`、`dooh` 不能同时出现。每个 `imp` 至少带一种广告形态；一条 Bid 只能对应其中一种，用 `mtype` 标明（1 banner / 2 video / 3 audio / 4 native）。

## 必填规则

字段级权威以 JSON Schema 为准。下列为 OakRTB **profile**（相对 IAB OpenRTB 2.6 最小集有收紧，见下节）。

BidRequest：

- `id`、`imp`（长度 ≥ 1）
- `at`（≥ 1；拒绝未指定的 0）
- `cur`（至少 1 个 ISO-4217）
- 每个 `imp.id`
- 每个 `imp` 至少有 `banner`、`video`、`audio`、`native` 之一
- `video.mimes`、`audio.mimes`、`native.request` 在对应形态下必填

BidResponse：

- `id`（必须等于 BidRequest.id）
- `cur`（出价币种；构建器默认 USD，须落在请求 `cur` 允许集合内）
- 若出价：`seatbid` 至少 1 个，每个含至少 1 条 `bid`
- 每条 `bid`：`id`、`impid`、`price`（CPM，必须 > 0）
- `impid` 必须指向请求中某个 `imp.id`

### 相对 IAB OpenRTB 2.6 的差异

| 项 | IAB 2.6 | OakRTB |
|---|---|---|
| BidRequest.`at` | 可选，缺省常按 2（二价+）理解 | **必填**（≥1） |
| BidRequest.`cur` | 可选 | **必填**（≥1） |
| BidResponse.`cur` | 可选，缺省常 USD | **必填** |

对接只认 IAB 最小集的旧流量时，缺 `at`/`cur` 会被 schema / LightGate 拒绝。对象名与语义仍对齐 2.6-202606。

缺字段表示 **unknown**，不是默认 0（规范写明 default 的字段除外）。未知字段必须忽略。扩展放在 `ext`。

## 拍卖与价格

| `at` | 含义 |
|---|---|
| 1 | 一价：成交价 = 出价 |
| 2 | 二价+（IAB 常见缺省；OakRTB 仍须显式下发） |
| 3 | 仅 Deal：`bidfloor` 即约定成交价 |
| ≥ 500 | Exchange 自定义 |

`price` 与 `bidfloor` 单位都是 **CPM**。实际成交的是单次曝光。处理金额时用十进制（例如 Java `BigDecimal`），不要用二进制浮点做账。

`Deal.at` 可覆盖请求级 `at`。`pmp.private_auction = 1` 时只接受列出的 deal。

## 素材与通知

素材优先放在 `bid.adm`。若同时提供 `nurl` 响应体，以 `adm` 为准。

Exchange 在 `nurl` / `burl` / `lurl` 以及 markup 中替换宏：

| 宏 | 含义 |
|---|---|
| `${AUCTION_ID}` | BidRequest.id |
| `${AUCTION_BID_ID}` | BidResponse.bidid |
| `${AUCTION_IMP_ID}` | 中标 imp.id |
| `${AUCTION_SEAT_ID}` | seat |
| `${AUCTION_PRICE}` | 结算成交价（若有折扣，为折扣后买方应付价；见 2.6-202606） |
| `${AUCTION_DISCOUNT_PCT}` | 折扣百分比（OpenRTB 2.6-202606） |
| `${AUCTION_DISCOUNT_CPM}` | 折扣对应的 CPM 金额（OpenRTB 2.6-202606） |
| `${AUCTION_CURRENCY}` | 币种 |
| `${AUCTION_MIN_TO_WIN}` | 赢或平所需最低价 |
| `${AUCTION_LOSS}` | 丢单原因码 |

`nurl` 只表示赢了拍卖，不等于可计费。视频计费以 VAST Impression 为准；`burl` 应在 Exchange 记账点由服务端触发。

## Native

`imp.native.request` 是 **JSON 字符串**，不是对象。1.1+ 根对象即 Native Markup Request（含 `assets[]`）。本仓库用 `native.schema.json` 校验解码后的内层 JSON。

## 合规与供应链

- `regs.coppa` / `regs.gdpr` / `regs.us_privacy` / `regs.gpp`
- GDPR 同意串在 `user.consent`
- `source.schain` 描述支付链路；`complete = 1` 表示从媒体主到本发送方节点齐全
- `device.ifa` 为操作系统广告 ID；`user.eids` 为第三方身份（如 UID2）

## 样例

见 `examples/bid-request/` 与 `examples/bid-response/`。OakRTB 最小 Banner 请求需要：`id`、`at`、`cur`（≥1）、以及至少一个带 `banner` 的 `imp`。生产流量还应带 `site` / `app` / `dooh` 之一、`device`、`source.schain`。

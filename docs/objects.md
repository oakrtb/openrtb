# 对象字典

字段级约束以 JSON Schema 为准。这里只列层次和必填项。

## BidRequest

| 字段 | 必填 | 说明 |
|---|---|---|
| `id` | 是 | 本场拍卖 ID |
| `imp` | 是 | 至少一个 Imp |
| `at` | 是 | 拍卖类型（≥1；常用 1/2） |
| `cur` | 是 | 可接受币种，至少 1 个 ISO-4217 |
| `site` / `app` / `dooh` | 推荐，互斥 | 库存所在媒体 |
| `device` | 推荐 | 投放设备 |
| `user` | 推荐 | 受众 |
| `tmax` | 否 | 超时毫秒 |
| `source` | 否 | 上游决策与 schain |
| `regs` | 否 | 合规信号 |

## Imp

| 字段 | 必填 | 说明 |
|---|---|---|
| `id` | 是 | 请求内唯一，Bid.impid 回填此值 |
| `banner` / `video` / `audio` / `native` | 至少一种 | 可同时出现，一标只中一种 |
| `bidfloor` | 否 | CPM 底价 |
| `secure` | 否 | 1 = 素材必须 HTTPS |
| `pmp` | 否 | 私有交易 |
| `rwdd` | 否 | 激励视频 |
| `ssai` | 否 | 服务端广告插入 |

## Banner / Video / Audio / Native

- Banner：推荐 `w`/`h` 或 `format[]`
- Video：必填 `mimes`；CTV pod 用 `podid` / `poddur` / `slotinpod`；`poddedupe` 可选
- Audio：必填 `mimes`
- Native：必填 `request`（JSON 字符串）

## Content（2.6-202606）

| 字段 | 说明 |
|---|---|
| `livestream` | 0=非排期(VOD等)，1=排期/线性（≠是否“正在直播发生”） |
| `realtime` | 0=非实时(回放等)，1=观看时事件正在实时发生 |
| `firstbroadcast` | 0=非首播，1=首次对观众播出 |
| `gtax` / `genres` | 类型 taxonomy 与 ID（2.6-202501+） |

## BidResponse

| 字段 | 必填 | 说明 |
|---|---|---|
| `id` | 是 | 等于 BidRequest.id |
| `cur` | 是 | 出价币种（ISO-4217；构建器默认 USD） |
| `seatbid` | 出价时是 | 至少一个 SeatBid |
| `nbr` | 否 | 不竞价原因 |

## Bid

| 字段 | 必填 | 说明 |
|---|---|---|
| `id` | 是 | Bidder 生成 |
| `impid` | 是 | 对应 Imp.id |
| `price` | 是 | CPM，> 0 |
| `adm` | 推荐 | 素材 markup |
| `adomain` | 推荐 | 广告主域名，供屏蔽检查 |
| `crid` | 推荐 | 创意 ID |
| `mtype` | 多形态 imp 时推荐 | 1–4 |
| `dealid` | deal 出价时是 | 对应 Deal.id |

完整属性表见 `schema/jsonschema/openrtb.schema.json` 中的 `$defs`。

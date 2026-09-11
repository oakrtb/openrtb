# SDK 与校验

OakRTB 提供 Go / Java / Rust 的**模型生成**、**请求/响应构建器**与**统一校验**（不包含 HTTP 服务或拍卖引擎）。

## 模块术语

| 模块 | 职责 | 不是 |
|---|---|---|
| `schema` | JSON Schema 合同 / `buildValidated` / 工具链（**非**热路径必经） | 业务拒投；与 LightGate 叠跑 |
| `view` | 热路径**读模型**；**LightGate** = 解码后唯一结构门禁 | 完整 schema / 拍卖 |
| `fit` | 可选**形态就绪 + bid↔imp 契合**（ERROR/WARN；业务不抛，Java null→NPE） | 人群/库存匹配引擎 |
| `build` | 构造 + Oak 护栏 | schema 本体 |

`MarkupMask`：Imp 上 banner/video/audio/native 的位掩码（≠ proto `Banner.Format` 尺寸条目）。
`Inventory`：BidRequest 的 site/app/dooh 库存面（≠ proto `Content.Channel` 内容频道）。

## 入站适配：双格式、单模型

HTTP 线格式可以是 **JSON** 或 **protobuf 二进制**（见 [transport.md](transport.md)）。SDK 的设计是：

```text
线格式（二选一）              内存 / 热路径                出站（与请求一致）
─────────────────           ─────────────              ─────────────────
application/json      ──→   BidRequest / BidResponse   ──→ JSON 或 protobuf
application/x-protobuf ──→   （语义模型）                （同 Content-Type）
```

**build / view / fit 只处理「已解码的对象」**，不直接读 HTTP body。  
无论 JSON 还是 protobuf 入站，**先解码成 OpenRTB 对象，再走同一条热路径**。

### 热路径只留一道结构验证：LightGate

JSON 与 protobuf **最终都变成对象**。解码之后不必再叠跑 Schema + LightGate：

| 验证 | 输入 | 热路径角色 |
|------|------|------------|
| **LightGate**（`view` step-0） | 已解码对象 | **推荐唯一门禁**：`id`/`at`/`cur`/`imp`、库存互斥、Imp 至少一种形态 |
| **Schema**（`schema`） | JSON 字节 | **非热路径**：`buildValidated`、夹具/`make validate`、可选的 Exchange 边界 400 |

```text
JSON bytes  ──parse──┐
                     ├──→ BidRequest ──→ LightGate（view）──→ fit / 业务 / build
proto bytes ─parse───┘
```

- **不要**在同一请求上先跑 `schema` 再 `RequestPipeline.run`（LightGate 会再验一遍重叠规则）。
- Proto 没有等价 JSON Schema；对象级门禁本来就只能是 LightGate。
- Schema 仍保留在 SDK 中，但职责收窄为合同/构建自检，不是 DSP 毫秒路径的必经步骤。

### 各模块与输入类型

| 模块 | JSON 线格式 | Proto 对象 / 二进制 |
|------|-------------|---------------------|
| **view（含 LightGate）** | 先解析为对象再调用 | ✅ Java/Go：`run(BidRequest)`；Rust：`run_request(&Value)` |
| **fit** | 同上 | ✅ 在 view Snapshot + Bid 上运行 |
| **build** | ✅ `Json.parse*` / `Unmarshal*` → 对象 | ✅ 构建器产出 proto（Java/Go）或 JSON（Rust） |
| **schema** | ✅ 可选：边界/`buildValidated` | ❌ 无 protobuf Schema；热路径不依赖 |

| 语言 | 热路径主类型 | JSON ↔ 对象 |
|------|-------------|-------------|
| Java | `com.oakrtb.openrtb.v2.BidRequest` | `Json.parseBidRequest` / `Json.toJson` |
| Go | `openrtb.BidRequest` | `build.UnmarshalBidRequest` / `MarshalJSON` |
| Rust | `serde_json::Value`（view/build）；`proto` 用于二进制 | 构建器产出 `Value`；protobuf 经 `prost` 解码后可再序列化为 JSON |

> Rust 当前 build/view 以 **JSON 对象** 为工作面，与 Java/Go 的 **proto 对象** 等价对齐同一 OpenRTB 语义；不是第二套协议。

### 路径 A：JSON 入站（推荐热路径）

```java
// Java — parse → LightGate（经 view）；不先跑 Schema
byte[] wire = ...; // Content-Type: application/json
BidRequest req = Json.parseBidRequest(new String(wire, UTF_8)); // parse 失败 → 400
var snap = RequestPipeline.run(req); // 内含 LightGate；失败 → 400
// … Fit / 定向 / 估价 …
BidResponse res = BidResponseBuilder.create(req.getId())...build();
byte[] out = Json.toJsonBytes(res);
```

```go
// Go
req, err := build.UnmarshalBidRequest(wire) // parse 失败 → 400
snap, err := view.RunRequest(req)        // LightGate 失败 → 400
out, _ := build.MarshalJSON(res)
```

### 路径 B：Protobuf 入站（同一热路径）

```java
// Java
BidRequest req = BidRequest.parseFrom(wire); // parse 失败 → 400
var snap = RequestPipeline.run(req);         // LightGate；与 JSON 路径相同
```

```go
// Go
req := &openrtb.BidRequest{}
if err := proto.Unmarshal(wire, req); err != nil { /* 400 parse */ }
snap, err := view.RunRequest(req)
out, _ := proto.Marshal(res)
```

### 统一热路径（解码之后相同）

```text
对象（BidRequest）
  → view（LightGate + 读模型）   ← 热路径唯一结构验证
  → fit.impReady（可选）
  → 业务：定向 / 估价
  → build（组 BidResponse）
  → fit.bid / fit.response（可选；Go：Bid / Response）
  → view（响应 LightGate + 读模型，可选）
  → 编码回 JSON 或 protobuf
```

Schema 仅在需要时使用，例如：`buildValidated()`、本地夹具、或 Exchange 侧「非法 JSON → 400 `Report`」合同——**不要与 LightGate 叠跑**。

### 约定与注意

1. **响应编码与请求一致**：JSON 对 JSON，protobuf 对 protobuf（[transport.md](transport.md)）。
2. **热路径验证 = LightGate**；Schema 为可选边界/构建工具，非必经。
3. **proto3 缺省 0/"" ≠ JSON 缺字段**：LightGate 按对象字段语义判断；完整类型/pattern 约束仍在 Schema（工具链用）。
4. **SDK 不提供 HTTP 服务**：Content-Type、压缩、`204` / `200+nbr` 由集成方处理；见 [view-usage.md](view-usage.md) HTTP 对照表。

## 权威与产物

| 角色 | 路径 |
|---|---|
| 校验权威 | `schema/jsonschema/*.schema.json` |
| 统一结果（`Report` JSON 形状） | `schema/jsonschema/validation-result.schema.json` |
| Protobuf | `proto/oakrtb/v2/openrtb.proto` |
| Go 模型 / 构建器 / 视图 | `sdk/go/oakrtb/v2`、`sdk/go/build`、`sdk/go/view`、`sdk/go/fit` |
| Java 模型 / 构建器 / 视图 | `com.oakrtb.openrtb.v2`、`com.oakrtb.sdk.build`、`com.oakrtb.sdk.view`、`com.oakrtb.sdk.fit` |
| Rust 模型 / 构建器 / 视图 | `oakrtb_sdk::proto`、`oakrtb_sdk::build`、`oakrtb_sdk::view`、`oakrtb_sdk::fit` |
| 手写 schema 层 | `sdk/{go,java,rust}/schema` |

## 构建

权威 JSON Schema 只维护在仓库根 `schema/jsonschema/`。各语言摄入方式：

| 语言 | 摄入方式 |
|---|---|
| Java | `maven-resources-plugin` → jar（构建时拷入，不入库） |
| Rust | `build.rs` → `OUT_DIR`（构建时拷入，不入库） |
| Go | `make sync-schemas` → 提交 `sdk/go/schema/schemas/*.json`（`//go:embed`，`go get` 可用） |

```bash
make sync-schemas  # 改 schema 后刷新 Go 副本并提交
make proto-go
make proto-java
make proto-rust
make sdk-test
make jar
```

## 怎么按广告类型组请求

看 `imp` 子对象决定形态；构建器按形态要求必填字段（请求须设 `at` + `cur`；Banner 要尺寸，Video/Audio 要 `mimes`，Native 要 `request`；响应须有 `cur`），再用 `BuildValidated` / `buildValidated` 跑 schema。

| 形态 | 构建入口（概念） | 必填要点 |
|---|---|---|
| Banner | `NewBannerImp` / `ImpBuilders.banner` / `BannerImpBuilder` | `w`+`h` 或 `format[]` |
| Video | `NewVideoImp` / `ImpBuilders.video` / `VideoImpBuilder` | `mimes`；常加时长、`protocols`、`plcmt` |
| Audio | `NewAudioImp` / `ImpBuilders.audio` / `AudioImpBuilder` | `mimes` |
| Native | `NewNativeImp` / `ImpBuilders.nativeAd` / `NativeImpBuilder` | `request`（Native 1.2 JSON 字符串） |

库存用 `Site` / `App` / `Dooh`（互斥）；设备用 `Device`。

## Schema API

| 语言 | 入口 | 类型 |
|---|---|---|
| Go | `schema.Request` / `schema.Response` | `Report`、`Issue`（`github.com/oakrtb/openrtb/sdk/go/schema`） |
| Java | `Schema.request` / `Schema.response` | `Report`、`Issue`（`com.oakrtb.sdk.schema`） |
| Rust | `schema::request` / `schema::response` | `Report`、`Issue`（`oakrtb_sdk::schema`） |

成功：`{"ok": true, "errors": []}`  
失败：`ok=false`，`errors` 含 `code` / `path` / `message`（`Report` JSON 形状，对齐 `validation-result.schema.json`；可直接作 HTTP 400 body）

## View 流水线（调用方热路径）

完整可拷贝示例见 **[view-usage.md](view-usage.md)**（Java / Go / Rust：组请求 → `RequestPipeline` → 可选 Fit → 组响应 → `ResponsePipeline`）。

解码后的 BidRequest 上跑轻量视图，供定向 / 估价 / 组 BidResponse 使用（**不是**完整 schema 校验）。

推荐用 **RequestPipeline** / **ResponsePipeline** 按序编排，一次拿到可查询的 Snapshot：

**BidRequest**

1. `lightGate` — 必填 `id`/`at`/`cur`/`imp≥1`、site/app/dooh 互斥、每 Imp 至少一种形态；拒绝空白 `id`、空白 `cur[]` 项、空白 `imp.id`  
2. `shared` — SharedView（库存面、设备、屏蔽列表、`tmax` 的 85% deadline）  
3. `imps` — `ImpView[]`（MarkupMask 位掩码、底价、banner/video/audio/native）  
4. `snapshot` / `facts()` — 扁平事实（`mtype`、尺寸、`native.request` 等）

**Fit（可选，不进 LightGate）**

- `impReady` — 选定形态后检查尺寸 / `mimes` / `native.request`  
- `bid` / `response` — `impid`、`mtype`↔MarkupMask；floor / battr / badv 等为 WARN（`ok()` 仍 true）；floor 须响应 `cur` 与 `imp.bidfloorcur` 均非空白且相等才比价（**不**隐式 USD；单条 `bid` 无响应 cur 则跳过）；响应 `cur` 与请求 `cur[]` 比较前均 trim/strip；空/`nil` `seatbid[].bid` → `MALFORMED`（与 LightGate 对齐）  
- 包：Java `com.oakrtb.sdk.fit`、Go `sdk/go/fit`、Rust `oakrtb_sdk::fit`

**BidResponse**

1. `lightGate` — 必填 `id`/`cur`（拒绝空白）；有 `seatbid` 时须为数组且每 bid 须 `id`+`impid`+`price>0`；空 `seatbid` 视为 no-bid（wire）；构建器须显式 `noBid(nbr)`  
2. `shared` — 响应 SharedView（`bidid`/`nbr`/`noBid`）  
3. `bids` — `SeatBidView` + 扁平 `BidView`（`mtype`→MarkupMask；不重算 shared）  
4. `snapshot` / `facts()` — `findBid` / `bidsForImp` / `bidsWith` / `BidFact`

`noBid` 与 `addSeatBid` 互斥：后调用会清掉对方状态（清 `seatbid` 或清 `nbr`）。

| 语言 | BidRequest | BidResponse |
|---|---|---|
| Go | `view.RunRequest(req)` | `view.RunResponse(res)` |
| Java | `RequestPipeline.run(req)` | `ResponsePipeline.run(res)` |
| Rust | `view::run_request(&Value)` | `view::run_response(&Value)` |

底层步骤：`RequestViews` / `ResponseViews`（Go：`LightGateRequest` / `LightGateResponse` 等）。

### Go

```go
import "github.com/oakrtb/openrtb/sdk/go/view"

snap, err := view.RunRequest(req)
if err != nil { /* LightGate 失败 */ }
for _, f := range snap.Facts() {
    if f.HasBanner() {
        // f.Mtype, f.BidFloor, f.BannerW …
    }
}
if snap.PastDeadline() { /* 接近 tmax */ }

rsnap, err := view.RunResponse(res)
for _, f := range rsnap.Facts() {
    if f.HasBanner() && f.HasAdm {
        // f.Price, f.ImpID, f.Mtype …
    }
}
```

### Java

```java
import com.oakrtb.sdk.view.RequestPipeline;
import com.oakrtb.sdk.view.ResponsePipeline;

var snap = RequestPipeline.run(req);
for (var f : snap.facts()) {
  if (f.hasBanner()) {
    // f.mtype(), f.bidFloor(), f.bannerW() …
  }
}

var rsnap = ResponsePipeline.run(res);
for (var f : rsnap.facts()) {
  if (f.hasBanner() && f.hasAdm()) {
    // f.price(), f.impid(), f.mtype() …
  }
}
```

### Rust

```rust
use oakrtb_sdk::view;

let snap = view::run_request(&req_json)?;
for f in snap.facts() {
    if f.has_banner() {
        // f.mtype, f.bidfloor, f.banner_w …
    }
}

let rsnap = view::run_response(&res_json)?;
for f in rsnap.facts() {
    if f.has_banner() && f.has_adm {
        // f.price, f.impid, f.mtype …
    }
}
```

### Go（构建器）

```go
import "github.com/oakrtb/openrtb/sdk/go/build"

raw, report, err := build.NewBidRequest("auction-1").
    FirstPrice().
    Tmax(120).
    Currency("USD").
    Site(build.NewSite().ID("s1").Domain("example.com").Page("https://example.com/a").Build()).
    Device(build.NewDevice().UA("Mozilla/5.0").IP("192.0.2.1").DeviceType(4).Build()).
    AddImp(build.NewBannerImp("1").Size(300, 250).Floor(0.03, "USD").Secure().Build()).
    BuildValidated()
if err != nil || !report.Ok {
    // reject / log report.Errors（schema.Report）
}

// 响应
raw, report, err = build.NewBidResponse("auction-1").
    AddSeatBid("512", build.NewBid("1", "1", 1.23).Banner().Size(300, 250).Adm("<img/>").Build()).
    BuildValidated()
```

直接校验 JSON：`schema.Request` / `schema.Response`（`github.com/oakrtb/openrtb/sdk/go/schema`）。

模型：`github.com/oakrtb/openrtb/sdk/go/oakrtb/v2`  
JSON：`build.MarshalJSON`（enum 数字 + proto 字段名）

### Java（JDK 21）

```java
import com.oakrtb.sdk.build.*;

var payload = BidRequestBuilder.create("auction-1")
    .firstPrice()
    .tmax(120)
    .currency("USD")
    .site(Parts.site().id("s1").domain("example.com").page("https://example.com/a").build())
    .device(Parts.device().ua("Mozilla/5.0").ip("192.0.2.1").deviceType(4).build())
    .addImp(ImpBuilders.banner("1").size(300, 250).floor(0.03, "USD").secure().build())
    .buildValidated();
if (!payload.ok()) {
    // payload.result() → 400 body
}

var bidPayload = BidResponseBuilder.create("auction-1")
    .addSeatBid("512",
        BidResponseBuilder.bid("1", "1", 1.23).banner().size(300, 250).adm("<img/>").build())
    .buildValidated();
```

```bash
make jar
# gen/java/dist/oakrtb-sdk-0.2.0.jar       — 薄 jar
# gen/java/dist/oakrtb-sdk-0.2.0-all.jar   — shade 依赖
```

Maven 坐标：`com.oakrtb:oakrtb-sdk:0.2.0`（本地 `mvn install`）

### Rust

```rust
use oakrtb_sdk::build::*;

let v = BidRequestBuilder::new("auction-1")
    .first_price()
    .tmax(120)
    .currency(&["USD"])
    .site(SiteBuilder::new().id("s1").domain("example.com").page("https://example.com/a").build())
    .device(DeviceBuilder::new().ua("Mozilla/5.0").ip("192.0.2.1").device_type(4).build())
    .add_imp(BannerImpBuilder::new("1").size(300, 250).floor(0.03, "USD").secure().build())
    .build_validated()?;
if !v.ok() {
    // v.result → 400 body
}
```

```toml
oakrtb-sdk = { path = "sdk/rust" }
```

生成的 protobuf 类型：`oakrtb_sdk::proto`（编解码）；构建器直接产出 OpenRTB JSON 以便 schema 校验。

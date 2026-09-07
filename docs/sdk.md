# SDK 与校验

OakRTB 提供 Go / Java / Rust 的**模型生成**、**请求/响应构建器**与**统一校验**（不包含 HTTP 服务或拍卖引擎）。

## 模块术语

| 模块 | 职责 | 不是 |
|---|---|---|
| `validate` | 报文是否**合法**（权威 JSON Schema） | 业务拒投 |
| `inspect` | 热路径**读模型**（Pipeline → Snapshot）；LightGate 为 step-0 结构门禁 | 完整 schema / 拍卖 |
| `fit` | 可选**形态就绪 + bid↔imp 契合**（ERROR/WARN，不抛） | 人群/库存匹配引擎 |
| `build` | 构造 + Oak 护栏 | schema 本体 |

`MarkupMask`：Imp 上 banner/video/audio/native 的位掩码（≠ proto `Banner.Format` 尺寸条目）。
`Inventory`：BidRequest 的 site/app/dooh 库存面（≠ proto `Content.Channel` 内容频道）。


## 权威与产物

| 角色 | 路径 |
|---|---|
| 校验权威 | `schema/jsonschema/*.schema.json` |
| 统一结果 | `schema/jsonschema/validation-result.schema.json` |
| Protobuf | `proto/oakrtb/v2/openrtb.proto` |
| Go 模型 / 构建器 / 视图 | `sdk/go/oakrtb/v2`、`sdk/go/build`、`sdk/go/inspect`、`sdk/go/fit` |
| Java 模型 / 构建器 / 视图 | `com.oakrtb.openrtb.v2`、`com.oakrtb.sdk.build`、`com.oakrtb.sdk.inspect`、`com.oakrtb.sdk.fit` |
| Rust 模型 / 构建器 / 视图 | `oakrtb_sdk::proto`、`oakrtb_sdk::build`、`oakrtb_sdk::inspect`、`oakrtb_sdk::fit` |
| 手写校验层 | `sdk/{go,java,rust}/validate` |

## 构建

权威 JSON Schema 只维护在仓库根 `schema/jsonschema/`。各语言摄入方式：

| 语言 | 摄入方式 |
|---|---|
| Java | `maven-resources-plugin` → jar（构建时拷入，不入库） |
| Rust | `build.rs` → `OUT_DIR`（构建时拷入，不入库） |
| Go | `make sync-schemas` → 提交 `sdk/go/validate/schemas/*.json`（`//go:embed`，`go get` 可用） |

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

## 校验 API

| 语言 | 校验入口 |
|---|---|
| Go | `validate.ValidateBidRequest` / `ValidateBidResponse` → `ValidationResult` |
| Java | `Validator.validateBidRequest` / `validateBidResponse` → `ValidationResult` |
| Rust | `validate::validate_bid_request` / `validate_bid_response` → `ValidationResult` |

成功：`{"ok": true, "errors": []}`  
失败：`ok=false`，`errors` 含 `code` / `path` / `message`（可直接作 HTTP 400 body）

## Inspect 流水线（调用方热路径）

完整可拷贝示例见 **[inspect-usage.md](inspect-usage.md)**（Java / Go / Rust：组请求 → `RequestPipeline` → 可选 Fit → 组响应 → `ResponsePipeline`）。

解码后的 BidRequest 上跑轻量视图，供定向 / 估价 / 组 BidResponse 使用（**不是**完整 schema 校验）。

推荐用 **RequestPipeline** / **ResponsePipeline** 按序编排，一次拿到可查询的 Snapshot：

**BidRequest**

1. `lightGate` — 必填 `id`/`at`/`cur`/`imp≥1`、site/app/dooh 互斥、每 Imp 至少一种形态  
2. `pinShared` — `RequestSharedView` / `RequestInspect.SharedView`（频道、设备、屏蔽列表、`tmax` 的 85% deadline）  
3. `viewImps` — `ImpView[]`（MarkupMask 位掩码、底价、banner/video/audio/native）  
4. `snapshot` / `facts()` — 扁平事实（`mtype`、尺寸、`native.request` 等）

**Fit（可选，不进 LightGate）**

- `impReady` — 选定形态后检查尺寸 / `mimes` / `native.request`  
- `bidFit` / `responseFit` — `impid`、`mtype`↔MarkupMask；floor / battr / badv 等为 WARN  
- 包：Java `com.oakrtb.sdk.fit`、Go `sdk/go/fit`、Rust `oakrtb_sdk::fit`

**BidResponse**

1. `lightGate` — 必填 `id`/`cur`；有 `seatbid` 时每 bid 须 `id`+`impid`+`price>0`；空 `seatbid` 视为 no-bid（wire）；构建器须显式 `noBid(nbr)`  
2. `pinShared` — 响应 SharedView（`bidid`/`nbr`/`noBid`）  
3. `viewBids` — `SeatBidView` + 扁平 `BidView`（`mtype`→MarkupMask；不重算 shared）  
4. `snapshot` / `facts()` — `findBid` / `bidsForImp` / `bidsWith` / `BidFact`

`noBid` 与 `addSeatBid` 互斥：后调用会清掉对方状态（清 `seatbid` 或清 `nbr`）。

| 语言 | BidRequest | BidResponse |
|---|---|---|
| Go | `inspect.RunRequest(req)` | `inspect.RunResponse(res)` |
| Java | `RequestPipeline.run(req)` | `ResponsePipeline.run(res)` |
| Rust | `inspect::run_request(&Value)` | `inspect::run_response(&Value)` |

底层步骤：`RequestInspect` / `ResponseInspect`（Go：`LightGateRequest` / `LightGateResponse` 等）。

### Go

```go
import "github.com/oakrtb/openrtb/sdk/go/inspect"

snap, err := inspect.RunRequest(req)
if err != nil { /* LightGate 失败 */ }
for _, f := range snap.Facts() {
    if f.HasBanner() {
        // f.Mtype, f.BidFloor, f.BannerW …
    }
}
if snap.PastDeadline() { /* 接近 tmax */ }

rsnap, err := inspect.RunResponse(res)
for _, f := range rsnap.Facts() {
    if f.HasBanner() && f.HasAdm {
        // f.Price, f.ImpID, f.Mtype …
    }
}
```

### Java

```java
import com.oakrtb.sdk.inspect.RequestPipeline;
import com.oakrtb.sdk.inspect.ResponsePipeline;

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
use oakrtb_sdk::inspect;

let snap = inspect::run_request(&req_json)?;
for f in snap.facts() {
    if f.has_banner() {
        // f.mtype, f.bidfloor, f.banner_w …
    }
}

let rsnap = inspect::run_response(&res_json)?;
for f in rsnap.facts() {
    if f.has_banner() && f.has_adm {
        // f.price, f.impid, f.mtype …
    }
}
```

### Go（构建器）

```go
import (
    "github.com/oakrtb/openrtb/sdk/go/build"
    "github.com/oakrtb/openrtb/sdk/go/validate"
)

raw, result, err := build.NewBidRequest("auction-1").
    FirstPrice().
    Tmax(120).
    Currency("USD").
    Site(build.NewSite().ID("s1").Domain("example.com").Page("https://example.com/a").Build()).
    Device(build.NewDevice().UA("Mozilla/5.0").IP("192.0.2.1").DeviceType(4).Build()).
    AddImp(build.NewBannerImp("1").Size(300, 250).Floor(0.03, "USD").Secure().Build()).
    BuildValidated()
if err != nil || !result.Ok {
    // reject / log result.Errors
}

// 响应
raw, result, err = build.NewBidResponse("auction-1").
    AddSeatBid("512", build.NewBid("1", "1", 1.23).Banner().Size(300, 250).Adm("<img/>").Build()).
    BuildValidated()
```

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

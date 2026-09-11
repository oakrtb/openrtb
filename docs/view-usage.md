# View / Pipeline 使用示例

调用方热路径：解码后的 BidRequest / BidResponse → **LightGate** → **Shared 视图** → **Imp / Bid 视图** →（可选）**Fit** → 组响应 → ResponseViews。

JSON / protobuf 入站均先变成对象；热路径**只跑 LightGate**，不与完整 JSON Schema 叠跑。Schema 留给 `buildValidated` / 夹具 / 可选边界 400（见 [sdk.md 入站适配](sdk.md#入站适配双格式单模型)）。Fit 不进 LightGate，默认 opt-in。

## 一键入口

| | BidRequest | BidResponse | Fit（可选） |
|---|---|---|---|
| Java | `RequestPipeline.run(req)` | `ResponsePipeline.run(res)` | `com.oakrtb.sdk.fit.Fit` |
| Go | `view.RunRequest(req)` | `view.RunResponse(res)` | `github.com/oakrtb/openrtb/sdk/go/fit` |
| Rust | `view::run_request(&json)` | `view::run_response(&json)` | `oakrtb_sdk::fit` |

`MarkupMask` 位掩码：`BANNER|VIDEO|AUDIO|NATIVE`。请求侧表示 Imp 上出现的子对象；响应侧由 `Bid.mtype` 映射。单形态时 `mtype()` → `Bid.mtype`（1–4）。

---

## Fit（RequestViews → Fit → ResponseViews）

在组标前检查形态规格是否就绪，组标后对照请求做 bid↔imp 一致性。返回 `FitResult`（`ok()` = 无 ERROR；低价/屏蔽/超时等为 WARN，`ok()` 仍可为 true——政策拒投请读 `warnings()`/`has()` / Rust `warnings()`）。业务不匹配不抛；Java `null` 参数抛 NPE。也不替代 schema。

底价：仅当响应 `cur` 与 `imp.bidfloorcur` **均非空白且相等**时才比价；**不**把省略的 `bidfloorcur` 隐式当成 USD。`Fit.bid` 无响应货币时跳过底价；完整检查用 `Fit.response`。三语言 Fit 对空/`nil` `seatbid[].bid` 发 `MALFORMED`（与 LightGate 对齐）。Rust JSON 另校验非 array `seatbid` / 非 string `cur`；仍建议先 `run_response`。

| API | 时机 | 作用 |
|---|---|---|
| `impReady` / `ImpReadyMtype` / `imp_ready_mtype` | 选定形态后、出价前 | Banner 尺寸、Video/Audio `mimes`、Native `request`；缺时长/protocols 为 WARN |
| `Fit.bid` / `fit.Bid` / `fit::bid` | 单条 Bid | `impid`、多形态须 `mtype`、`mtype`∈Imp MarkupMask；floor/battr/badv 等为 WARN |
| `Fit.response` / `fit.Response` / `fit::response` | 整包响应 | 遍历 seatbid；空 seatbid（no-bid）仅可能带 `PAST_DEADLINE`（**不**查 CUR）；有 seat 时查 `cur`/floor；空/`nil` `bid[]` → `MALFORMED`（与 LightGate 对齐） |

### Java 片段

```java
import com.oakrtb.sdk.fit.*;

var reqSnap = RequestPipeline.run(req);
for (var imp : reqSnap.imps()) {
  int mtype = imp.markup().count() == 1 ? imp.markup().mtype() : 2; // 业务选定
  var ready = Fit.impReady(imp, mtype);
  if (!ready.ok()) continue;

  Bid bid = BidResponseBuilder.bid("1", imp.id(), 1.2).markupType(
      MarkupType.forNumber(mtype)).build();
  var fitResult = Fit.bid(reqSnap, bid); // 无响应 cur → 不做底价比较
  if (!fitResult.ok()) { /* 丢 ERROR */ }
  if (fitResult.has(IssueCode.PRICE_BELOW_FLOOR)) { /* 政策：低价丢标 */ }
}
// 组完 res 后（整包含 cur → 才有底价 WARN）：
var resFit = Fit.response(reqSnap, res);
if (!resFit.ok()) { /* 丢 ERROR */ }
for (var w : resFit.warnings()) { /* 打点 / 政策拒投 */ }
```

### Go 片段

```go
import "github.com/oakrtb/openrtb/sdk/go/fit"

ready := fit.ImpReadyMtype(&imp, mtype)
fitRes := fit.Bid(reqSnap, bid) // 无响应 cur → 跳过底价
resFit := fit.Response(reqSnap, res)
if !resFit.OK() { /* 丢 ERROR */ }
for _, w := range resFit.Warnings() { /* 政策拒投 */ }
_ = ready
_ = fitRes
```

### Rust 片段

```rust
use oakrtb_sdk::{fit, view};

let req_snap = view::run_request(&req_json)?;
let _res_snap = view::run_response(&res_json)?; // LightGate 结构门
let ready = fit::imp_ready_mtype(&imp, mtype);
let bid_fit = fit::bid(&req_snap, &bid_json); // 无响应 cur → 跳过底价
let res_fit = fit::response(&req_snap, &res_json);
if !res_fit.ok() { /* 丢 ERROR（含畸形 seatbid） */ }
for w in res_fit.warnings() { /* 政策拒投 */ }
```

---

## Java（JDK 21）

### 组请求 → 视图 → 组响应 → 再视图

```java
import com.oakrtb.sdk.build.*;
import com.oakrtb.sdk.view.*;

// 1) 构建 BidRequest
var req = BidRequestBuilder.create("auction-1")
    .firstPrice()
    .tmax(120)
    .currency("USD")
    .site(Parts.site().id("s1").domain("example.com").page("https://example.com/a").build())
    .device(Parts.device().ua("Mozilla/5.0").ip("192.0.2.1").deviceType(4).build())
    .addImp(ImpBuilders.banner("1").size(300, 250).floor(0.03, "USD").secure().build())
    .build();

// 2) 请求流水线
var reqSnap = RequestPipeline.run(req);
System.out.println(reqSnap.auctionId() + " / " + reqSnap.inventory()); // site

for (var f : reqSnap.facts()) {
  if (f.hasBanner()) {
    int mtype = f.mtype();           // 1
    double floor = f.bidFloor();     // 0.03
    Integer w = f.bannerW();         // 300
    // targeting / price …
  }
}
if (reqSnap.pastDeadline()) {
  // 接近 tmax（pin 时起算 85%）→ 提前收束
}

// 3) 组 BidResponse（出价）
var res = BidResponseBuilder.create(reqSnap.auctionId())
    .currency("USD")
    .addSeatBid("512",
        BidResponseBuilder.bid("b1", "1", 1.23)
            .banner()
            .size(300, 250)
            .adm("<img src='https://cdn.example/ad.png'/>")
            .crid("c1")
            .adomain("adv.example")
            .build())
    .build();

// 或结构化不竞价：
// var res = BidResponseBuilder.create(reqSnap.auctionId()).noBid(2).build();

// 4) 响应流水线
var resSnap = ResponsePipeline.run(res);
if (resSnap.noBid()) {
  System.out.println("nbr=" + resSnap.nbr());
} else {
  for (var b : resSnap.facts()) {
    if (b.hasBanner() && b.hasAdm()) {
      // b.price(), b.impid(), b.mtype() …
    }
  }
  resSnap.bidsForImp("1");
  resSnap.bidsWith(MarkupMask.BANNER);
}
```

### 逐步编排（调试 / 插桩）

```java
var snap = RequestPipeline.of(req)
    .lightGate()
    .shared()
    .imps()
    .snapshot();

var rsnap = ResponsePipeline.of(res)
    .lightGate()
    .shared()
    .bids()
    .snapshot();
```

### MarkupMask

```java
MarkupMask f = RequestViews.markupMask(imp);
if (f.hasBanner() && f.hasVideo()) {
  // 多形态：mtype()==0，需调用方自选再写 Bid.mtype
}
int mtype = f.mtype(); // 仅单 bit 时为 1–4
```

### 校验（入库前）

```java
var payload = BidRequestBuilder.create("auction-1")
    /* … */
    .buildValidated();
if (!payload.ok()) {
  // payload.result() → HTTP 400 body
}
```

---

## Go

```go
package main

import (
	"fmt"

	"github.com/oakrtb/openrtb/sdk/go/build"
	"github.com/oakrtb/openrtb/sdk/go/view"
)

func main() {
	req, err := build.NewBidRequest("auction-1").
		FirstPrice().
		Tmax(120).
		Currency("USD").
		Site(build.NewSite().ID("s1").Domain("example.com").Page("https://example.com/a").Build()).
		Device(build.NewDevice().UA("Mozilla/5.0").IP("192.0.2.1").DeviceType(4).Build()).
		AddImp(build.NewBannerImp("1").Size(300, 250).Floor(0.03, "USD").Secure().Build()).
		Build()
	if err != nil {
		panic(err)
	}

	reqSnap, err := view.RunRequest(req)
	if err != nil {
		panic(err) // LightGate 失败
	}
	fmt.Println(reqSnap.AuctionID(), reqSnap.Inventory())

	for _, f := range reqSnap.Facts() {
		if f.HasBanner() {
			_ = f.Mtype // 1
			_ = f.BidFloor
			// targeting / price …
		}
	}
	if reqSnap.PastDeadline() {
		return
	}

	res, err := build.NewBidResponse(reqSnap.AuctionID()).
		Currency("USD").
		AddSeatBid("512",
			build.NewBid("b1", "1", 1.23).Banner().Size(300, 250).
				Adm(`<img src="https://cdn.example/ad.png"/>`).Crid("c1").Adomain("adv.example").Build(),
		).
		Build()
	if err != nil {
		panic(err)
	}

	resSnap, err := view.RunResponse(res)
	if err != nil {
		panic(err)
	}
	if resSnap.NoBid() {
		fmt.Println("nbr", resSnap.Nbr())
		return
	}
	for _, b := range resSnap.Facts() {
		if b.HasBanner() && b.HasAdm {
			fmt.Println(b.ImpID, b.Price, b.Mtype)
		}
	}
	_ = resSnap.BidsWith(view.MarkupBanner)
}
```

逐步：

```go
snap, err := view.OfRequest(req).LightGate().Shared().Imps().Snapshot()
rsnap, err := view.OfResponse(res).LightGate().Shared().Bids().Snapshot()
```

---

## Rust

```rust
use oakrtb_sdk::build::{
    BannerImpBuilder, BidBuilder, BidRequestBuilder, BidResponseBuilder, DeviceBuilder,
    SiteBuilder,
};
use oakrtb_sdk::view::{self, MarkupMask};

fn main() -> Result<(), String> {
    let req = BidRequestBuilder::new("auction-1")
        .first_price()
        .tmax(120)
        .currency(&["USD"])
        .site(
            SiteBuilder::new()
                .id("s1")
                .domain("example.com")
                .page("https://example.com/a")
                .build(),
        )
        .device(
            DeviceBuilder::new()
                .ua("Mozilla/5.0")
                .ip("192.0.2.1")
                .device_type(4)
                .build(),
        )
        .add_imp(
            BannerImpBuilder::new("1")
                .size(300, 250)
                .floor(0.03, "USD")
                .secure()
                .build(),
        )
        .build()?;

    let req_snap = view::run_request(&req)?;
    println!("{} / {:?}", req_snap.auction_id(), req_snap.inventory());

    for f in req_snap.facts() {
        if f.has_banner() {
            let _mtype = f.mtype; // 1
            let _floor = f.bidfloor;
            // targeting / price …
        }
    }
    if req_snap.past_deadline() {
        return Ok(());
    }

    let res = BidResponseBuilder::new(req_snap.auction_id())
        .currency("USD")
        .add_seat_bid(
            "512",
            vec![BidBuilder::new("b1", "1", 1.23)
                .banner()
                .size(300, 250)
                .adm(r#"<img src="https://cdn.example/ad.png"/>"#)
                .crid("c1")
                .adomain(&["adv.example"])
                .build()],
        )
        .build()?;

    let res_snap = view::run_response(&res)?;
    if res_snap.no_bid() {
        println!("nbr={}", res_snap.nbr());
        return Ok(());
    }
    for b in res_snap.facts() {
        if b.has_banner() && b.has_adm {
            println!("{} {} {}", b.impid, b.price, b.mtype);
        }
    }
    let _ = res_snap.bids_with(MarkupMask::BANNER);
    Ok(())
}
```

逐步：

```rust
let snap = view::RequestPipeline::of(&req)
    .light_gate()
    .shared()
    .imps()
    .snapshot()?;

let rsnap = view::ResponsePipeline::of(&res)
    .light_gate()
    .shared()
    .bids()
    .snapshot()?;
```

Rust 构建器产出 `serde_json::Value`；Go/Java 用 protobuf 模型。

---

## 与 OpenRTB 2.6 接入对照

完整 HTTP 合同见 [transport.md](transport.md)；对象必填见 [spec.md](spec.md) / [objects.md](objects.md)。

### 标准 DSP 主路径（对象层）

```
POST BidRequest
  → 解析为对象 + LightGate（热路径唯一结构验证；不叠 Schema）
  → Shared（at / cur / tmax / 库存 / 屏蔽）
  → 遍历 Imp（MarkupMask、floor）
  → Fit.impReady（可选）→ 定向 & 估价 → 组 Bid
  → Fit.bid / Fit.response（可选）
  → 应答（见下表）
```

与上文 `RequestPipeline` →（可选 Fit）→ 组 `BidResponse` 一致。`ResponsePipeline` 用于发出前自检或 Exchange 收包侧，非规范强制步骤。

### HTTP 应答（传输层，SDK 不代发）

| 场景 | HTTP | Body |
|---|---|---|
| 出价 | 200 | `BidResponse` + `seatbid` |
| 不竞价（生产最常见） | **204** | **无 body** |
| 结构化不竞价（联调/可观测） | 200 | `BidResponse` + `id` + `cur` + `nbr`（无 seatbid） |
| 请求非法（parse / LightGate） | 400 | 集成方自定 body；热路径以此为准 |
| 请求非法（可选 Schema 边界） | 400 | `Report` JSON（形状见 `validation-result.schema.json`；`buildValidated` / Exchange 合同） |

SDK 的 `noBid(nbr)` 对应表中第三行。生产大量「不想买」应优先 **204**，不要用 4xx/5xx 表示不竞价。

### OakRTB profile vs IAB 最小集

示例与 LightGate 按 OakRTB 校验：`BidRequest` 须带 `at`、`cur`；`BidResponse` 须带 `cur`。IAB 2.6 原文里这些多为可选（`at` 常按 2、`cur` 常按 USD 理解）——对接旧流量时需补齐字段，或不要期望纯最小集报文通过 schema。

### 示例里易误解的点

| 点 | 说明 |
|---|---|
| `firstPrice()` | 合法；IAB 常见缺省是 `at=2`。线上以请求里的 `at` 为准。 |
| `currency("USD")` | 须落在请求 `cur` 内；多币种时应从 `reqSnap.currencies()` / `shared.cur` 选取。 |
| `pastDeadline()` | **不是** OpenRTB 字段；在 `shared` 起算 `tmax` 的 85%，供调用方提前收束。 |
| 多形态 Imp | `MarkupMask.count()>1` 时 `mtype()==0`，出价前自选并写 `Bid.mtype`。 |

---

## 注意

1. **`noBid` ↔ `addSeatBid`**：后调用清对方状态（清 seats 或清 `nbr`）。
2. **空 `seatbid`**：view 当 wire no-bid；builder 组带 body 的不竞价须显式 `noBid(nbr)`；生产无 body 用 HTTP 204。
3. **多形态 Imp**：`MarkupMask.count() > 1` 时 `mtype()==0`，出价前自行选定形态并写 `Bid.mtype`。
4. **deadline**：在 `shared` 时刻起算 `tmax` 的 85%，不是解码时刻；非规范字段。
5. **Schema**：`buildValidated` / `schema.Request`（及 Java `Schema.request`、Rust `schema::request`）仍可用，属构建/边界工具；热路径结构验证用 LightGate，勿叠跑。

更多 API 表见 [sdk.md](sdk.md)。

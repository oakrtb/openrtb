# Inspect / Pipeline 使用示例

调用方热路径：解码后的 BidRequest / BidResponse → **轻量门禁** → **Shared 视图** → **Imp / Bid 视图** →（可选）**Fit** → 组响应 → ResponseInspect。

不做完整 JSON Schema（权威校验仍用 `validate`）；也不实现拍卖引擎。Fit 不进 LightGate，默认 opt-in。

## 一键入口

| | BidRequest | BidResponse | Fit（可选） |
|---|---|---|---|
| Java | `RequestPipeline.run(req)` | `ResponsePipeline.run(res)` | `com.oakrtb.sdk.fit.Fit` |
| Go | `inspect.RunRequest(req)` | `inspect.RunResponse(res)` | `github.com/oakrtb/openrtb/sdk/go/fit` |
| Rust | `inspect::run_request(&json)` | `inspect::run_response(&json)` | `oakrtb_sdk::fit` |

`MarkupMask` 位掩码：`BANNER|VIDEO|AUDIO|NATIVE`。请求侧表示 Imp 上出现的子对象；响应侧由 `Bid.mtype` 映射。单形态时 `mtype()` → `Bid.mtype`（1–4）。

---

## Fit（RequestInspect → Fit → ResponseInspect）

在组标前检查形态规格是否就绪，组标后对照请求做 bid↔imp 一致性。返回 `FitResult`（`ok()` = 无 ERROR；WARN 可保留仍出价）。**不抛异常**，也不替代 schema。

| API | 时机 | 作用 |
|---|---|---|
| `impReady` / `ImpReadyMtype` / `imp_ready_mtype` | 选定形态后、出价前 | Banner 尺寸、Video/Audio `mimes`、Native `request`；缺时长/protocols 为 WARN |
| `bidFit` / `BidFit` / `bid_fit` | 单条 Bid | `impid`、多形态须 `mtype`、`mtype`∈Imp MarkupMask；floor/battr/badv 等为 WARN |
| `responseFit` / `ResponseFit` / `response_fit` | 整包响应 | 遍历 seatbid；空 seatbid（no-bid）直接 ok；另查 `cur` / deadline |

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
  var fitResult = Fit.bidFit(reqSnap, bid);
  if (!fitResult.ok()) { /* 丢标 */ }
  // fitResult.warnings() 可打点
}
// 组完 res 后：
Fit.responseFit(reqSnap, res);
```

### Go 片段

```go
import "github.com/oakrtb/openrtb/sdk/go/fit"

ready := fit.ImpReadyMtype(&imp, mtype)
fitRes := fit.BidFit(reqSnap, bid)
_ = fit.ResponseFit(reqSnap, res)
_ = ready
_ = fitRes
```

### Rust 片段

```rust
use oakrtb_sdk::fit;

let ready = fit::imp_ready_mtype(&imp, mtype);
let fit = fit::bid_fit(&req_snap, &bid_json);
let _ = fit::response_fit(&req_snap, &res_json);
```

---

## Java（JDK 21）

### 组请求 → 视图 → 组响应 → 再视图

```java
import com.oakrtb.sdk.build.*;
import com.oakrtb.sdk.inspect.*;

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
    .pinShared()
    .viewImps()
    .snapshot();

var rsnap = ResponsePipeline.of(res)
    .lightGate()
    .pinShared()
    .viewBids()
    .snapshot();
```

### MarkupMask

```java
MarkupMask f = RequestInspect.markupMask(imp);
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
	"github.com/oakrtb/openrtb/sdk/go/inspect"
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

	reqSnap, err := inspect.RunRequest(req)
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

	resSnap, err := inspect.RunResponse(res)
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
	_ = resSnap.BidsWith(inspect.MarkupBanner)
}
```

逐步：

```go
snap, err := inspect.OfRequest(req).LightGate().PinShared().ViewImps().Snapshot()
rsnap, err := inspect.OfResponse(res).LightGate().PinShared().ViewBids().Snapshot()
```

---

## Rust

```rust
use oakrtb_sdk::build::{
    BannerImpBuilder, BidBuilder, BidRequestBuilder, BidResponseBuilder, DeviceBuilder,
    SiteBuilder,
};
use oakrtb_sdk::inspect::{self, MarkupMask};

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

    let req_snap = inspect::run_request(&req)?;
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

    let res_snap = inspect::run_response(&res)?;
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
let snap = inspect::RequestPipeline::of(&req)
    .light_gate()
    .pin_shared()
    .view_imps()
    .snapshot()?;

let rsnap = inspect::ResponsePipeline::of(&res)
    .light_gate()
    .pin_shared()
    .view_bids()
    .snapshot()?;
```

Rust 构建器产出 `serde_json::Value`；Go/Java 用 protobuf 模型。

---

## 与 OpenRTB 2.6 接入对照

完整 HTTP 合同见 [transport.md](transport.md)；对象必填见 [spec.md](spec.md) / [objects.md](objects.md)。

### 标准 DSP 主路径（对象层）

```
POST BidRequest
  → 解析 + LightGate / schema
  → Shared（at / cur / tmax / 库存 / 屏蔽）
  → 遍历 Imp（MarkupMask、floor）
  → Fit.impReady（可选）→ 定向 & 估价 → 组 Bid
  → Fit.bidFit / responseFit（可选）
  → 应答（见下表）
```

与上文 `RequestPipeline` →（可选 Fit）→ 组 `BidResponse` 一致。`ResponsePipeline` 用于发出前自检或 Exchange 收包侧，非规范强制步骤。

### HTTP 应答（传输层，SDK 不代发）

| 场景 | HTTP | Body |
|---|---|---|
| 出价 | 200 | `BidResponse` + `seatbid` |
| 不竞价（生产最常见） | **204** | **无 body** |
| 结构化不竞价（联调/可观测） | 200 | `BidResponse` + `id` + `cur` + `nbr`（无 seatbid） |
| 请求非法 / schema 失败 | 400 | `ValidationResult` |

SDK 的 `noBid(nbr)` 对应表中第三行。生产大量「不想买」应优先 **204**，不要用 4xx/5xx 表示不竞价。

### OakRTB profile vs IAB 最小集

示例与 LightGate 按 OakRTB 校验：`BidRequest` 须带 `at`、`cur`；`BidResponse` 须带 `cur`。IAB 2.6 原文里这些多为可选（`at` 常按 2、`cur` 常按 USD 理解）——对接旧流量时需补齐字段，或不要期望纯最小集报文通过 schema。

### 示例里易误解的点

| 点 | 说明 |
|---|---|
| `firstPrice()` | 合法；IAB 常见缺省是 `at=2`。线上以请求里的 `at` 为准。 |
| `currency("USD")` | 须落在请求 `cur` 内；多币种时应从 `reqSnap.currencies()` / `shared.cur` 选取。 |
| `pastDeadline()` | **不是** OpenRTB 字段；在 `pinShared` 起算 `tmax` 的 85%，供调用方提前收束。 |
| 多形态 Imp | `MarkupMask.count()>1` 时 `mtype()==0`，出价前自选并写 `Bid.mtype`。 |

---

## 注意

1. **`noBid` ↔ `addSeatBid`**：后调用清对方状态（清 seats 或清 `nbr`）。
2. **空 `seatbid`**：inspect 当 wire no-bid；builder 组带 body 的不竞价须显式 `noBid(nbr)`；生产无 body 用 HTTP 204。
3. **多形态 Imp**：`MarkupMask.count() > 1` 时 `mtype()==0`，出价前自行选定形态并写 `Bid.mtype`。
4. **deadline**：在 `pinShared` 时刻起算 `tmax` 的 85%，不是解码时刻；非规范字段。
5. **权威校验**：`buildValidated` / `ValidateBidRequest` 仍走 JSON Schema。

更多 API 表见 [sdk.md](sdk.md)。

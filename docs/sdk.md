# SDK 与校验

OakRTB 提供 Go / Java / Rust 的**模型生成**、**请求/响应构建器**与**统一校验**（不包含 HTTP 服务或拍卖引擎）。

## 权威与产物

| 角色 | 路径 |
|---|---|
| 校验权威 | `schema/jsonschema/*.schema.json` |
| 统一结果 | `schema/jsonschema/validation-result.schema.json` |
| Protobuf | `proto/oakrtb/v2/openrtb.proto` |
| Go 模型 / 构建器 | `sdk/go/oakrtb/v2`、`sdk/go/build` |
| Java 模型 / 构建器 | `com.oakrtb.openrtb.v2`、`com.oakrtb.sdk.build` |
| Rust 模型 / 构建器 | `oakrtb_sdk::proto`、`oakrtb_sdk::build` |
| 手写校验层 | `sdk/{go,java,rust}/validate` |

## 构建

```bash
make sync-schemas  # 将 schema 同步进各 SDK 资源目录
make proto-go
make proto-java
make proto-rust
make sdk-test
make jar           # gen/java/dist/oakrtb-sdk-<version>.jar 与 -all.jar
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

### Go

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

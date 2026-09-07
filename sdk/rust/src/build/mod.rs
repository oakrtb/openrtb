//! 流式 Builder：输出 OpenRTB JSON（[`serde_json::Value`]），并可经 JSON Schema 校验。
//!
//! 推荐在发送前调用 `build_validated()`，使 Builder 结构检查与 Schema 一致。

mod request;
mod response;

pub use request::{
    AppBuilder, AudioImpBuilder, BannerImpBuilder, BidRequestBuilder, ContentBuilder,
    DeviceBuilder, DoohBuilder, GeoBuilder, NativeImpBuilder, PublisherBuilder, SiteBuilder,
    VideoImpBuilder,
};
pub use response::{BidBuilder, BidResponseBuilder, ValidatedJson};

#[cfg(test)]
mod build_test;

#[cfg(test)]
mod full_fields_test;

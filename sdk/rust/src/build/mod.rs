//! Fluent builders that emit OpenRTB JSON (`serde_json::Value`) and validate via schema.
//!
//! Prefer `build_validated()` so structural helpers + JSON Schema agree before send.

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

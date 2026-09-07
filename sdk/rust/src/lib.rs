//! OakRTB Rust SDK：基于 prost 的 protobuf 模型、JSON Schema 校验、Builder、Inspect 与 Fit。
//!
//! - [`validate`]：权威 JSON Schema 校验
//! - [`build`]：流式 Builder 生成 OpenRTB JSON
//! - [`inspect`]：轻量 BidRequest/BidResponse 解析与视图（含 [`inspect::MarkupMask`]、[`inspect::Inventory`]）
//! - [`fit`]：请求快照与出价之间的软匹配检查（非 Schema、非 LightGate）
//! - [`proto`]：生成的 protobuf 消息（`oakrtb.v2`）

pub mod build;
pub mod fit;
pub mod inspect;
pub mod validate;

/// 由 build.rs 生成的 protobuf 消息（`oakrtb.v2` 包）。
pub mod proto {
    include!(concat!(env!("OUT_DIR"), "/oakrtb.v2.rs"));
}

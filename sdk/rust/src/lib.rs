//! OakRTB Rust SDK：基于 prost 的 protobuf 模型、JSON Schema 校验、Builder、View 与 Fit。
//!
//! - [`schema`]：JSON Schema 合同校验（非热路径必经）
//! - [`build`]：流式 Builder 生成 OpenRTB JSON
//! - [`view`]：轻量 BidRequest/BidResponse 解析与视图（含 LightGate）
//! - [`fit`]：形态就绪与 bid↔imp 软契合（非 Schema、非 LightGate）
//! - [`proto`]：生成的 protobuf 消息（`oakrtb.v2`）

pub mod build;
pub mod fit;
pub mod view;
pub mod schema;

/// 由 build.rs 生成的 protobuf 消息（`oakrtb.v2` 包）。
pub mod proto {
    include!(concat!(env!("OUT_DIR"), "/oakrtb.v2.rs"));
}

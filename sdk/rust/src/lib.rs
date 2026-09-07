//! OakRTB SDK: protobuf models (via prost), JSON Schema validation, and builders.

pub mod build;
pub mod validate;

/// Generated protobuf messages (`oakrtb.v2`).
pub mod proto {
    include!(concat!(env!("OUT_DIR"), "/oakrtb.v2.rs"));
}

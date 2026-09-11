# oakrtb-sdk (Rust)

OpenRTB-compatible models, builders, hot-path `view` (LightGate), optional `fit`, and off-path `schema` validation for [OakRTB](https://github.com/oakrtb/openrtb).

```toml
[dependencies]
oakrtb-sdk = "0.2.0"
```

Requires **Rust 1.85+** and a `protoc` on `PATH` for the first build (prost).

Schema JSON and `openrtb.proto` are **vendored** under `schemas/` and `proto/` (refreshed by `make sync-schemas` in the monorepo).

See the [repository docs](https://github.com/oakrtb/openrtb/blob/main/docs/sdk.md) and [publishing guide](https://github.com/oakrtb/openrtb/blob/main/docs/publishing.md).

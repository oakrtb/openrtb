use std::env;
use std::fs;
use std::path::{Path, PathBuf};

fn main() {
    let manifest_dir = PathBuf::from(env::var("CARGO_MANIFEST_DIR").unwrap());
    let proto = manifest_dir
        .join("../../proto/oakrtb/v2/openrtb.proto")
        .canonicalize()
        .expect("proto path");
    let proto_root = manifest_dir.join("../../proto").canonicalize().unwrap();

    println!("cargo:rerun-if-changed={}", proto.display());
    prost_build::Config::new()
        .compile_protos(&[proto], &[proto_root])
        .expect("prost compile");

    sync_schemas(&manifest_dir);
}

/// 从仓库根 `schema/jsonschema/` 拷到 `OUT_DIR/schemas/`，供 `include_str!` 嵌入。
fn sync_schemas(manifest_dir: &Path) {
    let src = manifest_dir
        .join("../../schema/jsonschema")
        .canonicalize()
        .expect("schema/jsonschema (run from oakrtb monorepo)");
    let out = PathBuf::from(env::var("OUT_DIR").unwrap()).join("schemas");
    fs::create_dir_all(&out).expect("create OUT_DIR/schemas");

    for name in [
        "openrtb.schema.json",
        "bid-request.schema.json",
        "bid-response.schema.json",
        "native.schema.json",
        "validation-result.schema.json",
    ] {
        let from = src.join(name);
        println!("cargo:rerun-if-changed={}", from.display());
        fs::copy(&from, out.join(name)).unwrap_or_else(|e| {
            panic!("copy {name} from {}: {e}", from.display());
        });
    }
}

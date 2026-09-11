use std::env;
use std::fs;
use std::path::{Path, PathBuf};

fn main() {
    let manifest_dir = PathBuf::from(env::var("CARGO_MANIFEST_DIR").unwrap());

    // Prefer vendored copies (crates.io); fall back to monorepo roots.
    let proto = first_existing(&[
        manifest_dir.join("proto/oakrtb/v2/openrtb.proto"),
        manifest_dir.join("../../proto/oakrtb/v2/openrtb.proto"),
    ])
    .expect("openrtb.proto (run make sync-schemas or build inside oakrtb monorepo)");
    let proto_root = proto
        .parent()
        .and_then(|p| p.parent())
        .and_then(|p| p.parent())
        .expect("proto root")
        .to_path_buf();

    println!("cargo:rerun-if-changed={}", proto.display());
    prost_build::Config::new()
        .compile_protos(&[proto.as_path()], &[proto_root.as_path()])
        .expect("prost compile");

    sync_schemas(&manifest_dir);
}

fn first_existing(candidates: &[PathBuf]) -> Option<PathBuf> {
    candidates.iter().find_map(|p| p.canonicalize().ok())
}

/// Copy JSON schemas into `OUT_DIR/schemas/` for `include_str!`.
fn sync_schemas(manifest_dir: &Path) {
    let src = first_existing(&[
        manifest_dir.join("schemas"),
        manifest_dir.join("../../schema/jsonschema"),
    ])
    .expect("schemas/ (run make sync-schemas or build inside oakrtb monorepo)");
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

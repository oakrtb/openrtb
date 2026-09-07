use std::env;
use std::path::PathBuf;

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
}

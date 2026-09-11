use crate::schema::{request, response};
use serde_json::Value;
use std::fs;
use std::path::PathBuf;

fn repo_testdata_full() -> PathBuf {
    // sdk/rust/src/build → repo testdata/full
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../../testdata/full")
}

fn load(name: &str) -> Vec<u8> {
    let path = repo_testdata_full().join(name);
    fs::read(&path).unwrap_or_else(|e| panic!("read {}: {e}", path.display()))
}

/// Every property in the full fixtures must be present (non-null) at the top
/// levels we care about; schema validation proves the payload is legal.
#[test]
fn full_bid_request_app_sets_all_major_objects() {
    let raw = load("bid-request/app.json");
    let result = request(&raw);
    assert!(result.ok, "{:?}", result.errors);

    let v: Value = serde_json::from_slice(&raw).unwrap();
    let obj = v.as_object().unwrap();
    for key in [
        "id", "imp", "app", "device", "user", "test", "at", "tmax", "wseat", "bseat",
        "allimps", "cur", "wlang", "wlangb", "acat", "bcat", "cattax", "badv", "bapp",
        "source", "regs",
    ] {
        assert!(obj.contains_key(key), "missing BidRequest.{key}");
    }
    assert!(!obj.contains_key("site"));
    assert!(!obj.contains_key("dooh"));

    let imp = &obj["imp"].as_array().unwrap()[0];
    for key in [
        "id", "metric", "banner", "video", "audio", "native", "pmp", "displaymanager",
        "displaymanagerver", "instl", "tagid", "bidfloor", "bidfloorcur", "clickbrowser",
        "secure", "iframebuster", "rwdd", "ssai", "exp", "qty", "dt", "refresh",
    ] {
        assert!(imp.get(key).is_some(), "missing Imp.{key}");
    }
    let video = imp["video"].as_object().unwrap();
    for key in [
        "mimes", "minduration", "maxduration", "startdelay", "maxseq", "poddur", "protocols",
        "w", "h", "podid", "podseq", "rqddurs", "plcmt", "linearity", "skip", "skipmin",
        "skipafter", "slotinpod", "mincpmpersec", "battr", "maxextended", "minbitrate",
        "maxbitrate", "boxingallowed", "playbackmethod", "playbackend", "delivery", "pos",
        "companionad", "api", "companiontype", "placement", "poddedupe", "durfloors",
    ] {
        assert!(video.contains_key(key), "missing Video.{key}");
    }
}

#[test]
fn full_bid_request_site_and_dooh_validate() {
    for name in ["bid-request/site.json", "bid-request/dooh.json"] {
        let raw = load(name);
        let result = request(&raw);
        assert!(result.ok, "{name}: {:?}", result.errors);
    }
}

#[test]
fn full_bid_response_sets_all_fields() {
    let raw = load("bid-response/full.json");
    let result = response(&raw);
    assert!(result.ok, "{:?}", result.errors);

    let v: Value = serde_json::from_slice(&raw).unwrap();
    let obj = v.as_object().unwrap();
    for key in ["id", "seatbid", "bidid", "cur", "customdata", "nbr"] {
        assert!(obj.contains_key(key), "missing BidResponse.{key}");
    }
    let bid = &obj["seatbid"].as_array().unwrap()[0].as_object().unwrap()["bid"]
        .as_array()
        .unwrap()[0];
    let bid = bid.as_object().unwrap();
    for key in [
        "id", "impid", "price", "nurl", "burl", "lurl", "adm", "adid", "adomain", "bundle",
        "iurl", "cid", "crid", "tactic", "cattax", "cat", "attr", "apis", "protocol",
        "qagmediarating", "language", "langb", "dealid", "w", "h", "wratio", "hratio", "exp",
        "dur", "mtype", "slotinpod",
    ] {
        assert!(bid.contains_key(key), "missing Bid.{key}");
    }
}

/// Count leaf keys under a JSON value (objects/arrays walked).
fn count_leaves(v: &Value) -> usize {
    match v {
        Value::Object(m) => m.values().map(count_leaves).sum(),
        Value::Array(a) => a.iter().map(count_leaves).sum(),
        _ => 1,
    }
}

#[test]
fn full_fixtures_are_dense() {
    let app: Value = serde_json::from_slice(&load("bid-request/app.json")).unwrap();
    let leaves = count_leaves(&app);
    // Fully populated tree should be large; guards against empty stubs.
    assert!(leaves > 150, "expected dense fixture, got {leaves} leaves");
}

//! Unified ValidationResult and BidRequest / BidResponse validators.

use jsonschema::Validator as JsValidator;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::sync::OnceLock;

const OPENRTB: &str = include_str!("../schemas/openrtb.schema.json");
const BID_REQUEST: &str = include_str!("../schemas/bid-request.schema.json");
const BID_RESPONSE: &str = include_str!("../schemas/bid-response.schema.json");
const NATIVE: &str = include_str!("../schemas/native.schema.json");

/// Unified validation outcome (HTTP 400 body when `ok` is false).
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct ValidationResult {
    pub ok: bool,
    pub errors: Vec<ValidationError>,
}

/// One failing validation check.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct ValidationError {
    pub code: String,
    pub path: String,
    pub message: String,
}

impl ValidationResult {
    pub fn ok_result() -> Self {
        Self {
            ok: true,
            errors: vec![],
        }
    }

    pub fn fail(errors: Vec<ValidationError>) -> Self {
        Self { ok: false, errors }
    }

    pub fn to_json(&self) -> String {
        serde_json::to_string(self).expect("ValidationResult serializes")
    }

    pub fn to_json_bytes(&self) -> Vec<u8> {
        serde_json::to_vec(self).expect("ValidationResult serializes")
    }
}

fn request_validator() -> &'static JsValidator {
    static V: OnceLock<JsValidator> = OnceLock::new();
    V.get_or_init(|| compile(BID_REQUEST, "bid-request"))
}

fn response_validator() -> &'static JsValidator {
    static V: OnceLock<JsValidator> = OnceLock::new();
    V.get_or_init(|| compile(BID_RESPONSE, "bid-response"))
}

fn native_validator() -> &'static JsValidator {
    static V: OnceLock<JsValidator> = OnceLock::new();
    V.get_or_init(|| compile(NATIVE, "native"))
}

fn compile(schema_str: &str, label: &str) -> JsValidator {
    let schema: Value = serde_json::from_str(schema_str).unwrap_or_else(|e| {
        panic!("invalid {label} schema: {e}");
    });
    // Register sibling schemas for $ref resolution via retrieving
    let openrtb: Value = serde_json::from_str(OPENRTB).expect("openrtb schema");
    let resource = jsonschema::Resource::from_contents(openrtb).expect("openrtb resource");

    jsonschema::options()
        .with_resource(
            "https://github.com/oakrtb/openrtb/schema/jsonschema/openrtb.schema.json",
            resource.clone(),
        )
        .with_resource("openrtb.schema.json", resource)
        .build(&schema)
        .unwrap_or_else(|e| panic!("compile {label}: {e}"))
}

/// Validate BidRequest JSON bytes.
pub fn validate_bid_request(data: &[u8]) -> ValidationResult {
    validate(data, request_validator(), true)
}

/// Validate BidResponse JSON bytes.
pub fn validate_bid_response(data: &[u8]) -> ValidationResult {
    validate(data, response_validator(), false)
}

fn validate(data: &[u8], validator: &JsValidator, check_native: bool) -> ValidationResult {
    let doc: Value = match serde_json::from_slice(data) {
        Ok(v) => v,
        Err(e) => {
            return ValidationResult::fail(vec![ValidationError {
                code: "parse".into(),
                path: String::new(),
                message: e.to_string(),
            }]);
        }
    };

    let mut errors: Vec<ValidationError> = validator
        .iter_errors(&doc)
        .map(|e| ValidationError {
            code: classify(&e.to_string()),
            path: pointer_path(e.instance_path.to_string()),
            message: e.to_string(),
        })
        .collect();

    if check_native {
        errors.extend(validate_native_embedded(&doc));
    }

    if errors.is_empty() {
        ValidationResult::ok_result()
    } else {
        ValidationResult::fail(errors)
    }
}

fn pointer_path(path: String) -> String {
    if path.is_empty() || path == "/" {
        return path;
    }
    if path.starts_with('/') {
        path
    } else {
        format!("/{path}")
    }
}

fn classify(msg: &str) -> String {
    let lower = msg.to_ascii_lowercase();
    if lower.contains("required") {
        "required".into()
    } else if lower.contains("type") {
        "type".into()
    } else if lower.contains("format") || lower.contains("pattern") {
        "format".into()
    } else {
        "constraint".into()
    }
}

fn validate_native_embedded(doc: &Value) -> Vec<ValidationError> {
    let mut errors = Vec::new();
    let Some(imps) = doc.get("imp").and_then(|v| v.as_array()) else {
        return errors;
    };
    for (i, imp) in imps.iter().enumerate() {
        let Some(req) = imp
            .get("native")
            .and_then(|n| n.get("request"))
            .and_then(|r| r.as_str())
        else {
            continue;
        };
        let inner: Value = match serde_json::from_str(req) {
            Ok(v) => v,
            Err(e) => {
                errors.push(ValidationError {
                    code: "native".into(),
                    path: format!("/imp/{i}/native/request"),
                    message: format!("native.request is not JSON: {e}"),
                });
                continue;
            }
        };
        for e in native_validator().iter_errors(&inner) {
            errors.push(ValidationError {
                code: "native".into(),
                path: format!(
                    "/imp/{i}/native/request{}",
                    pointer_path(e.instance_path.to_string())
                ),
                message: format!("native.request: {e}"),
            });
        }
    }
    errors
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs;
    use std::path::PathBuf;
    use walkdir::WalkDir;

    fn repo_root() -> PathBuf {
        PathBuf::from(env!("CARGO_MANIFEST_DIR"))
            .join("../..")
            .canonicalize()
            .unwrap()
    }

    #[test]
    fn examples_pass() {
        let root = repo_root().join("examples");
        for entry in WalkDir::new(&root).into_iter().filter_map(|e| e.ok()) {
            let path = entry.path();
            if path.extension().and_then(|s| s.to_str()) != Some("json") {
                continue;
            }
            let data = fs::read(path).unwrap();
            let parent = path.parent().unwrap().file_name().unwrap().to_string_lossy();
            let result = if parent == "bid-request" {
                validate_bid_request(&data)
            } else {
                validate_bid_response(&data)
            };
            assert!(
                result.ok,
                "{} => {:?}",
                path.display(),
                result.errors
            );
        }
    }

    #[test]
    fn invalid_rejected() {
        let root = repo_root().join("testdata/invalid");
        for entry in WalkDir::new(&root).into_iter().filter_map(|e| e.ok()) {
            let path = entry.path();
            if path.extension().and_then(|s| s.to_str()) != Some("json") {
                continue;
            }
            let data = fs::read(path).unwrap();
            let parent = path.parent().unwrap().file_name().unwrap().to_string_lossy();
            let result = if parent == "bid-request" {
                validate_bid_request(&data)
            } else {
                validate_bid_response(&data)
            };
            assert!(!result.ok, "{} should fail", path.display());
            assert!(!result.errors.is_empty());
        }
    }

    #[test]
    fn parse_error() {
        let result = validate_bid_request(b"{");
        assert!(!result.ok);
        assert_eq!(result.errors[0].code, "parse");
    }
}

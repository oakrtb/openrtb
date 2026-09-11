//! BidRequest / BidResponse JSON Schema 校验（[`Report`] / [`Issue`]）。

use jsonschema::Validator as JsValidator;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::sync::OnceLock;

// 由 build.rs 从仓库根 schema/jsonschema/ 拷入 OUT_DIR。
const OPENRTB: &str = include_str!(concat!(env!("OUT_DIR"), "/schemas/openrtb.schema.json"));
const BID_REQUEST: &str =
    include_str!(concat!(env!("OUT_DIR"), "/schemas/bid-request.schema.json"));
const BID_RESPONSE: &str =
    include_str!(concat!(env!("OUT_DIR"), "/schemas/bid-response.schema.json"));
const NATIVE: &str = include_str!(concat!(env!("OUT_DIR"), "/schemas/native.schema.json"));

/// Schema 校验结果；`ok == false` 时可作为 HTTP 400 响应体。
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct Report {
    /// 是否通过全部校验。
    pub ok: bool,
    /// 失败项列表；通过时为空。
    pub errors: Vec<Issue>,
}

/// 单条 Schema 校验失败项。
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct Issue {
    /// 错误分类（如 `required`、`type`、`parse`、`native`）。
    pub code: String,
    /// JSON Pointer 路径（如 `/imp/0/id`）。
    pub path: String,
    /// 人类可读说明。
    pub message: String,
}

impl Report {
    /// 构造通过结果。
    pub fn ok() -> Self {
        Self {
            ok: true,
            errors: vec![],
        }
    }

    /// 构造失败结果。
    pub fn fail(errors: Vec<Issue>) -> Self {
        Self { ok: false, errors }
    }

    /// 序列化为 JSON 字符串。
    pub fn to_json(&self) -> String {
        serde_json::to_string(self).expect("Report serializes")
    }

    /// 序列化为 JSON 字节。
    pub fn to_json_bytes(&self) -> Vec<u8> {
        serde_json::to_vec(self).expect("Report serializes")
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
    let openrtb: Value = serde_json::from_str(OPENRTB).expect("openrtb schema");
    let registry = jsonschema::Registry::new()
        .add(
            "https://github.com/oakrtb/openrtb/schema/jsonschema/openrtb.schema.json",
            openrtb.clone(),
        )
        .unwrap_or_else(|e| panic!("register openrtb uri: {e}"))
        .add("openrtb.schema.json", openrtb)
        .unwrap_or_else(|e| panic!("register openrtb relative: {e}"))
        .prepare()
        .unwrap_or_else(|e| panic!("prepare registry: {e}"));

    jsonschema::options()
        .with_registry(&registry)
        .build(&schema)
        .unwrap_or_else(|e| panic!("compile {label}: {e}"))
}

/// 校验 BidRequest JSON 字节。
pub fn request(data: &[u8]) -> Report {
    check(data, request_validator(), true)
}

/// 校验 BidResponse JSON 字节。
pub fn response(data: &[u8]) -> Report {
    check(data, response_validator(), false)
}

fn check(data: &[u8], validator: &JsValidator, check_native: bool) -> Report {
    let doc: Value = match serde_json::from_slice(data) {
        Ok(v) => v,
        Err(e) => {
            return Report::fail(vec![Issue {
                code: "parse".into(),
                path: String::new(),
                message: e.to_string(),
            }]);
        }
    };

    let mut errors: Vec<Issue> = validator
        .iter_errors(&doc)
        .map(|e| Issue {
            code: classify(&e.to_string()),
            path: pointer_path(e.instance_path().to_string()),
            message: e.to_string(),
        })
        .collect();

    if check_native {
        errors.extend(native_embedded(&doc));
    }

    if errors.is_empty() {
        Report::ok()
    } else {
        Report::fail(errors)
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

fn native_embedded(doc: &Value) -> Vec<Issue> {
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
                errors.push(Issue {
                    code: "native".into(),
                    path: format!("/imp/{i}/native/request"),
                    message: format!("native.request is not JSON: {e}"),
                });
                continue;
            }
        };
        for e in native_validator().iter_errors(&inner) {
            errors.push(Issue {
                code: "native".into(),
                path: format!(
                    "/imp/{i}/native/request{}",
                    pointer_path(e.instance_path().to_string())
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
                request(&data)
            } else {
                response(&data)
            };
            assert!(result.ok, "{} => {:?}", path.display(), result.errors);
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
                request(&data)
            } else {
                response(&data)
            };
            assert!(!result.ok, "{} should fail", path.display());
            assert!(!result.errors.is_empty());
        }
    }

    #[test]
    fn parse_error() {
        let result = request(b"{");
        assert!(!result.ok);
        assert_eq!(result.errors[0].code, "parse");
    }
}

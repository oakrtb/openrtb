//! 可选的 Bid↔Request 软匹配检查（基于 [`RequestSnapshot`] 与响应 JSON）。
//!
//! 非 JSON Schema，也非 LightGate；返回软性的 [`FitResult`] 发现项。
//! [`FitResult::ok`] 仅表示无 ERROR；WARN（低价/屏蔽/超时等）仍可能 ok。

use crate::view::{markup_from_mtype, MarkupMask, ImpView, RequestSnapshot};
use serde_json::Value;

/// 发现项严重级别。
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum Severity {
    /// 错误：[`FitResult::ok`] 为 false。
    Error,
    /// 警告：不影响 `ok()`，但应关注。
    Warn,
}

impl Severity {
    /// 返回 `"ERROR"` 或 `"WARN"` 字符串。
    pub fn as_str(self) -> &'static str {
        match self {
            Severity::Error => "ERROR",
            Severity::Warn => "WARN",
        }
    }
}

/// Imp 上不存在所选展示格式。
pub const CODE_FORMAT_NOT_ON_IMP: &str = "FORMAT_NOT_ON_IMP";
/// [`MarkupMask`] 必须恰好选中一种格式。
pub const CODE_MULTI_NEEDS_CHOICE: &str = "MULTI_NEEDS_CHOICE";
/// Banner 缺少 w/h 或 format[]。
pub const CODE_BANNER_SIZE_MISSING: &str = "BANNER_SIZE_MISSING";
/// Video 缺少 mimes。
pub const CODE_VIDEO_MIMES_MISSING: &str = "VIDEO_MIMES_MISSING";
/// Audio 缺少 mimes。
pub const CODE_AUDIO_MIMES_MISSING: &str = "AUDIO_MIMES_MISSING";
/// Native 缺少 request 字符串。
pub const CODE_NATIVE_REQUEST_MISSING: &str = "NATIVE_REQUEST_MISSING";
/// Video minduration/maxduration 均未设置（警告）。
pub const CODE_VIDEO_DURATION_UNSET: &str = "VIDEO_DURATION_UNSET";
/// Video protocols 未设置（警告）。
pub const CODE_VIDEO_PROTOCOLS_UNSET: &str = "VIDEO_PROTOCOLS_UNSET";
/// 响应/载荷结构无法解析（如非 JSON object）。
pub const CODE_MALFORMED: &str = "MALFORMED";
/// Bid.impid 在请求中不存在。
pub const CODE_IMP_NOT_FOUND: &str = "IMP_NOT_FOUND";
/// 多格式 Imp 要求 Bid.mtype。
pub const CODE_MTYPE_REQUIRED: &str = "MTYPE_REQUIRED";
/// Bid.mtype 与 Imp 格式不匹配。
pub const CODE_MTYPE_MISMATCH: &str = "MTYPE_MISMATCH";
/// mtype 不在 1–4 且非 vendor 范围。
pub const CODE_MTYPE_UNKNOWN: &str = "MTYPE_UNKNOWN";
/// 厂商扩展 mtype ≥ 500（警告）。
pub const CODE_MTYPE_VENDOR: &str = "MTYPE_VENDOR";
/// 出价低于 imp.bidfloor（警告）。
pub const CODE_PRICE_BELOW_FLOOR: &str = "PRICE_BELOW_FLOOR";
/// 响应货币与 imp.bidfloorcur 不一致，跳过底价比较（警告）。
pub const CODE_FLOOR_CUR_DIFF: &str = "FLOOR_CUR_DIFF";
/// bid.attr 与格式 battr 相交（警告）。
pub const CODE_ATTR_BLOCKED: &str = "ATTR_BLOCKED";
/// adomain 命中 BidRequest.badv（警告）。
pub const CODE_ADOMAIN_BLOCKED: &str = "ADOMAIN_BLOCKED";
/// bundle 命中 BidRequest.bapp（警告）。
pub const CODE_BUNDLE_BLOCKED: &str = "BUNDLE_BLOCKED";
/// cat 命中 BidRequest.bcat（警告）。
pub const CODE_CAT_BLOCKED: &str = "CAT_BLOCKED";
/// 响应货币不在 BidRequest.cur（警告）。
pub const CODE_CUR_NOT_ALLOWED: &str = "CUR_NOT_ALLOWED";
/// 已超过请求 tmax 的 85% 截止时间（警告）。
pub const CODE_PAST_DEADLINE: &str = "PAST_DEADLINE";

/// 单条 Fit 发现。
#[derive(Clone, Debug, Eq, PartialEq)]
pub struct FitIssue {
    /// 稳定错误码（见 `CODE_*` 常量）。
    pub code: String,
    /// 严重级别。
    pub severity: Severity,
    /// JSON 风格路径（如 `seatbid[0].bid[1].price`）。
    pub path: String,
    /// 人类可读说明。
    pub message: String,
}

impl FitIssue {
    fn new(code: &str, severity: Severity, path: impl Into<String>, message: impl Into<String>) -> Self {
        Self {
            code: code.to_string(),
            severity,
            path: path.into(),
            message: message.into(),
        }
    }
}

/// Fit 检查汇总结果。
#[derive(Clone, Debug, Default)]
pub struct FitResult {
    /// 全部发现项。
    pub issues: Vec<FitIssue>,
}

impl FitResult {
    /// 无 Error 级别项时为 true。
    pub fn ok(&self) -> bool {
        !self.issues.iter().any(|i| i.severity == Severity::Error)
    }

    /// 是否包含指定 code 的发现项。
    pub fn has(&self, code: &str) -> bool {
        self.issues.iter().any(|i| i.code == code)
    }

    /// ERROR 级别发现项。
    pub fn errors(&self) -> impl Iterator<Item = &FitIssue> {
        self.issues.iter().filter(|i| i.severity == Severity::Error)
    }

    /// WARN 级别发现项。
    pub fn warnings(&self) -> impl Iterator<Item = &FitIssue> {
        self.issues.iter().filter(|i| i.severity == Severity::Warn)
    }
}

/// 按 OpenRTB `Bid.mtype`（1–4）检查 Imp 是否具备所选格式及 Builder 级规格。
pub fn imp_ready_mtype(imp: &ImpView<'_>, mtype: i64) -> FitResult {
    let path = format!("imp[{}]", imp.id);
    if !(1..=4).contains(&mtype) {
        return FitResult {
            issues: vec![FitIssue::new(
                CODE_MTYPE_UNKNOWN,
                Severity::Error,
                format!("{path}.mtype"),
                "mtype must be 1–4 for imp_ready",
            )],
        };
    }
    imp_ready_chosen(imp, markup_from_mtype(mtype), &path)
}

/// 与 [`imp_ready_mtype`] 相同，但使用单比特 [`MarkupMask`] 指定格式。
///
/// **注意**：[`MarkupMask`] 表示 Imp 上的展示类型（banner/video/audio/native），
/// 与 `Banner.format[]`（尺寸列表）不是同一概念。
pub fn imp_ready_markup(imp: &ImpView<'_>, chosen: MarkupMask) -> FitResult {
    let path = format!("imp[{}]", imp.id);
    if chosen.count() != 1 {
        return FitResult {
            issues: vec![FitIssue::new(
                CODE_MULTI_NEEDS_CHOICE,
                Severity::Error,
                path,
                "chosen MarkupMask must be exactly one bit",
            )],
        };
    }
    imp_ready_chosen(imp, chosen, &path)
}

fn imp_ready_chosen(imp: &ImpView<'_>, chosen: MarkupMask, path: &str) -> FitResult {
    let mut issues = Vec::new();
    if !imp.markup.has(chosen) {
        return FitResult {
            issues: vec![FitIssue::new(
                CODE_FORMAT_NOT_ON_IMP,
                Severity::Error,
                path,
                "chosen format is not present on Imp",
            )],
        };
    }
    if chosen.has_banner() {
        let size_ok = imp.banner.map(banner_size_ok).unwrap_or(false);
        if !size_ok {
            issues.push(FitIssue::new(
                CODE_BANNER_SIZE_MISSING,
                Severity::Error,
                format!("{path}.banner"),
                "banner needs w/h or format[]",
            ));
        }
    }
    if chosen.has_video() {
        let v = imp.video;
        let mimes = v.and_then(|o| o.get("mimes")).and_then(|v| v.as_array());
        if mimes.map(|a| a.is_empty()).unwrap_or(true) {
            issues.push(FitIssue::new(
                CODE_VIDEO_MIMES_MISSING,
                Severity::Error,
                format!("{path}.video.mimes"),
                "video.mimes is required",
            ));
        } else if let Some(v) = v {
            let mind = v.get("minduration").and_then(|x| x.as_i64()).unwrap_or(0);
            let maxd = v.get("maxduration").and_then(|x| x.as_i64()).unwrap_or(0);
            if mind == 0 && maxd == 0 {
                issues.push(FitIssue::new(
                    CODE_VIDEO_DURATION_UNSET,
                    Severity::Warn,
                    format!("{path}.video"),
                    "video minduration/maxduration unset",
                ));
            }
            let protocols = v.get("protocols").and_then(|x| x.as_array());
            if protocols.map(|a| a.is_empty()).unwrap_or(true) {
                issues.push(FitIssue::new(
                    CODE_VIDEO_PROTOCOLS_UNSET,
                    Severity::Warn,
                    format!("{path}.video.protocols"),
                    "video.protocols unset",
                ));
            }
        }
    }
    if chosen.has_audio() {
        let mimes = imp
            .audio
            .and_then(|o| o.get("mimes"))
            .and_then(|v| v.as_array());
        if mimes.map(|a| a.is_empty()).unwrap_or(true) {
            issues.push(FitIssue::new(
                CODE_AUDIO_MIMES_MISSING,
                Severity::Error,
                format!("{path}.audio.mimes"),
                "audio.mimes is required",
            ));
        }
    }
    if chosen.has_native() {
        let req = imp
            .native
            .and_then(|o| o.get("request"))
            .and_then(|v| v.as_str())
            .unwrap_or("");
        if req.trim().is_empty() {
            issues.push(FitIssue::new(
                CODE_NATIVE_REQUEST_MISSING,
                Severity::Error,
                format!("{path}.native.request"),
                "native.request is required",
            ));
        }
    }
    FitResult { issues }
}

fn banner_size_ok(b: &Value) -> bool {
    let w = b.get("w").and_then(|v| v.as_i64()).unwrap_or(0);
    let h = b.get("h").and_then(|v| v.as_i64()).unwrap_or(0);
    let formats = b.get("format").and_then(|v| v.as_array());
    (w > 0 && h > 0) || formats.map(|a| !a.is_empty()).unwrap_or(false)
}

/// 将单个 Bid JSON 对象与请求快照做 Fit 检查。
/// 无响应货币时跳过底价比较；完整检查请用 [`response`]。
pub fn bid(req: &RequestSnapshot<'_>, bid: &Value) -> FitResult {
    if !bid.is_object() {
        return FitResult {
            issues: vec![FitIssue::new(
                CODE_MALFORMED,
                Severity::Error,
                "bid",
                "bid must be a JSON object",
            )],
        };
    }
    bid_at(req, bid, None, "bid")
}

/// 对整个 BidResponse 做 Fit 检查。空 seatbid 视为通过（可附带 PAST_DEADLINE 警告）。
pub fn response(req: &RequestSnapshot<'_>, res: &Value) -> FitResult {
    let mut issues = Vec::new();
    if req.past_deadline() {
        issues.push(FitIssue::new(
            CODE_PAST_DEADLINE,
            Severity::Warn,
            "tmax",
            "past request deadline (85% of tmax)",
        ));
    }
    let Some(obj) = res.as_object() else {
        issues.push(FitIssue::new(
            CODE_MALFORMED,
            Severity::Error,
            "",
            "BidResponse must be a JSON object",
        ));
        return FitResult { issues };
    };
    let seatbid = match obj.get("seatbid") {
        None => return FitResult { issues },
        Some(v) if v.is_null() => return FitResult { issues },
        Some(v) => match v.as_array() {
            Some(a) => a,
            None => {
                issues.push(FitIssue::new(
                    CODE_MALFORMED,
                    Severity::Error,
                    "seatbid",
                    "BidResponse.seatbid must be an array",
                ));
                return FitResult { issues };
            }
        },
    };
    if seatbid.is_empty() {
        return FitResult { issues };
    }
    // Same blank rule as LightGate / check_floor: trim; blank skips CUR + floor.
    // Non-string cur is structural (LightGate would reject); Fit emits MALFORMED.
    let res_cur = match obj.get("cur") {
        None | Some(serde_json::Value::Null) => None,
        Some(v) => match v.as_str() {
            Some(s) => {
                let s = s.trim();
                if s.is_empty() {
                    None
                } else {
                    Some(s)
                }
            }
            None => {
                issues.push(FitIssue::new(
                    CODE_MALFORMED,
                    Severity::Error,
                    "BidResponse.cur",
                    "BidResponse.cur must be a string",
                ));
                None
            }
        },
    };
    if let Some(rc) = res_cur {
        let allowed = req
            .shared
            .cur
            .iter()
            .filter_map(|v| v.as_str())
            .map(str::trim)
            .filter(|c| !c.is_empty())
            .any(|c| c.eq_ignore_ascii_case(rc));
        if !allowed {
            issues.push(FitIssue::new(
                CODE_CUR_NOT_ALLOWED,
                Severity::Warn,
                "BidResponse.cur",
                "response currency not in BidRequest.cur",
            ));
        }
    }
    for (i, sb) in seatbid.iter().enumerate() {
        if !sb.is_object() {
            issues.push(FitIssue::new(
                CODE_MALFORMED,
                Severity::Error,
                format!("seatbid[{i}]"),
                "seatbid entry must be an object",
            ));
            continue;
        }
        let Some(bids) = sb.get("bid").and_then(|v| v.as_array()) else {
            issues.push(FitIssue::new(
                CODE_MALFORMED,
                Severity::Error,
                format!("seatbid[{i}].bid"),
                "seatbid.bid must be a non-empty array",
            ));
            continue;
        };
        if bids.is_empty() {
            issues.push(FitIssue::new(
                CODE_MALFORMED,
                Severity::Error,
                format!("seatbid[{i}].bid"),
                "seatbid.bid must be a non-empty array",
            ));
            continue;
        }
        for (j, bid) in bids.iter().enumerate() {
            if !bid.is_object() {
                issues.push(FitIssue::new(
                    CODE_MALFORMED,
                    Severity::Error,
                    format!("seatbid[{i}].bid[{j}]"),
                    "bid must be an object",
                ));
                continue;
            }
            let path = format!("seatbid[{i}].bid[{j}]");
            let one = bid_at(req, bid, res_cur, &path);
            issues.extend(one.issues);
        }
    }
    FitResult { issues }
}

fn bid_at(
    req: &RequestSnapshot<'_>,
    bid: &Value,
    response_cur: Option<&str>,
    path: &str,
) -> FitResult {
    let mut issues = Vec::new();
    let impid = bid.get("impid").and_then(|v| v.as_str()).unwrap_or("");
    let Some(imp) = req.find_imp(impid) else {
        return FitResult {
            issues: vec![FitIssue::new(
                CODE_IMP_NOT_FOUND,
                Severity::Error,
                format!("{path}.impid"),
                "impid not found in request",
            )],
        };
    };
    let mtype = bid.get("mtype").and_then(|v| v.as_i64()).unwrap_or(0);
    let markup = imp.markup;

    if mtype >= 500 {
        issues.push(FitIssue::new(
            CODE_MTYPE_VENDOR,
            Severity::Warn,
            format!("{path}.mtype"),
            "vendor mtype >=500",
        ));
    } else if mtype != 0 && !(1..=4).contains(&mtype) {
        issues.push(FitIssue::new(
            CODE_MTYPE_UNKNOWN,
            Severity::Error,
            format!("{path}.mtype"),
            "mtype must be 0–4 or >=500",
        ));
    } else if mtype == 0 && markup.count() > 1 {
        issues.push(FitIssue::new(
            CODE_MTYPE_REQUIRED,
            Severity::Error,
            format!("{path}.mtype"),
            "multi-format Imp requires Bid.mtype",
        ));
    } else if (1..=4).contains(&mtype) {
        let chosen = markup_from_mtype(mtype);
        if !markup.has(chosen) {
            issues.push(FitIssue::new(
                CODE_MTYPE_MISMATCH,
                Severity::Error,
                format!("{path}.mtype"),
                "mtype does not match Imp formats",
            ));
        }
    }

    let eff = if (1..=4).contains(&mtype) {
        markup_from_mtype(mtype)
    } else if markup.count() == 1 {
        markup
    } else {
        MarkupMask::NONE
    };

    issues.extend(check_floor(imp, bid, response_cur, path));
    issues.extend(check_attr(imp, bid, eff, path));
    issues.extend(check_blocks(req, bid, path));
    FitResult { issues }
}

fn check_floor(
    imp: &ImpView<'_>,
    bid: &Value,
    response_cur: Option<&str>,
    path: &str,
) -> Vec<FitIssue> {
    let floor = imp.bidfloor;
    if floor <= 0.0 {
        return vec![];
    }
    // Require both response cur and bidfloorcur before numeric compare (`bid` has no response cur).
    let resp_cur = response_cur.map(str::trim).unwrap_or("");
    let floor_cur = imp.bidfloorcur.map(str::trim).unwrap_or("");
    if resp_cur.is_empty() || floor_cur.is_empty() {
        return vec![];
    }
    let price = bid.get("price").and_then(|v| v.as_f64()).unwrap_or(0.0);
    if !resp_cur.eq_ignore_ascii_case(floor_cur) {
        return vec![FitIssue::new(
            CODE_FLOOR_CUR_DIFF,
            Severity::Warn,
            format!("{path}.price"),
            "bid currency differs from imp.bidfloorcur; skip floor compare",
        )];
    }
    if price < floor {
        return vec![FitIssue::new(
            CODE_PRICE_BELOW_FLOOR,
            Severity::Warn,
            format!("{path}.price"),
            "price below imp.bidfloor",
        )];
    }
    vec![]
}

fn check_attr(imp: &ImpView<'_>, bid: &Value, eff: MarkupMask, path: &str) -> Vec<FitIssue> {
    let attrs = bid.get("attr").and_then(|v| v.as_array());
    if attrs.map(|a| a.is_empty()).unwrap_or(true) || eff == MarkupMask::NONE {
        return vec![];
    }
    let battr = battr_for(imp, eff);
    if battr.is_empty() {
        return vec![];
    }
    for a in attrs.unwrap() {
        let Some(n) = a.as_i64() else { continue };
        if battr.contains(&n) {
            return vec![FitIssue::new(
                CODE_ATTR_BLOCKED,
                Severity::Warn,
                format!("{path}.attr"),
                "bid.attr intersects format battr",
            )];
        }
    }
    vec![]
}

fn battr_for(imp: &ImpView<'_>, eff: MarkupMask) -> Vec<i64> {
    let src = if eff.has_banner() {
        imp.banner
    } else if eff.has_video() {
        imp.video
    } else if eff.has_audio() {
        imp.audio
    } else if eff.has_native() {
        imp.native
    } else {
        None
    };
    src.and_then(|o| o.get("battr"))
        .and_then(|v| v.as_array())
        .map(|a| a.iter().filter_map(|x| x.as_i64()).collect())
        .unwrap_or_default()
}

fn check_blocks(req: &RequestSnapshot<'_>, bid: &Value, path: &str) -> Vec<FitIssue> {
    let mut issues = Vec::new();
    let badv = str_list(req.shared.badv);
    if !badv.is_empty() {
        if let Some(domains) = bid.get("adomain").and_then(|v| v.as_array()) {
            for d in domains {
                if let Some(s) = d.as_str() {
                    if badv.iter().any(|b| b.eq_ignore_ascii_case(s)) {
                        issues.push(FitIssue::new(
                            CODE_ADOMAIN_BLOCKED,
                            Severity::Warn,
                            format!("{path}.adomain"),
                            "adomain hit BidRequest.badv",
                        ));
                        break;
                    }
                }
            }
        }
    }
    let bapp = str_list(req.shared.bapp);
    if !bapp.is_empty() {
        if let Some(bundle) = bid.get("bundle").and_then(|v| v.as_str()).map(str::trim) {
            if !bundle.is_empty() && bapp.iter().any(|b| b.eq_ignore_ascii_case(bundle)) {
                issues.push(FitIssue::new(
                    CODE_BUNDLE_BLOCKED,
                    Severity::Warn,
                    format!("{path}.bundle"),
                    "bundle hit BidRequest.bapp",
                ));
            }
        }
    }
    let bcat = str_list(req.shared.bcat);
    if !bcat.is_empty() {
        if let Some(cats) = bid.get("cat").and_then(|v| v.as_array()) {
            for c in cats {
                if let Some(s) = c.as_str() {
                    if bcat.iter().any(|b| b.eq_ignore_ascii_case(s)) {
                        issues.push(FitIssue::new(
                            CODE_CAT_BLOCKED,
                            Severity::Warn,
                            format!("{path}.cat"),
                            "cat hit BidRequest.bcat",
                        ));
                        break;
                    }
                }
            }
        }
    }
    issues
}

fn str_list(vals: &[Value]) -> Vec<&str> {
    vals.iter().filter_map(|v| v.as_str()).collect()
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::view::{run_request, MarkupMask};
    use serde_json::json;

    fn banner_req() -> Value {
        json!({
            "id": "a1",
            "at": 1,
            "cur": ["USD"],
            "site": {"id": "s1"},
            "imp": [{
                "id": "1",
                "bidfloor": 1.0,
                "bidfloorcur": "USD",
                "banner": {"w": 300, "h": 250}
            }]
        })
    }

    #[test]
    fn video_mimes_missing() {
        let req = json!({
            "id": "x", "at": 1, "cur": ["USD"],
            "imp": [{"id": "1", "video": {}}]
        });
        let snap = run_request(&req).unwrap();
        let r = imp_ready_mtype(&snap.imps[0], 2);
        assert!(!r.ok());
        assert!(r.has(CODE_VIDEO_MIMES_MISSING));
    }

    #[test]
    fn multi_needs_mtype() {
        let req = json!({
            "id": "m", "at": 1, "cur": ["USD"],
            "imp": [{
                "id": "1",
                "banner": {"w": 320, "h": 50},
                "video": {"mimes": ["video/mp4"]}
            }]
        });
        let snap = run_request(&req).unwrap();
        let bid = json!({"id": "b1", "impid": "1", "price": 2.0});
        let r = super::bid(&snap, &bid);
        assert!(!r.ok());
        assert!(r.has(CODE_MTYPE_REQUIRED));
    }

    #[test]
    fn mtype_mismatch() {
        let req = banner_req();
        let snap = run_request(&req).unwrap();
        let bid = json!({"id": "b1", "impid": "1", "price": 2.0, "mtype": 2});
        let r = super::bid(&snap, &bid);
        assert!(!r.ok());
        assert!(r.has(CODE_MTYPE_MISMATCH));
    }

    #[test]
    fn price_below_floor_warn() {
        let req = banner_req();
        let snap = run_request(&req).unwrap();
        let bid = json!({"id": "b1", "impid": "1", "price": 0.5, "mtype": 1});
        // Fit.bid has no response cur → skip floor; use response path.
        assert!(!super::bid(&snap, &bid).has(CODE_PRICE_BELOW_FLOOR));
        let res = json!({
            "id": "a1", "cur": "USD",
            "seatbid": [{"bid": [bid]}]
        });
        let r = response(&snap, &res);
        assert!(r.ok());
        assert!(r.has(CODE_PRICE_BELOW_FLOOR));
    }

    #[test]
    fn response_non_object_malformed() {
        let req = banner_req();
        let snap = run_request(&req).unwrap();
        let r = response(&snap, &json!([]));
        assert!(!r.ok());
        assert!(r.has(CODE_MALFORMED));
    }

    #[test]
    fn response_seatbid_non_array_malformed() {
        let req = banner_req();
        let snap = run_request(&req).unwrap();
        let r = response(&snap, &json!({"id": "a1", "cur": "USD", "seatbid": {}}));
        assert!(!r.ok());
        assert!(r.has(CODE_MALFORMED));
    }

    #[test]
    fn response_seatbid_empty_bid_malformed() {
        let req = banner_req();
        let snap = run_request(&req).unwrap();
        for seatbid in [
            json!([{}]),
            json!([{"bid": []}]),
            json!([{"bid": 1}]),
            json!([{"bid": [1]}]),
        ] {
            let r = response(&snap, &json!({"id": "a1", "cur": "USD", "seatbid": seatbid}));
            assert!(!r.ok(), "{r:?}");
            assert!(r.has(CODE_MALFORMED), "{r:?}");
        }
    }

    #[test]
    fn bid_non_object_malformed() {
        let req = banner_req();
        let snap = run_request(&req).unwrap();
        let r = super::bid(&snap, &json!([]));
        assert!(!r.ok());
        assert!(r.has(CODE_MALFORMED));
    }

    #[test]
    fn response_blank_cur_skips_cur_and_floor() {
        let req = banner_req();
        let snap = run_request(&req).unwrap();
        let bid = json!({"id": "b1", "impid": "1", "price": 0.5, "mtype": 1});
        let res = json!({
            "id": "a1", "cur": "   ",
            "seatbid": [{"bid": [bid]}]
        });
        let r = response(&snap, &res);
        assert!(r.ok());
        assert!(!r.has(CODE_CUR_NOT_ALLOWED));
        assert!(!r.has(CODE_PRICE_BELOW_FLOOR));
    }

    #[test]
    fn response_non_string_cur_malformed() {
        let req = banner_req();
        let snap = run_request(&req).unwrap();
        let bid = json!({"id": "b1", "impid": "1", "price": 2.0, "mtype": 1});
        let r = response(
            &snap,
            &json!({"id": "a1", "cur": 1, "seatbid": [{"bid": [bid]}]}),
        );
        assert!(!r.ok());
        assert!(r.has(CODE_MALFORMED));
    }

    #[test]
    fn fit_result_warnings_errors() {
        let mut r = FitResult {
            issues: vec![
                FitIssue::new(CODE_IMP_NOT_FOUND, Severity::Error, "x", "e"),
                FitIssue::new(CODE_PRICE_BELOW_FLOOR, Severity::Warn, "y", "w"),
            ],
        };
        assert_eq!(r.errors().count(), 1);
        assert_eq!(r.warnings().count(), 1);
        assert!(!r.ok());
        let _ = &mut r;
    }

    #[test]
    fn response_cur_whitespace_trimmed_for_allow_and_floor() {
        let req = banner_req();
        let snap = run_request(&req).unwrap();
        let bid = json!({"id": "b1", "impid": "1", "price": 0.5, "mtype": 1});
        let res = json!({
            "id": "a1", "cur": "  USD  ",
            "seatbid": [{"bid": [bid]}]
        });
        let r = response(&snap, &res);
        assert!(r.ok());
        assert!(!r.has(CODE_CUR_NOT_ALLOWED));
        assert!(r.has(CODE_PRICE_BELOW_FLOOR));
    }

    #[test]
    fn attr_blocked_warn() {
        let req = json!({
            "id": "x", "at": 1, "cur": ["USD"],
            "imp": [{"id": "1", "banner": {"w": 300, "h": 250, "battr": [1]}}]
        });
        let snap = run_request(&req).unwrap();
        let bid = json!({"id": "b1", "impid": "1", "price": 2.0, "mtype": 1, "attr": [1]});
        let r = super::bid(&snap, &bid);
        assert!(r.ok());
        assert!(r.has(CODE_ATTR_BLOCKED));
    }

    #[test]
    fn no_bid_response() {
        let req = banner_req();
        let snap = run_request(&req).unwrap();
        let res = json!({"id": "a1", "cur": "USD", "nbr": 0});
        let r = response(&snap, &res);
        assert!(r.ok());
    }

    #[test]
    fn impid_not_found() {
        let req = banner_req();
        let snap = run_request(&req).unwrap();
        let bid = json!({"id": "b1", "impid": "missing", "price": 2.0, "mtype": 1});
        let r = super::bid(&snap, &bid);
        assert!(!r.ok());
        assert!(r.has(CODE_IMP_NOT_FOUND));
    }

    #[test]
    fn response_happy() {
        let req = banner_req();
        let snap = run_request(&req).unwrap();
        let res = json!({
            "id": "a1",
            "cur": "USD",
            "seatbid": [{"bid": [{
                "id": "b1", "impid": "1", "price": 2.0, "mtype": 1, "adm": "<a/>"
            }]}]
        });
        let r = response(&snap, &res);
        assert!(r.ok());
    }

    #[test]
    fn imp_ready_mtype_unknown() {
        let req = banner_req();
        let snap = run_request(&req).unwrap();
        let r = imp_ready_mtype(&snap.imps[0], 9);
        assert!(!r.ok());
        assert!(r.has(CODE_MTYPE_UNKNOWN));
    }

    #[test]
    fn imp_ready_markup_multi_bit() {
        let req = banner_req();
        let snap = run_request(&req).unwrap();
        let r = imp_ready_markup(&snap.imps[0], MarkupMask::BANNER | MarkupMask::VIDEO);
        assert!(!r.ok());
        assert!(r.has(CODE_MULTI_NEEDS_CHOICE));
    }

    #[test]
    fn imp_ready_banner_size_missing() {
        let req = json!({
            "id": "x", "at": 1, "cur": ["USD"],
            "imp": [{"id": "1", "banner": {}}]
        });
        let snap = run_request(&req).unwrap();
        let r = imp_ready_markup(&snap.imps[0], MarkupMask::BANNER);
        assert!(!r.ok());
        assert!(r.has(CODE_BANNER_SIZE_MISSING));
    }

    #[test]
    fn imp_ready_banner_format_array_ok() {
        let req = json!({
            "id": "x", "at": 1, "cur": ["USD"],
            "imp": [{"id": "1", "banner": {"format": [{"w": 300, "h": 250}]}}]
        });
        let snap = run_request(&req).unwrap();
        let r = imp_ready_markup(&snap.imps[0], MarkupMask::BANNER);
        assert!(r.ok());
    }

    #[test]
    fn audio_mimes_missing() {
        let req = json!({
            "id": "x", "at": 1, "cur": ["USD"],
            "imp": [{"id": "1", "audio": {}}]
        });
        let snap = run_request(&req).unwrap();
        let r = imp_ready_mtype(&snap.imps[0], 3);
        assert!(!r.ok());
        assert!(r.has(CODE_AUDIO_MIMES_MISSING));
    }

    #[test]
    fn native_request_missing() {
        let req = json!({
            "id": "x", "at": 1, "cur": ["USD"],
            "imp": [{"id": "1", "native": {"ver": "1.2"}}]
        });
        let snap = run_request(&req).unwrap();
        let r = imp_ready_mtype(&snap.imps[0], 4);
        assert!(!r.ok());
        assert!(r.has(CODE_NATIVE_REQUEST_MISSING));
    }

    #[test]
    fn floor_cur_diff_skips_compare() {
        let req = json!({
            "id": "a1", "at": 1, "cur": ["USD"],
            "imp": [{"id": "1", "bidfloor": 1.0, "bidfloorcur": "EUR", "banner": {"w": 1, "h": 1}}]
        });
        let snap = run_request(&req).unwrap();
        let bid = json!({"id": "b1", "impid": "1", "price": 0.1, "mtype": 1});
        let res = json!({
            "id": "a1", "cur": "USD",
            "seatbid": [{"bid": [bid.clone()]}]
        });
        let r = response(&snap, &res);
        assert!(r.ok());
        assert!(r.has(CODE_FLOOR_CUR_DIFF));
        assert!(!r.has(CODE_PRICE_BELOW_FLOOR));
    }

    #[test]
    fn adomain_blocked() {
        let req = json!({
            "id": "x", "at": 1, "cur": ["USD"], "badv": ["blocked.com"],
            "imp": [{"id": "1", "banner": {"w": 300, "h": 250}}]
        });
        let snap = run_request(&req).unwrap();
        let bid = json!({
            "id": "b1", "impid": "1", "price": 2.0, "mtype": 1,
            "adomain": ["blocked.com"]
        });
        let r = super::bid(&snap, &bid);
        assert!(r.has(CODE_ADOMAIN_BLOCKED));
    }

    #[test]
    fn bundle_blocked() {
        let req = json!({
            "id": "x", "at": 1, "cur": ["USD"], "bapp": ["com.blocked"],
            "imp": [{"id": "1", "banner": {"w": 300, "h": 250}}]
        });
        let snap = run_request(&req).unwrap();
        let bid = json!({
            "id": "b1", "impid": "1", "price": 2.0, "mtype": 1,
            "bundle": "com.blocked"
        });
        let r = super::bid(&snap, &bid);
        assert!(r.has(CODE_BUNDLE_BLOCKED));
    }

    #[test]
    fn cat_blocked() {
        let req = json!({
            "id": "x", "at": 1, "cur": ["USD"], "bcat": ["IAB25"],
            "imp": [{"id": "1", "banner": {"w": 300, "h": 250}}]
        });
        let snap = run_request(&req).unwrap();
        let bid = json!({
            "id": "b1", "impid": "1", "price": 2.0, "mtype": 1,
            "cat": ["IAB25"]
        });
        let r = super::bid(&snap, &bid);
        assert!(r.has(CODE_CAT_BLOCKED));
    }

    #[test]
    fn cur_not_allowed() {
        let req = banner_req();
        let snap = run_request(&req).unwrap();
        let res = json!({
            "id": "a1", "cur": "EUR",
            "seatbid": [{"bid": [{
                "id": "b1", "impid": "1", "price": 2.0, "mtype": 1
            }]}]
        });
        let r = response(&snap, &res);
        assert!(r.has(CODE_CUR_NOT_ALLOWED));
    }

    #[test]
    fn fit_result_has_and_severity() {
        let r = FitResult {
            issues: vec![FitIssue::new(CODE_IMP_NOT_FOUND, Severity::Error, "p", "m")],
        };
        assert!(!r.ok());
        assert!(r.has(CODE_IMP_NOT_FOUND));
        assert_eq!(Severity::Error.as_str(), "ERROR");
        assert_eq!(Severity::Warn.as_str(), "WARN");
    }
}

use serde_json::{json, Map, Value};

use crate::validate::validate_bid_response;
use crate::validate::ValidationResult;

/// JSON 字节及其 Schema 校验结果。
#[derive(Debug)]
pub struct ValidatedJson {
    /// 序列化后的 JSON 字节。
    pub json: Vec<u8>,
    /// Schema 校验结果。
    pub result: ValidationResult,
}

impl ValidatedJson {
    /// 是否通过 Schema 校验。
    pub fn ok(&self) -> bool {
        self.result.ok
    }
}

/// BidResponse 流式 Builder，输出 OpenRTB JSON。
#[derive(Clone, Debug)]
pub struct BidResponseBuilder {
    id: String,
    bidid: Option<String>,
    cur: String,
    nbr: Option<i32>,
    no_bid_set: bool,
    seatbid: Vec<Value>,
    error: Option<String>,
}

impl BidResponseBuilder {
    /// 创建 Builder；`request_id` 须回显 BidRequest.id。
    pub fn new(request_id: impl Into<String>) -> Self {
        let id = request_id.into();
        let error = if id.is_empty() {
            Some("BidResponse.id is required (echo BidRequest.id)".into())
        } else {
            None
        };
        Self {
            id,
            bidid: None,
            cur: "USD".into(),
            nbr: None,
            no_bid_set: false,
            seatbid: Vec::new(),
            error,
        }
    }

    /// 设置 DSP 侧 bid id（可选）。
    pub fn bid_id(mut self, bidid: impl Into<String>) -> Self {
        self.bidid = Some(bidid.into());
        self
    }

    /// 设置响应货币（ISO-4217，默认 `"USD"`）。
    pub fn currency(mut self, cur: impl Into<String>) -> Self {
        self.cur = cur.into();
        self
    }

    /// 结构化 No-Bid：设置 `nbr` 并清空 seatbid。
    pub fn no_bid(mut self, nbr: i32) -> Self {
        self.nbr = Some(nbr);
        self.seatbid.clear();
        self.no_bid_set = true;
        self
    }

    /// 追加一个 SeatBid；`bids` 至少含一条 Bid。
    pub fn add_seat_bid(mut self, seat: &str, bids: Vec<Value>) -> Self {
        if bids.is_empty() {
            self.error = Some("SeatBid requires at least one Bid".into());
            return self;
        }
        // Switching to a bid clears structured no-bid.
        self.no_bid_set = false;
        self.nbr = None;
        self.seatbid.push(json!({
            "seat": seat,
            "bid": bids,
        }));
        self
    }

    /// 构建 JSON 对象；执行 Builder 级校验。
    pub fn build(self) -> Result<Value, String> {
        if let Some(e) = self.error {
            return Err(e);
        }
        if self.cur.is_empty() {
            return Err("BidResponse.cur is required (ISO-4217)".into());
        }
        if self.seatbid.is_empty() && !self.no_bid_set {
            return Err("BidResponse needs seatbid[] or no_bid(nbr)".into());
        }
        for (i, sb) in self.seatbid.iter().enumerate() {
            let bids = sb
                .get("bid")
                .and_then(|v| v.as_array())
                .ok_or_else(|| format!("seatbid[{i}].bid missing"))?;
            for (j, bid) in bids.iter().enumerate() {
                let id = bid.get("id").and_then(|v| v.as_str()).unwrap_or("");
                let impid = bid.get("impid").and_then(|v| v.as_str()).unwrap_or("");
                let price = bid.get("price").and_then(|v| v.as_f64()).unwrap_or(0.0);
                if id.is_empty() || impid.is_empty() {
                    return Err(format!(
                        "seatbid[{i}].bid[{j}] requires id and impid"
                    ));
                }
                if price <= 0.0 {
                    return Err(format!(
                        "seatbid[{i}].bid[{j}].price must be > 0"
                    ));
                }
            }
        }
        let mut obj = Map::new();
        obj.insert("id".into(), json!(self.id));
        if let Some(bidid) = self.bidid {
            obj.insert("bidid".into(), json!(bidid));
        }
        obj.insert("cur".into(), json!(self.cur));
        if let Some(nbr) = self.nbr {
            obj.insert("nbr".into(), json!(nbr));
        }
        if !self.seatbid.is_empty() {
            obj.insert("seatbid".into(), Value::Array(self.seatbid));
        }
        Ok(Value::Object(obj))
    }

    /// 构建并序列化为 JSON 字节。
    pub fn build_json(self) -> Result<Vec<u8>, String> {
        let v = self.build()?;
        serde_json::to_vec(&v).map_err(|e| e.to_string())
    }

    /// 构建 JSON 并运行 BidResponse Schema 校验。
    pub fn build_validated(self) -> Result<ValidatedJson, String> {
        let json = self.build_json()?;
        let result = validate_bid_response(&json);
        Ok(ValidatedJson { json, result })
    }
}

/// 单条 Bid 的流式 Builder。
#[derive(Clone, Debug)]
pub struct BidBuilder {
    o: Map<String, Value>,
}

impl BidBuilder {
    /// 创建 Bid；`price` 须 > 0（由 BidResponseBuilder 校验）。
    pub fn new(id: &str, impid: &str, price: f64) -> Self {
        let mut o = Map::new();
        o.insert("id".into(), json!(id));
        o.insert("impid".into(), json!(impid));
        o.insert("price".into(), json!(price));
        Self { o }
    }

    /// 设置 adm（广告 markup）。
    pub fn adm(mut self, adm: &str) -> Self {
        self.o.insert("adm".into(), json!(adm));
        self
    }
    /// 设置 nurl（胜出通知 URL）。
    pub fn nurl(mut self, u: &str) -> Self {
        self.o.insert("nurl".into(), json!(u));
        self
    }
    /// 设置 burl（计费通知 URL）。
    pub fn burl(mut self, u: &str) -> Self {
        self.o.insert("burl".into(), json!(u));
        self
    }
    /// 设置 crid（创意 id）。
    pub fn crid(mut self, crid: &str) -> Self {
        self.o.insert("crid".into(), json!(crid));
        self
    }
    /// 设置 cid（活动/广告组 id）。
    pub fn cid(mut self, cid: &str) -> Self {
        self.o.insert("cid".into(), json!(cid));
        self
    }
    /// 设置广告主域名列表。
    pub fn adomain(mut self, domains: &[&str]) -> Self {
        self.o.insert("adomain".into(), json!(domains));
        self
    }
    /// 设置创意宽高。
    pub fn size(mut self, w: i32, h: i32) -> Self {
        self.o.insert("w".into(), json!(w));
        self.o.insert("h".into(), json!(h));
        self
    }
    /// 设置 PMP deal id。
    pub fn deal_id(mut self, id: &str) -> Self {
        self.o.insert("dealid".into(), json!(id));
        self
    }
    /// 设置 OpenRTB `mtype`（1=banner, 2=video, 3=audio, 4=native）。
    pub fn markup_type(mut self, m: i32) -> Self {
        self.o.insert("mtype".into(), json!(m));
        self
    }
    /// 等价于 `markup_type(1)`。
    pub fn banner(self) -> Self {
        self.markup_type(1)
    }
    /// 等价于 `markup_type(2)`。
    pub fn video(self) -> Self {
        self.markup_type(2)
    }
    /// 等价于 `markup_type(3)`。
    pub fn audio(self) -> Self {
        self.markup_type(3)
    }
    /// 等价于 `markup_type(4)`。
    pub fn native(self) -> Self {
        self.markup_type(4)
    }
    /// 设置创意时长（秒，多用于 video/audio）。
    pub fn dur(mut self, seconds: i32) -> Self {
        self.o.insert("dur".into(), json!(seconds));
        self
    }
    /// 输出 Bid JSON 对象。
    pub fn build(self) -> Value {
        Value::Object(self.o)
    }
}

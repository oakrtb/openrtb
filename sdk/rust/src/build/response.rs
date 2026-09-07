use serde_json::{json, Map, Value};

use crate::validate::{validate_bid_response, ValidationResult};

/// JSON bytes plus schema validation outcome.
#[derive(Debug)]
pub struct ValidatedJson {
    pub json: Vec<u8>,
    pub result: ValidationResult,
}

impl ValidatedJson {
    pub fn ok(&self) -> bool {
        self.result.ok
    }
}

/// BidResponse builder → OpenRTB JSON.
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

    pub fn bid_id(mut self, bidid: impl Into<String>) -> Self {
        self.bidid = Some(bidid.into());
        self
    }

    pub fn currency(mut self, cur: impl Into<String>) -> Self {
        self.cur = cur.into();
        self
    }

    pub fn no_bid(mut self, nbr: i32) -> Self {
        self.nbr = Some(nbr);
        self.seatbid.clear();
        self.no_bid_set = true;
        self
    }

    pub fn add_seat_bid(mut self, seat: &str, bids: Vec<Value>) -> Self {
        if bids.is_empty() {
            self.error = Some("SeatBid requires at least one Bid".into());
            return self;
        }
        self.seatbid.push(json!({
            "seat": seat,
            "bid": bids,
        }));
        self
    }

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

    pub fn build_json(self) -> Result<Vec<u8>, String> {
        let v = self.build()?;
        serde_json::to_vec(&v).map_err(|e| e.to_string())
    }

    pub fn build_validated(self) -> Result<ValidatedJson, String> {
        let json = self.build_json()?;
        let result = validate_bid_response(&json);
        Ok(ValidatedJson { json, result })
    }
}

/// Single Bid builder.
#[derive(Clone, Debug)]
pub struct BidBuilder {
    o: Map<String, Value>,
}

impl BidBuilder {
    pub fn new(id: &str, impid: &str, price: f64) -> Self {
        let mut o = Map::new();
        o.insert("id".into(), json!(id));
        o.insert("impid".into(), json!(impid));
        o.insert("price".into(), json!(price));
        Self { o }
    }

    pub fn adm(mut self, adm: &str) -> Self {
        self.o.insert("adm".into(), json!(adm));
        self
    }
    pub fn nurl(mut self, u: &str) -> Self {
        self.o.insert("nurl".into(), json!(u));
        self
    }
    pub fn burl(mut self, u: &str) -> Self {
        self.o.insert("burl".into(), json!(u));
        self
    }
    pub fn crid(mut self, crid: &str) -> Self {
        self.o.insert("crid".into(), json!(crid));
        self
    }
    pub fn cid(mut self, cid: &str) -> Self {
        self.o.insert("cid".into(), json!(cid));
        self
    }
    pub fn adomain(mut self, domains: &[&str]) -> Self {
        self.o.insert("adomain".into(), json!(domains));
        self
    }
    pub fn size(mut self, w: i32, h: i32) -> Self {
        self.o.insert("w".into(), json!(w));
        self.o.insert("h".into(), json!(h));
        self
    }
    pub fn deal_id(mut self, id: &str) -> Self {
        self.o.insert("dealid".into(), json!(id));
        self
    }
    pub fn markup_type(mut self, m: i32) -> Self {
        self.o.insert("mtype".into(), json!(m));
        self
    }
    pub fn banner(self) -> Self {
        self.markup_type(1)
    }
    pub fn video(self) -> Self {
        self.markup_type(2)
    }
    pub fn audio(self) -> Self {
        self.markup_type(3)
    }
    pub fn native(self) -> Self {
        self.markup_type(4)
    }
    pub fn dur(mut self, seconds: i32) -> Self {
        self.o.insert("dur".into(), json!(seconds));
        self
    }
    pub fn build(self) -> Value {
        Value::Object(self.o)
    }
}

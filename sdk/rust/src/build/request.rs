use serde_json::{json, Map, Value};

use crate::validate::validate_bid_request;

use super::response::ValidatedJson;

/// BidRequest builder → OpenRTB JSON object.
#[derive(Clone, Debug, Default)]
pub struct BidRequestBuilder {
    id: String,
    at: Option<i32>,
    tmax: Option<i32>,
    cur: Vec<String>,
    test: Option<i32>,
    bcat: Vec<String>,
    badv: Vec<String>,
    site: Option<Value>,
    app: Option<Value>,
    dooh: Option<Value>,
    device: Option<Value>,
    user: Option<Value>,
    regs: Option<Value>,
    source: Option<Value>,
    imps: Vec<Value>,
    error: Option<String>,
}

impl BidRequestBuilder {
    pub fn new(id: impl Into<String>) -> Self {
        let id = id.into();
        let error = if id.is_empty() {
            Some("BidRequest.id is required".into())
        } else {
            None
        };
        Self {
            id,
            error,
            ..Default::default()
        }
    }

    pub fn first_price(mut self) -> Self {
        self.at = Some(1);
        self
    }

    pub fn second_price_plus(mut self) -> Self {
        self.at = Some(2);
        self
    }

    pub fn auction_type(mut self, at: i32) -> Self {
        self.at = Some(at);
        self
    }

    pub fn tmax(mut self, ms: i32) -> Self {
        self.tmax = Some(ms);
        self
    }

    pub fn currency(mut self, codes: &[&str]) -> Self {
        self.cur = codes.iter().map(|s| (*s).to_string()).collect();
        self
    }

    pub fn test(mut self) -> Self {
        self.test = Some(1);
        self
    }

    pub fn bcat(mut self, cats: &[&str]) -> Self {
        self.bcat = cats.iter().map(|s| (*s).to_string()).collect();
        self
    }

    pub fn badv(mut self, domains: &[&str]) -> Self {
        self.badv = domains.iter().map(|s| (*s).to_string()).collect();
        self
    }

    pub fn site(mut self, site: Value) -> Self {
        self.site = Some(site);
        self.app = None;
        self.dooh = None;
        self
    }

    pub fn app(mut self, app: Value) -> Self {
        self.app = Some(app);
        self.site = None;
        self.dooh = None;
        self
    }

    pub fn dooh(mut self, dooh: Value) -> Self {
        self.dooh = Some(dooh);
        self.site = None;
        self.app = None;
        self
    }

    pub fn device(mut self, device: Value) -> Self {
        self.device = Some(device);
        self
    }

    pub fn user(mut self, user: Value) -> Self {
        self.user = Some(user);
        self
    }

    pub fn regs(mut self, regs: Value) -> Self {
        self.regs = Some(regs);
        self
    }

    pub fn source(mut self, source: Value) -> Self {
        self.source = Some(source);
        self
    }

    pub fn add_imp(mut self, imp: Value) -> Self {
        self.imps.push(imp);
        self
    }

    pub fn build(self) -> Result<Value, String> {
        if let Some(e) = self.error {
            return Err(e);
        }
        let at = match self.at {
            Some(v) if v != 0 => v,
            _ => {
                return Err(
                    "BidRequest.at is required (use first_price/second_price_plus/auction_type)"
                        .into(),
                )
            }
        };
        if self.cur.is_empty() {
            return Err("BidRequest.cur is required (at least one ISO-4217 code)".into());
        }
        if self.imps.is_empty() {
            return Err("BidRequest.imp requires at least one Imp".into());
        }
        for (i, imp) in self.imps.iter().enumerate() {
            check_imp(imp, i)?;
        }
        let mut obj = Map::new();
        obj.insert("id".into(), json!(self.id));
        obj.insert("at".into(), json!(at));
        if let Some(tmax) = self.tmax {
            obj.insert("tmax".into(), json!(tmax));
        }
        obj.insert("cur".into(), json!(self.cur));
        if let Some(test) = self.test {
            obj.insert("test".into(), json!(test));
        }
        if !self.bcat.is_empty() {
            obj.insert("bcat".into(), json!(self.bcat));
        }
        if !self.badv.is_empty() {
            obj.insert("badv".into(), json!(self.badv));
        }
        if let Some(v) = self.site {
            obj.insert("site".into(), v);
        }
        if let Some(v) = self.app {
            obj.insert("app".into(), v);
        }
        if let Some(v) = self.dooh {
            obj.insert("dooh".into(), v);
        }
        if let Some(v) = self.device {
            obj.insert("device".into(), v);
        }
        if let Some(v) = self.user {
            obj.insert("user".into(), v);
        }
        if let Some(v) = self.regs {
            obj.insert("regs".into(), v);
        }
        if let Some(v) = self.source {
            obj.insert("source".into(), v);
        }
        obj.insert("imp".into(), Value::Array(self.imps));
        Ok(Value::Object(obj))
    }

    pub fn build_json(self) -> Result<Vec<u8>, String> {
        let v = self.build()?;
        serde_json::to_vec(&v).map_err(|e| e.to_string())
    }

    pub fn build_validated(self) -> Result<ValidatedJson, String> {
        let json = self.build_json()?;
        let result = validate_bid_request(&json);
        Ok(ValidatedJson { json, result })
    }
}

fn check_imp(imp: &Value, i: usize) -> Result<(), String> {
    let obj = imp
        .as_object()
        .ok_or_else(|| format!("imp[{i}] must be object"))?;
    let id = obj.get("id").and_then(|v| v.as_str()).unwrap_or("");
    if id.is_empty() {
        return Err(format!("imp[{i}].id is required"));
    }
    let mut formats = 0;
    if let Some(banner) = obj.get("banner") {
        formats += 1;
        let b = banner
            .as_object()
            .ok_or_else(|| format!("imp[{i}].banner must be object"))?;
        let w = b.get("w").and_then(|v| v.as_i64()).unwrap_or(0);
        let h = b.get("h").and_then(|v| v.as_i64()).unwrap_or(0);
        let formats_len = b
            .get("format")
            .and_then(|v| v.as_array())
            .map(|a| a.len())
            .unwrap_or(0);
        if w == 0 && h == 0 && formats_len == 0 {
            return Err(format!("imp[{i}].banner needs w/h or format[]"));
        }
    }
    if let Some(video) = obj.get("video") {
        formats += 1;
        let mimes = video
            .get("mimes")
            .and_then(|v| v.as_array())
            .map(|a| a.len())
            .unwrap_or(0);
        if mimes == 0 {
            return Err(format!("imp[{i}].video.mimes is required"));
        }
    }
    if let Some(audio) = obj.get("audio") {
        formats += 1;
        let mimes = audio
            .get("mimes")
            .and_then(|v| v.as_array())
            .map(|a| a.len())
            .unwrap_or(0);
        if mimes == 0 {
            return Err(format!("imp[{i}].audio.mimes is required"));
        }
    }
    if let Some(native) = obj.get("native") {
        formats += 1;
        let req = native
            .get("request")
            .and_then(|v| v.as_str())
            .unwrap_or("");
        if req.is_empty() {
            return Err(format!("imp[{i}].native.request is required"));
        }
    }
    if formats == 0 {
        return Err(format!(
            "imp[{i}] needs banner, video, audio, or native"
        ));
    }
    Ok(())
}

// --- Imp / inventory helpers -------------------------------------------------

fn base_imp(id: &str) -> Map<String, Value> {
    let mut m = Map::new();
    m.insert("id".into(), json!(id));
    m
}

macro_rules! imp_common {
    ($s:ident) => {
        pub fn floor(mut self, bidfloor: f64, cur: &str) -> Self {
            self.imp.insert("bidfloor".into(), json!(bidfloor));
            self.imp.insert("bidfloorcur".into(), json!(cur));
            self
        }
        pub fn secure(mut self) -> Self {
            self.imp.insert("secure".into(), json!(1));
            self
        }
        pub fn tag_id(mut self, tagid: &str) -> Self {
            self.imp.insert("tagid".into(), json!(tagid));
            self
        }
        pub fn interstitial(mut self) -> Self {
            self.imp.insert("instl".into(), json!(1));
            self
        }
        pub fn rewarded(mut self) -> Self {
            self.imp.insert("rwdd".into(), json!(1));
            self
        }
    };
}

#[derive(Clone, Debug)]
pub struct BannerImpBuilder {
    imp: Map<String, Value>,
    banner: Map<String, Value>,
}

impl BannerImpBuilder {
    pub fn new(id: impl Into<String>) -> Self {
        Self {
            imp: base_imp(&id.into()),
            banner: Map::new(),
        }
    }
    imp_common!(self);
    pub fn size(mut self, w: i32, h: i32) -> Self {
        self.banner.insert("w".into(), json!(w));
        self.banner.insert("h".into(), json!(h));
        self
    }
    pub fn pos(mut self, pos: i32) -> Self {
        self.banner.insert("pos".into(), json!(pos));
        self
    }
    pub fn mimes(mut self, mimes: &[&str]) -> Self {
        self.banner.insert("mimes".into(), json!(mimes));
        self
    }
    pub fn build(mut self) -> Value {
        self.imp.insert("banner".into(), Value::Object(self.banner));
        Value::Object(self.imp)
    }
}

#[derive(Clone, Debug)]
pub struct VideoImpBuilder {
    imp: Map<String, Value>,
    video: Map<String, Value>,
}

impl VideoImpBuilder {
    pub fn new(id: impl Into<String>) -> Self {
        Self {
            imp: base_imp(&id.into()),
            video: Map::new(),
        }
    }
    imp_common!(self);
    pub fn mimes(mut self, mimes: &[&str]) -> Self {
        self.video.insert("mimes".into(), json!(mimes));
        self
    }
    pub fn duration(mut self, min: i32, max: i32) -> Self {
        self.video.insert("minduration".into(), json!(min));
        self.video.insert("maxduration".into(), json!(max));
        self
    }
    pub fn protocols(mut self, protocols: &[i32]) -> Self {
        self.video.insert("protocols".into(), json!(protocols));
        self
    }
    pub fn size(mut self, w: i32, h: i32) -> Self {
        self.video.insert("w".into(), json!(w));
        self.video.insert("h".into(), json!(h));
        self
    }
    pub fn start_delay(mut self, v: i32) -> Self {
        self.video.insert("startdelay".into(), json!(v));
        self
    }
    pub fn plcmt(mut self, plcmt: i32) -> Self {
        self.video.insert("plcmt".into(), json!(plcmt));
        self
    }
    pub fn linearity(mut self, v: i32) -> Self {
        self.video.insert("linearity".into(), json!(v));
        self
    }
    pub fn skip(mut self, skipafter: i32) -> Self {
        self.video.insert("skip".into(), json!(1));
        self.video.insert("skipafter".into(), json!(skipafter));
        self
    }
    pub fn pod(mut self, podid: &str, slotinpod: i32) -> Self {
        self.video.insert("podid".into(), json!(podid));
        self.video.insert("slotinpod".into(), json!(slotinpod));
        self
    }
    pub fn playback_method(mut self, methods: &[i32]) -> Self {
        self.video.insert("playbackmethod".into(), json!(methods));
        self
    }
    pub fn build(mut self) -> Value {
        self.imp.insert("video".into(), Value::Object(self.video));
        Value::Object(self.imp)
    }
}

#[derive(Clone, Debug)]
pub struct AudioImpBuilder {
    imp: Map<String, Value>,
    audio: Map<String, Value>,
}

impl AudioImpBuilder {
    pub fn new(id: impl Into<String>) -> Self {
        Self {
            imp: base_imp(&id.into()),
            audio: Map::new(),
        }
    }
    imp_common!(self);
    pub fn mimes(mut self, mimes: &[&str]) -> Self {
        self.audio.insert("mimes".into(), json!(mimes));
        self
    }
    pub fn duration(mut self, min: i32, max: i32) -> Self {
        self.audio.insert("minduration".into(), json!(min));
        self.audio.insert("maxduration".into(), json!(max));
        self
    }
    pub fn protocols(mut self, protocols: &[i32]) -> Self {
        self.audio.insert("protocols".into(), json!(protocols));
        self
    }
    pub fn feed(mut self, feed: i32) -> Self {
        self.audio.insert("feed".into(), json!(feed));
        self
    }
    pub fn build(mut self) -> Value {
        self.imp.insert("audio".into(), Value::Object(self.audio));
        Value::Object(self.imp)
    }
}

#[derive(Clone, Debug)]
pub struct NativeImpBuilder {
    imp: Map<String, Value>,
    native: Map<String, Value>,
}

impl NativeImpBuilder {
    pub fn new(id: impl Into<String>) -> Self {
        let mut native = Map::new();
        native.insert("ver".into(), json!("1.2"));
        Self {
            imp: base_imp(&id.into()),
            native,
        }
    }
    imp_common!(self);
    pub fn request(mut self, native_request_json: &str) -> Self {
        self.native
            .insert("request".into(), json!(native_request_json));
        self
    }
    pub fn ver(mut self, ver: &str) -> Self {
        self.native.insert("ver".into(), json!(ver));
        self
    }
    pub fn build(mut self) -> Value {
        self.imp.insert("native".into(), Value::Object(self.native));
        Value::Object(self.imp)
    }
}

#[derive(Clone, Debug, Default)]
pub struct SiteBuilder {
    o: Map<String, Value>,
}
impl SiteBuilder {
    pub fn new() -> Self {
        Self::default()
    }
    pub fn id(mut self, v: &str) -> Self {
        self.o.insert("id".into(), json!(v));
        self
    }
    pub fn name(mut self, v: &str) -> Self {
        self.o.insert("name".into(), json!(v));
        self
    }
    pub fn domain(mut self, v: &str) -> Self {
        self.o.insert("domain".into(), json!(v));
        self
    }
    pub fn page(mut self, v: &str) -> Self {
        self.o.insert("page".into(), json!(v));
        self
    }
    pub fn cat(mut self, cats: &[&str]) -> Self {
        self.o.insert("cat".into(), json!(cats));
        self
    }
    pub fn publisher(mut self, p: Value) -> Self {
        self.o.insert("publisher".into(), p);
        self
    }
    pub fn build(self) -> Value {
        Value::Object(self.o)
    }
}

#[derive(Clone, Debug, Default)]
pub struct AppBuilder {
    o: Map<String, Value>,
}
impl AppBuilder {
    pub fn new() -> Self {
        Self::default()
    }
    pub fn id(mut self, v: &str) -> Self {
        self.o.insert("id".into(), json!(v));
        self
    }
    pub fn name(mut self, v: &str) -> Self {
        self.o.insert("name".into(), json!(v));
        self
    }
    pub fn bundle(mut self, v: &str) -> Self {
        self.o.insert("bundle".into(), json!(v));
        self
    }
    pub fn domain(mut self, v: &str) -> Self {
        self.o.insert("domain".into(), json!(v));
        self
    }
    pub fn publisher(mut self, p: Value) -> Self {
        self.o.insert("publisher".into(), p);
        self
    }
    pub fn content(mut self, c: Value) -> Self {
        self.o.insert("content".into(), c);
        self
    }
    pub fn build(self) -> Value {
        Value::Object(self.o)
    }
}

#[derive(Clone, Debug, Default)]
pub struct DoohBuilder {
    o: Map<String, Value>,
}
impl DoohBuilder {
    pub fn new() -> Self {
        Self::default()
    }
    pub fn id(mut self, v: &str) -> Self {
        self.o.insert("id".into(), json!(v));
        self
    }
    pub fn name(mut self, v: &str) -> Self {
        self.o.insert("name".into(), json!(v));
        self
    }
    pub fn venue_type(mut self, ids: &[&str]) -> Self {
        self.o.insert("venuetype".into(), json!(ids));
        self
    }
    pub fn venue_type_tax(mut self, tax: i32) -> Self {
        self.o.insert("venuetypetax".into(), json!(tax));
        self
    }
    pub fn publisher(mut self, p: Value) -> Self {
        self.o.insert("publisher".into(), p);
        self
    }
    pub fn build(self) -> Value {
        Value::Object(self.o)
    }
}

#[derive(Clone, Debug, Default)]
pub struct DeviceBuilder {
    o: Map<String, Value>,
}
impl DeviceBuilder {
    pub fn new() -> Self {
        Self::default()
    }
    pub fn ua(mut self, v: &str) -> Self {
        self.o.insert("ua".into(), json!(v));
        self
    }
    pub fn ip(mut self, v: &str) -> Self {
        self.o.insert("ip".into(), json!(v));
        self
    }
    pub fn device_type(mut self, t: i32) -> Self {
        self.o.insert("devicetype".into(), json!(t));
        self
    }
    pub fn make(mut self, v: &str) -> Self {
        self.o.insert("make".into(), json!(v));
        self
    }
    pub fn model(mut self, v: &str) -> Self {
        self.o.insert("model".into(), json!(v));
        self
    }
    pub fn os(mut self, os: &str, osv: &str) -> Self {
        self.o.insert("os".into(), json!(os));
        self.o.insert("osv".into(), json!(osv));
        self
    }
    pub fn ifa(mut self, v: &str) -> Self {
        self.o.insert("ifa".into(), json!(v));
        self
    }
    pub fn geo(mut self, g: Value) -> Self {
        self.o.insert("geo".into(), g);
        self
    }
    pub fn build(self) -> Value {
        Value::Object(self.o)
    }
}

#[derive(Clone, Debug, Default)]
pub struct PublisherBuilder {
    o: Map<String, Value>,
}
impl PublisherBuilder {
    pub fn new() -> Self {
        Self::default()
    }
    pub fn id(mut self, v: &str) -> Self {
        self.o.insert("id".into(), json!(v));
        self
    }
    pub fn name(mut self, v: &str) -> Self {
        self.o.insert("name".into(), json!(v));
        self
    }
    pub fn domain(mut self, v: &str) -> Self {
        self.o.insert("domain".into(), json!(v));
        self
    }
    pub fn build(self) -> Value {
        Value::Object(self.o)
    }
}

#[derive(Clone, Debug, Default)]
pub struct ContentBuilder {
    o: Map<String, Value>,
}
impl ContentBuilder {
    pub fn new() -> Self {
        Self::default()
    }
    pub fn title(mut self, v: &str) -> Self {
        self.o.insert("title".into(), json!(v));
        self
    }
    pub fn series(mut self, v: &str) -> Self {
        self.o.insert("series".into(), json!(v));
        self
    }
    pub fn season(mut self, v: &str) -> Self {
        self.o.insert("season".into(), json!(v));
        self
    }
    pub fn episode(mut self, n: i32) -> Self {
        self.o.insert("episode".into(), json!(n));
        self
    }
    pub fn context(mut self, v: i32) -> Self {
        self.o.insert("context".into(), json!(v));
        self
    }
    pub fn livestream(mut self, v: i32) -> Self {
        self.o.insert("livestream".into(), json!(v));
        self
    }
    pub fn realtime(mut self, v: i32) -> Self {
        self.o.insert("realtime".into(), json!(v));
        self
    }
    pub fn build(self) -> Value {
        Value::Object(self.o)
    }
}

#[derive(Clone, Debug, Default)]
pub struct GeoBuilder {
    o: Map<String, Value>,
}
impl GeoBuilder {
    pub fn new() -> Self {
        Self::default()
    }
    pub fn lat_lon(mut self, lat: f64, lon: f64) -> Self {
        self.o.insert("lat".into(), json!(lat));
        self.o.insert("lon".into(), json!(lon));
        self
    }
    pub fn type_(mut self, t: i32) -> Self {
        self.o.insert("type".into(), json!(t));
        self
    }
    pub fn country(mut self, c: &str) -> Self {
        self.o.insert("country".into(), json!(c));
        self
    }
    pub fn city(mut self, c: &str) -> Self {
        self.o.insert("city".into(), json!(c));
        self
    }
    pub fn build(self) -> Value {
        Value::Object(self.o)
    }
}

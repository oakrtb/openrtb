use serde_json::{json, Map, Value};

use crate::schema::request;

use super::response::ValidatedJson;

/// BidRequest 流式 Builder，输出 OpenRTB JSON 对象。
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
    /// 创建 Builder；`id` 不能为空。
    pub fn new(id: impl Into<String>) -> Self {
        let id = id.into();
        let error = if id.trim().is_empty() {
            Some("build: BidRequest.id is required".into())
        } else {
            None
        };
        Self {
            id,
            error,
            ..Default::default()
        }
    }

    /// 第一价格拍卖（`at = 1`）。
    pub fn first_price(mut self) -> Self {
        self.at = Some(1);
        self
    }

    /// 第二价格加拍卖（`at = 2`）。
    pub fn second_price_plus(mut self) -> Self {
        self.at = Some(2);
        self
    }

    /// 设置拍卖类型 `at`。
    pub fn auction_type(mut self, at: i32) -> Self {
        self.at = Some(at);
        self
    }

    /// 设置最大处理时间 `tmax`（毫秒）。
    pub fn tmax(mut self, ms: i32) -> Self {
        self.tmax = Some(ms);
        self
    }

    /// 设置允许的货币代码列表（至少一项）。
    pub fn currency(mut self, codes: &[&str]) -> Self {
        self.cur = codes.iter().map(|s| (*s).to_string()).collect();
        self
    }

    /// 标记为测试请求（`test = 1`）。
    pub fn test(mut self) -> Self {
        self.test = Some(1);
        self
    }

    /// 设置屏蔽 IAB 类别 `bcat`。
    pub fn bcat(mut self, cats: &[&str]) -> Self {
        self.bcat = cats.iter().map(|s| (*s).to_string()).collect();
        self
    }

    /// 设置屏蔽广告主域名 `badv`。
    pub fn badv(mut self, domains: &[&str]) -> Self {
        self.badv = domains.iter().map(|s| (*s).to_string()).collect();
        self
    }

    /// 设置 Web 库存 `site`（与 app/dooh 互斥）。
    pub fn site(mut self, site: Value) -> Self {
        self.site = Some(site);
        self.app = None;
        self.dooh = None;
        self
    }

    /// 设置 App 库存 `app`（与 site/dooh 互斥）。
    pub fn app(mut self, app: Value) -> Self {
        self.app = Some(app);
        self.site = None;
        self.dooh = None;
        self
    }

    /// 设置 DOOH 库存 `dooh`（与 site/app 互斥）。
    pub fn dooh(mut self, dooh: Value) -> Self {
        self.dooh = Some(dooh);
        self.site = None;
        self.app = None;
        self
    }

    /// 设置 `device` 对象。
    pub fn device(mut self, device: Value) -> Self {
        self.device = Some(device);
        self
    }

    /// 设置 `user` 对象。
    pub fn user(mut self, user: Value) -> Self {
        self.user = Some(user);
        self
    }

    /// 设置 `regs` 对象。
    pub fn regs(mut self, regs: Value) -> Self {
        self.regs = Some(regs);
        self
    }

    /// 设置 `source` 对象。
    pub fn source(mut self, source: Value) -> Self {
        self.source = Some(source);
        self
    }

    /// 追加一条 Imp（须含 banner/video/audio/native 之一）。
    pub fn add_imp(mut self, imp: Value) -> Self {
        self.imps.push(imp);
        self
    }

    /// 构建 JSON 对象；执行 Builder 级校验。
    pub fn build(self) -> Result<Value, String> {
        if let Some(e) = self.error {
            return Err(e);
        }
        let at = match self.at {
            Some(v) if v != 0 => v,
            _ => return Err("build: BidRequest.at is required".into()),
        };
        if self.cur.is_empty() {
            return Err("build: BidRequest.cur is required (at least one ISO-4217 code)".into());
        }
        for (i, c) in self.cur.iter().enumerate() {
            if c.trim().is_empty() {
                return Err(format!("build: BidRequest.cur[{i}] is blank"));
            }
        }
        if self.imps.is_empty() {
            return Err("build: BidRequest.imp requires at least one Imp".into());
        }
        let inv = self.site.is_some() as u8
            + self.app.is_some() as u8
            + self.dooh.is_some() as u8;
        if inv > 1 {
            return Err("build: site/app/dooh are mutually exclusive".into());
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

    /// 构建并序列化为 JSON 字节。
    pub fn build_json(self) -> Result<Vec<u8>, String> {
        let v = self.build()?;
        serde_json::to_vec(&v).map_err(|e| e.to_string())
    }

    /// 构建 JSON 并运行 BidRequest Schema 校验（含 embedded native）。
    pub fn build_validated(self) -> Result<ValidatedJson, String> {
        let json = self.build_json()?;
        let result = request(&json);
        Ok(ValidatedJson { json, result })
    }
}

fn check_imp(imp: &Value, i: usize) -> Result<(), String> {
    let obj = imp
        .as_object()
        .ok_or_else(|| format!("build: imp[{i}] must be object"))?;
    let id = obj.get("id").and_then(|v| v.as_str()).unwrap_or("");
    if id.trim().is_empty() {
        return Err(format!("build: imp[{i}].id is required"));
    }
    let mut formats = 0;
    if let Some(banner) = obj.get("banner") {
        formats += 1;
        let b = banner
            .as_object()
            .ok_or_else(|| format!("build: imp[{i}].banner must be object"))?;
        let w = b.get("w").and_then(|v| v.as_i64()).unwrap_or(0);
        let h = b.get("h").and_then(|v| v.as_i64()).unwrap_or(0);
        let formats_len = b
            .get("format")
            .and_then(|v| v.as_array())
            .map(|a| a.len())
            .unwrap_or(0);
        if w == 0 && h == 0 && formats_len == 0 {
            return Err(format!("build: imp[{i}].banner needs w/h or format[]"));
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
            return Err(format!("build: imp[{i}].video.mimes is required"));
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
            return Err(format!("build: imp[{i}].audio.mimes is required"));
        }
    }
    if let Some(native) = obj.get("native") {
        formats += 1;
        let req = native
            .get("request")
            .and_then(|v| v.as_str())
            .unwrap_or("");
        if req.trim().is_empty() {
            return Err(format!("build: imp[{i}].native.request is required"));
        }
    }
    if formats == 0 {
        return Err(format!(
            "build: imp[{i}] needs banner, video, audio, or native"
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
        /// 设置底价及货币。
        pub fn floor(mut self, bidfloor: f64, cur: &str) -> Self {
            self.imp.insert("bidfloor".into(), json!(bidfloor));
            self.imp.insert("bidfloorcur".into(), json!(cur));
            self
        }
        /// 要求 HTTPS 创意（`secure = 1`）。
        pub fn secure(mut self) -> Self {
            self.imp.insert("secure".into(), json!(1));
            self
        }
        /// 设置广告位 tag id。
        pub fn tag_id(mut self, tagid: &str) -> Self {
            self.imp.insert("tagid".into(), json!(tagid));
            self
        }
        /// 插屏（`instl = 1`）。
        pub fn interstitial(mut self) -> Self {
            self.imp.insert("instl".into(), json!(1));
            self
        }
        /// 激励（`rwdd = 1`）。
        pub fn rewarded(mut self) -> Self {
            self.imp.insert("rwdd".into(), json!(1));
            self
        }
    };
}

/// Banner 类型 Imp Builder。
#[derive(Clone, Debug)]
pub struct BannerImpBuilder {
    imp: Map<String, Value>,
    banner: Map<String, Value>,
}

impl BannerImpBuilder {
    /// 创建 Banner Imp；`id` 为 Imp 标识。
    pub fn new(id: impl Into<String>) -> Self {
        Self {
            imp: base_imp(&id.into()),
            banner: Map::new(),
        }
    }
    imp_common!(self);
    /// 设置 Banner 宽高（与 `format[]` 二选一或兼有）。
    pub fn size(mut self, w: i32, h: i32) -> Self {
        self.banner.insert("w".into(), json!(w));
        self.banner.insert("h".into(), json!(h));
        self
    }
    /// 设置广告位置 `pos`。
    pub fn pos(mut self, pos: i32) -> Self {
        self.banner.insert("pos".into(), json!(pos));
        self
    }
    /// 设置允许的 MIME 类型。
    pub fn mimes(mut self, mimes: &[&str]) -> Self {
        self.banner.insert("mimes".into(), json!(mimes));
        self
    }
    /// 输出 Imp JSON 对象。
    pub fn build(mut self) -> Value {
        self.imp.insert("banner".into(), Value::Object(self.banner));
        Value::Object(self.imp)
    }
}

/// Video 类型 Imp Builder。
#[derive(Clone, Debug)]
pub struct VideoImpBuilder {
    imp: Map<String, Value>,
    video: Map<String, Value>,
}

impl VideoImpBuilder {
    /// 创建 Video Imp。
    pub fn new(id: impl Into<String>) -> Self {
        Self {
            imp: base_imp(&id.into()),
            video: Map::new(),
        }
    }
    imp_common!(self);
    /// 设置 video MIME 列表（必填）。
    pub fn mimes(mut self, mimes: &[&str]) -> Self {
        self.video.insert("mimes".into(), json!(mimes));
        self
    }
    /// 设置最小时长与最大时长（秒）。
    pub fn duration(mut self, min: i32, max: i32) -> Self {
        self.video.insert("minduration".into(), json!(min));
        self.video.insert("maxduration".into(), json!(max));
        self
    }
    /// 设置支持的视频协议列表。
    pub fn protocols(mut self, protocols: &[i32]) -> Self {
        self.video.insert("protocols".into(), json!(protocols));
        self
    }
    /// 设置播放器宽高。
    pub fn size(mut self, w: i32, h: i32) -> Self {
        self.video.insert("w".into(), json!(w));
        self.video.insert("h".into(), json!(h));
        self
    }
    /// 设置 `startdelay`。
    pub fn start_delay(mut self, v: i32) -> Self {
        self.video.insert("startdelay".into(), json!(v));
        self
    }
    /// 设置视频版位 `plcmt`。
    pub fn plcmt(mut self, plcmt: i32) -> Self {
        self.video.insert("plcmt".into(), json!(plcmt));
        self
    }
    /// 设置线性/非线性 `linearity`。
    pub fn linearity(mut self, v: i32) -> Self {
        self.video.insert("linearity".into(), json!(v));
        self
    }
    /// 启用可跳过及 `skipafter`（秒）。
    pub fn skip(mut self, skipafter: i32) -> Self {
        self.video.insert("skip".into(), json!(1));
        self.video.insert("skipafter".into(), json!(skipafter));
        self
    }
    /// 设置 pod 广告 `podid` 与 `slotinpod`。
    pub fn pod(mut self, podid: &str, slotinpod: i32) -> Self {
        self.video.insert("podid".into(), json!(podid));
        self.video.insert("slotinpod".into(), json!(slotinpod));
        self
    }
    /// 设置 `playbackmethod` 列表。
    pub fn playback_method(mut self, methods: &[i32]) -> Self {
        self.video.insert("playbackmethod".into(), json!(methods));
        self
    }
    /// 输出 Imp JSON 对象。
    pub fn build(mut self) -> Value {
        self.imp.insert("video".into(), Value::Object(self.video));
        Value::Object(self.imp)
    }
}

/// Audio 类型 Imp Builder。
#[derive(Clone, Debug)]
pub struct AudioImpBuilder {
    imp: Map<String, Value>,
    audio: Map<String, Value>,
}

impl AudioImpBuilder {
    /// 创建 Audio Imp。
    pub fn new(id: impl Into<String>) -> Self {
        Self {
            imp: base_imp(&id.into()),
            audio: Map::new(),
        }
    }
    imp_common!(self);
    /// 设置 audio MIME 列表（必填）。
    pub fn mimes(mut self, mimes: &[&str]) -> Self {
        self.audio.insert("mimes".into(), json!(mimes));
        self
    }
    /// 设置最小时长与最大时长（秒）。
    pub fn duration(mut self, min: i32, max: i32) -> Self {
        self.audio.insert("minduration".into(), json!(min));
        self.audio.insert("maxduration".into(), json!(max));
        self
    }
    /// 设置支持的音频协议列表。
    pub fn protocols(mut self, protocols: &[i32]) -> Self {
        self.audio.insert("protocols".into(), json!(protocols));
        self
    }
    /// 设置 feed 类型。
    pub fn feed(mut self, feed: i32) -> Self {
        self.audio.insert("feed".into(), json!(feed));
        self
    }
    /// 输出 Imp JSON 对象。
    pub fn build(mut self) -> Value {
        self.imp.insert("audio".into(), Value::Object(self.audio));
        Value::Object(self.imp)
    }
}

/// Native 类型 Imp Builder。
#[derive(Clone, Debug)]
pub struct NativeImpBuilder {
    imp: Map<String, Value>,
    native: Map<String, Value>,
}

impl NativeImpBuilder {
    /// 创建 Native Imp（默认 `native.ver = "1.2"`）。
    pub fn new(id: impl Into<String>) -> Self {
        let mut native = Map::new();
        native.insert("ver".into(), json!("1.2"));
        Self {
            imp: base_imp(&id.into()),
            native,
        }
    }
    imp_common!(self);
    /// 设置 Native Request JSON 字符串（必填）。
    pub fn request(mut self, native_request_json: &str) -> Self {
        self.native
            .insert("request".into(), json!(native_request_json));
        self
    }
    /// 设置 Native 协议版本。
    pub fn ver(mut self, ver: &str) -> Self {
        self.native.insert("ver".into(), json!(ver));
        self
    }
    /// 输出 Imp JSON 对象。
    pub fn build(mut self) -> Value {
        self.imp.insert("native".into(), Value::Object(self.native));
        Value::Object(self.imp)
    }
}

/// Site 对象 Builder（Web 库存）。
#[derive(Clone, Debug, Default)]
pub struct SiteBuilder {
    o: Map<String, Value>,
}
impl SiteBuilder {
    /// 创建空的 Site Builder。
    pub fn new() -> Self {
        Self::default()
    }
    /// 设置 site id。
    pub fn id(mut self, v: &str) -> Self {
        self.o.insert("id".into(), json!(v));
        self
    }
    /// 设置 site 名称。
    pub fn name(mut self, v: &str) -> Self {
        self.o.insert("name".into(), json!(v));
        self
    }
    /// 设置站点域名。
    pub fn domain(mut self, v: &str) -> Self {
        self.o.insert("domain".into(), json!(v));
        self
    }
    /// 设置当前页面 URL。
    pub fn page(mut self, v: &str) -> Self {
        self.o.insert("page".into(), json!(v));
        self
    }
    /// 设置 IAB 内容类别。
    pub fn cat(mut self, cats: &[&str]) -> Self {
        self.o.insert("cat".into(), json!(cats));
        self
    }
    /// 设置 Publisher 子对象。
    pub fn publisher(mut self, p: Value) -> Self {
        self.o.insert("publisher".into(), p);
        self
    }
    /// 输出 Site JSON 对象。
    pub fn build(self) -> Value {
        Value::Object(self.o)
    }
}

/// App 对象 Builder（移动/App 库存）。
#[derive(Clone, Debug, Default)]
pub struct AppBuilder {
    o: Map<String, Value>,
}
impl AppBuilder {
    /// 创建空的 App Builder。
    pub fn new() -> Self {
        Self::default()
    }
    /// 设置 app id。
    pub fn id(mut self, v: &str) -> Self {
        self.o.insert("id".into(), json!(v));
        self
    }
    /// 设置 app 名称。
    pub fn name(mut self, v: &str) -> Self {
        self.o.insert("name".into(), json!(v));
        self
    }
    /// 设置包名/bundle。
    pub fn bundle(mut self, v: &str) -> Self {
        self.o.insert("bundle".into(), json!(v));
        self
    }
    /// 设置 app 域名。
    pub fn domain(mut self, v: &str) -> Self {
        self.o.insert("domain".into(), json!(v));
        self
    }
    /// 设置 Publisher 子对象。
    pub fn publisher(mut self, p: Value) -> Self {
        self.o.insert("publisher".into(), p);
        self
    }
    /// 设置 Content 子对象（**非** [`crate::view::Inventory`]）。
    pub fn content(mut self, c: Value) -> Self {
        self.o.insert("content".into(), c);
        self
    }
    /// 输出 App JSON 对象。
    pub fn build(self) -> Value {
        Value::Object(self.o)
    }
}

/// DOOH 对象 Builder（数字户外库存）。
#[derive(Clone, Debug, Default)]
pub struct DoohBuilder {
    o: Map<String, Value>,
}
impl DoohBuilder {
    /// 创建空的 DOOH Builder。
    pub fn new() -> Self {
        Self::default()
    }
    /// 设置 dooh id。
    pub fn id(mut self, v: &str) -> Self {
        self.o.insert("id".into(), json!(v));
        self
    }
    /// 设置 dooh 名称。
    pub fn name(mut self, v: &str) -> Self {
        self.o.insert("name".into(), json!(v));
        self
    }
    /// 设置 venue 类型 id 列表。
    pub fn venue_type(mut self, ids: &[&str]) -> Self {
        self.o.insert("venuetype".into(), json!(ids));
        self
    }
    /// 设置 venue 类型 taxonomy。
    pub fn venue_type_tax(mut self, tax: i32) -> Self {
        self.o.insert("venuetypetax".into(), json!(tax));
        self
    }
    /// 设置 Publisher 子对象。
    pub fn publisher(mut self, p: Value) -> Self {
        self.o.insert("publisher".into(), p);
        self
    }
    /// 输出 DOOH JSON 对象。
    pub fn build(self) -> Value {
        Value::Object(self.o)
    }
}

/// Device 对象 Builder。
#[derive(Clone, Debug, Default)]
pub struct DeviceBuilder {
    o: Map<String, Value>,
}
impl DeviceBuilder {
    /// 创建空的 Device Builder。
    pub fn new() -> Self {
        Self::default()
    }
    /// 设置 User-Agent。
    pub fn ua(mut self, v: &str) -> Self {
        self.o.insert("ua".into(), json!(v));
        self
    }
    /// 设置 IPv4 地址。
    pub fn ip(mut self, v: &str) -> Self {
        self.o.insert("ip".into(), json!(v));
        self
    }
    /// 设置设备类型 `devicetype`。
    pub fn device_type(mut self, t: i32) -> Self {
        self.o.insert("devicetype".into(), json!(t));
        self
    }
    /// 设置设备制造商。
    pub fn make(mut self, v: &str) -> Self {
        self.o.insert("make".into(), json!(v));
        self
    }
    /// 设置设备型号。
    pub fn model(mut self, v: &str) -> Self {
        self.o.insert("model".into(), json!(v));
        self
    }
    /// 设置操作系统及版本。
    pub fn os(mut self, os: &str, osv: &str) -> Self {
        self.o.insert("os".into(), json!(os));
        self.o.insert("osv".into(), json!(osv));
        self
    }
    /// 设置 IFA（广告标识符）。
    pub fn ifa(mut self, v: &str) -> Self {
        self.o.insert("ifa".into(), json!(v));
        self
    }
    /// 设置 Geo 子对象。
    pub fn geo(mut self, g: Value) -> Self {
        self.o.insert("geo".into(), g);
        self
    }
    /// 输出 Device JSON 对象。
    pub fn build(self) -> Value {
        Value::Object(self.o)
    }
}

/// Publisher 对象 Builder。
#[derive(Clone, Debug, Default)]
pub struct PublisherBuilder {
    o: Map<String, Value>,
}
impl PublisherBuilder {
    /// 创建空的 Publisher Builder。
    pub fn new() -> Self {
        Self::default()
    }
    /// 设置 publisher id。
    pub fn id(mut self, v: &str) -> Self {
        self.o.insert("id".into(), json!(v));
        self
    }
    /// 设置 publisher 名称。
    pub fn name(mut self, v: &str) -> Self {
        self.o.insert("name".into(), json!(v));
        self
    }
    /// 设置 publisher 域名。
    pub fn domain(mut self, v: &str) -> Self {
        self.o.insert("domain".into(), json!(v));
        self
    }
    /// 输出 Publisher JSON 对象。
    pub fn build(self) -> Value {
        Value::Object(self.o)
    }
}

/// Content 对象 Builder（内容元数据；**非** [`crate::view::Inventory`]）。
#[derive(Clone, Debug, Default)]
pub struct ContentBuilder {
    o: Map<String, Value>,
}
impl ContentBuilder {
    /// 创建空的 Content Builder。
    pub fn new() -> Self {
        Self::default()
    }
    /// 设置内容标题。
    pub fn title(mut self, v: &str) -> Self {
        self.o.insert("title".into(), json!(v));
        self
    }
    /// 设置系列名称。
    pub fn series(mut self, v: &str) -> Self {
        self.o.insert("series".into(), json!(v));
        self
    }
    /// 设置季。
    pub fn season(mut self, v: &str) -> Self {
        self.o.insert("season".into(), json!(v));
        self
    }
    /// 设置集数。
    pub fn episode(mut self, n: i32) -> Self {
        self.o.insert("episode".into(), json!(n));
        self
    }
    /// 设置内容上下文 `context`。
    pub fn context(mut self, v: i32) -> Self {
        self.o.insert("context".into(), json!(v));
        self
    }
    /// 设置是否直播流。
    pub fn livestream(mut self, v: i32) -> Self {
        self.o.insert("livestream".into(), json!(v));
        self
    }
    /// 设置是否实时内容。
    pub fn realtime(mut self, v: i32) -> Self {
        self.o.insert("realtime".into(), json!(v));
        self
    }
    /// 输出 Content JSON 对象。
    pub fn build(self) -> Value {
        Value::Object(self.o)
    }
}

/// Geo 对象 Builder。
#[derive(Clone, Debug, Default)]
pub struct GeoBuilder {
    o: Map<String, Value>,
}
impl GeoBuilder {
    /// 创建空的 Geo Builder。
    pub fn new() -> Self {
        Self::default()
    }
    /// 设置纬度与经度。
    pub fn lat_lon(mut self, lat: f64, lon: f64) -> Self {
        self.o.insert("lat".into(), json!(lat));
        self.o.insert("lon".into(), json!(lon));
        self
    }
    /// 设置定位类型 `type`。
    pub fn type_(mut self, t: i32) -> Self {
        self.o.insert("type".into(), json!(t));
        self
    }
    /// 设置国家代码。
    pub fn country(mut self, c: &str) -> Self {
        self.o.insert("country".into(), json!(c));
        self
    }
    /// 设置城市。
    pub fn city(mut self, c: &str) -> Self {
        self.o.insert("city".into(), json!(c));
        self
    }
    /// 输出 Geo JSON 对象。
    pub fn build(self) -> Value {
        Value::Object(self.o)
    }
}

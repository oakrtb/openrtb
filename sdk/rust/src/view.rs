//! 轻量 BidRequest / BidResponse View 流水线（匹配 / 出价 / 响应决策）。
//!
//! 基于 OpenRTB JSON（[`serde_json::Value`]），**不**运行完整 JSON Schema——
//! 权威校验请用 [`crate::schema`]。
//!
//! 核心概念：
//! - [`MarkupMask`]：Imp 上存在的展示类型位掩码（对应 `Bid.mtype` 1–4），
//!   **不是** `Banner.format[]`（尺寸列表）。
//! - [`Inventory`]：BidRequest 级库存面（site/app/dooh 互斥），
//!   **不是** protobuf `Content.Channel` 或 OpenRTB `Content` 对象。

use std::time::{Duration, Instant};

use serde_json::Value;

/// Imp 上存在的展示类型位掩码（banner / video / audio / native）。
///
/// 与 OpenRTB `Bid.mtype` 一一对应：`BANNER=1`、`VIDEO=2`、`AUDIO=3`、`NATIVE=4`。
///
/// **注意**：本类型表示 Imp 上挂载的展示对象类型，与 `Banner.format[]`
///（允许的尺寸列表）是不同概念；后者仅描述 Banner 创意尺寸，不表示展示类型。
#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct MarkupMask(u8);

impl MarkupMask {
    /// 空掩码（无展示类型）。
    pub const NONE: MarkupMask = MarkupMask(0);
    /// Banner（`Bid.mtype = 1`）。
    pub const BANNER: MarkupMask = MarkupMask(1 << 0);
    /// Video（`Bid.mtype = 2`）。
    pub const VIDEO: MarkupMask = MarkupMask(1 << 1);
    /// Audio（`Bid.mtype = 3`）。
    pub const AUDIO: MarkupMask = MarkupMask(1 << 2);
    /// Native（`Bid.mtype = 4`）。
    pub const NATIVE: MarkupMask = MarkupMask(1 << 3);

    /// 返回底层位值。
    pub fn bits(self) -> u8 {
        self.0
    }

    /// 是否包含 `flag` 所表示的类型。
    pub fn has(self, flag: MarkupMask) -> bool {
        self.0 & flag.0 != 0
    }

    /// 是否含 Banner 位。
    pub fn has_banner(self) -> bool {
        self.has(Self::BANNER)
    }
    /// 是否含 Video 位。
    pub fn has_video(self) -> bool {
        self.has(Self::VIDEO)
    }
    /// 是否含 Audio 位。
    pub fn has_audio(self) -> bool {
        self.has(Self::AUDIO)
    }
    /// 是否含 Native 位。
    pub fn has_native(self) -> bool {
        self.has(Self::NATIVE)
    }

    /// 已置位的类型数量。
    pub fn count(self) -> u32 {
        self.0.count_ones()
    }

    /// 当且仅当恰好一种类型时返回该掩码，否则 [`Self::NONE`]。
    pub fn primary(self) -> MarkupMask {
        if self.count() == 1 {
            self
        } else {
            Self::NONE
        }
    }

    /// 推断 OpenRTB `Bid.mtype`；多格式或空 → 0。
    pub fn mtype(self) -> i32 {
        match self.primary() {
            Self::BANNER => 1,
            Self::VIDEO => 2,
            Self::AUDIO => 3,
            Self::NATIVE => 4,
            _ => 0,
        }
    }
}

impl std::ops::BitOr for MarkupMask {
    type Output = MarkupMask;
    fn bitor(self, rhs: MarkupMask) -> MarkupMask {
        MarkupMask(self.0 | rhs.0)
    }
}

impl std::ops::BitOrAssign for MarkupMask {
    fn bitor_assign(&mut self, rhs: MarkupMask) {
        self.0 |= rhs.0;
    }
}

/// BidRequest 库存面（site / app / dooh，互斥）。
///
/// 由顶层 `site`、`app`、`dooh` 字段推断，表示**库存载体**。
///
/// **注意**：与 protobuf 模型中的 `Content.Channel`、OpenRTB `Content` 对象
/// 或 `ContentBuilder` 无关；后者描述内容元数据，不是库存类型。
#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub enum Inventory {
    /// 未指定 site/app/dooh。
    #[default]
    None,
    /// Web 站点库存（`BidRequest.site`）。
    Site,
    /// 移动/App 库存（`BidRequest.app`）。
    App,
    /// 数字户外库存（`BidRequest.dooh`）。
    Dooh,
}

impl Inventory {
    /// 返回 `"site"` / `"app"` / `"dooh"` / `"none"`。
    pub fn as_str(self) -> &'static str {
        match self {
            Inventory::Site => "site",
            Inventory::App => "app",
            Inventory::Dooh => "dooh",
            Inventory::None => "none",
        }
    }
}

/// 拍卖级共享视图；`req` 为原始 JSON 对象引用。
#[derive(Clone, Debug)]
pub struct RequestSharedView<'a> {
    /// 原始 BidRequest JSON。
    pub req: &'a Value,
    /// 拍卖 id。
    pub id: &'a str,
    /// 拍卖类型 `at`。
    pub at: i64,
    /// 允许的货币列表。
    pub cur: &'a [Value],
    /// 最大处理时间（毫秒）。
    pub tmax: i64,
    /// 是否测试流量。
    pub test: i64,
    /// 库存类型（site/app/dooh）。
    pub inventory: Inventory,
    /// 当 `tmax > 0` 时：view 开始时刻 + 85% × tmax。
    pub deadline: Option<Instant>,
    /// `site` 对象（若存在）。
    pub site: Option<&'a Value>,
    /// `app` 对象（若存在）。
    pub app: Option<&'a Value>,
    /// `dooh` 对象（若存在）。
    pub dooh: Option<&'a Value>,
    /// `device` 对象（若存在）。
    pub device: Option<&'a Value>,
    /// `user` 对象（若存在）。
    pub user: Option<&'a Value>,
    /// `regs` 对象（若存在）。
    pub regs: Option<&'a Value>,
    /// `source` 对象（若存在）。
    pub source: Option<&'a Value>,
    /// 屏蔽类别 `bcat`。
    pub bcat: &'a [Value],
    /// 屏蔽广告主域名 `badv`。
    pub badv: &'a [Value],
    /// 屏蔽 app bundle `bapp`。
    pub bapp: &'a [Value],
}

impl RequestSharedView<'_> {
    /// 是否已超过 85% tmax 截止时间。
    pub fn past_deadline(&self) -> bool {
        self.deadline
            .map(|d| Instant::now() > d)
            .unwrap_or(false)
    }
}

/// 单条 Imp 视图（含 [`MarkupMask`]）。
#[derive(Clone, Debug)]
pub struct ImpView<'a> {
    /// 原始 Imp JSON。
    pub imp: &'a Value,
    /// Imp id。
    pub id: &'a str,
    /// 该 Imp 上的展示类型掩码。
    pub markup: MarkupMask,
    /// 广告位 tag id。
    pub tagid: Option<&'a str>,
    /// 底价。
    pub bidfloor: f64,
    /// 底价货币。
    pub bidfloorcur: Option<&'a str>,
    /// 是否插屏。
    pub instl: i64,
    /// 是否要求 HTTPS。
    pub secure: i64,
    /// 是否激励。
    pub rwdd: i64,
    /// SSAI 标志。
    pub ssai: i64,
    /// `banner` 子对象。
    pub banner: Option<&'a Value>,
    /// `video` 子对象。
    pub video: Option<&'a Value>,
    /// `audio` 子对象。
    pub audio: Option<&'a Value>,
    /// `native` 子对象。
    pub native: Option<&'a Value>,
    /// `pmp` 子对象。
    pub pmp: Option<&'a Value>,
}

/// 从 Imp JSON 推断 [`MarkupMask`]（检查 banner/video/audio/native 键是否存在且非 null）。
pub fn markup_mask(imp: &Value) -> MarkupMask {
    let Some(obj) = imp.as_object() else {
        return MarkupMask::NONE;
    };
    let mut f = MarkupMask::NONE;
    if obj.get("banner").map(|v| !v.is_null()).unwrap_or(false) {
        f |= MarkupMask::BANNER;
    }
    if obj.get("video").map(|v| !v.is_null()).unwrap_or(false) {
        f |= MarkupMask::VIDEO;
    }
    if obj.get("audio").map(|v| !v.is_null()).unwrap_or(false) {
        f |= MarkupMask::AUDIO;
    }
    if obj.get("native").map(|v| !v.is_null()).unwrap_or(false) {
        f |= MarkupMask::NATIVE;
    }
    f
}

/// 廉价结构检查（非完整 JSON Schema）；通过后再调用 [`request_shared`] 等。
pub fn light_gate_request(req: &Value) -> std::result::Result<(), String> {
    let obj = req
        .as_object()
        .ok_or_else(|| "view: BidRequest must be a JSON object".to_string())?;
    let id = obj
        .get("id")
        .and_then(|v| v.as_str())
        .map(str::trim)
        .filter(|s| !s.is_empty())
        .ok_or_else(|| "view: BidRequest.id is required".to_string())?;
    let _ = id;
    let at = obj.get("at").and_then(|v| v.as_i64()).unwrap_or(0);
    if at == 0 {
        return Err("view: BidRequest.at is required".into());
    }
    let cur = obj.get("cur").and_then(|v| v.as_array());
    if cur.map(|a| a.is_empty()).unwrap_or(true) {
        return Err("view: BidRequest.cur is required".into());
    }
    for (i, c) in cur.unwrap().iter().enumerate() {
        let s = c.as_str().unwrap_or("").trim();
        if s.is_empty() {
            return Err(format!("view: BidRequest.cur[{i}] is blank"));
        }
    }
    let imps = obj
        .get("imp")
        .and_then(|v| v.as_array())
        .ok_or_else(|| "view: BidRequest.imp requires at least one Imp".to_string())?;
    if imps.is_empty() {
        return Err("view: BidRequest.imp requires at least one Imp".into());
    }
    let n = [
        obj.get("site").is_some_and(|v| !v.is_null()),
        obj.get("app").is_some_and(|v| !v.is_null()),
        obj.get("dooh").is_some_and(|v| !v.is_null()),
    ]
    .into_iter()
    .filter(|&x| x)
    .count();
    if n > 1 {
        return Err("view: site/app/dooh are mutually exclusive".into());
    }
    for (i, imp) in imps.iter().enumerate() {
        let id = imp.get("id").and_then(|v| v.as_str()).unwrap_or("").trim();
        if id.is_empty() {
            return Err(format!("view: imp[{i}].id is required"));
        }
        if markup_mask(imp) == MarkupMask::NONE {
            return Err(format!(
                "view: imp[{i}] needs banner, video, audio, or native"
            ));
        }
    }
    Ok(())
}

/// LightGate 之后：构建拍卖级 [`RequestSharedView`]。
pub fn request_shared(req: &Value) -> RequestSharedView<'_> {
    let obj = req.as_object().expect("light_gate ensured object");
    let inventory = if obj.get("site").is_some_and(|v| !v.is_null()) {
        Inventory::Site
    } else if obj.get("app").is_some_and(|v| !v.is_null()) {
        Inventory::App
    } else if obj.get("dooh").is_some_and(|v| !v.is_null()) {
        Inventory::Dooh
    } else {
        Inventory::None
    };
    let tmax = obj.get("tmax").and_then(|v| v.as_i64()).unwrap_or(0);
    let deadline = if tmax > 0 {
        Some(Instant::now() + Duration::from_millis((tmax as u64) * 85 / 100))
    } else {
        None
    };
    let arr = |k: &str| -> &[Value] {
        obj.get(k)
            .and_then(|v| v.as_array())
            .map(|a| a.as_slice())
            .unwrap_or_else(empty_slice)
    };
    RequestSharedView {
        req,
        id: obj.get("id").and_then(|v| v.as_str()).unwrap_or(""),
        at: obj.get("at").and_then(|v| v.as_i64()).unwrap_or(0),
        cur: arr("cur"),
        tmax,
        test: obj.get("test").and_then(|v| v.as_i64()).unwrap_or(0),
        inventory,
        deadline,
        site: obj.get("site").filter(|v| !v.is_null()),
        app: obj.get("app").filter(|v| !v.is_null()),
        dooh: obj.get("dooh").filter(|v| !v.is_null()),
        device: obj.get("device").filter(|v| !v.is_null()),
        user: obj.get("user").filter(|v| !v.is_null()),
        regs: obj.get("regs").filter(|v| !v.is_null()),
        source: obj.get("source").filter(|v| !v.is_null()),
        bcat: arr("bcat"),
        badv: arr("badv"),
        bapp: arr("bapp"),
    }
}

fn empty_slice<'a>() -> &'a [Value] {
    &[]
}

/// [`request_shared`] 之后：构建各 Imp 的 [`ImpView`] 列表。
pub fn imps(req: &Value) -> std::result::Result<Vec<ImpView<'_>>, String> {
    let imps = req
        .get("imp")
        .and_then(|v| v.as_array())
        .ok_or_else(|| "view: missing imp".to_string())?;
    let mut out = Vec::with_capacity(imps.len());
    for imp in imps {
        let obj = imp.as_object().ok_or_else(|| "view: imp not object".to_string())?;
        out.push(ImpView {
            imp,
            id: obj.get("id").and_then(|v| v.as_str()).unwrap_or(""),
            markup: markup_mask(imp),
            tagid: obj.get("tagid").and_then(|v| v.as_str()),
            bidfloor: obj.get("bidfloor").and_then(|v| v.as_f64()).unwrap_or(0.0),
            bidfloorcur: obj.get("bidfloorcur").and_then(|v| v.as_str()),
            instl: obj.get("instl").and_then(|v| v.as_i64()).unwrap_or(0),
            secure: obj.get("secure").and_then(|v| v.as_i64()).unwrap_or(0),
            rwdd: obj.get("rwdd").and_then(|v| v.as_i64()).unwrap_or(0),
            ssai: obj.get("ssai").and_then(|v| v.as_i64()).unwrap_or(0),
            banner: obj.get("banner").filter(|v| !v.is_null()),
            video: obj.get("video").filter(|v| !v.is_null()),
            audio: obj.get("audio").filter(|v| !v.is_null()),
            native: obj.get("native").filter(|v| !v.is_null()),
            pmp: obj.get("pmp").filter(|v| !v.is_null()),
        });
    }
    Ok(out)
}

/// BidRequest View 流水线：LightGate → shared → imps。
///
/// 链式调用：[`RequestPipeline::of`] → 各 step → [`RequestPipeline::snapshot`]。
pub struct RequestPipeline<'a> {
    req: &'a Value,
    shared: Option<RequestSharedView<'a>>,
    imps: Vec<ImpView<'a>>,
    gated: bool,
    pinned: bool,
    viewed: bool,
    err: Option<String>,
}

impl<'a> RequestPipeline<'a> {
    /// 绑定 BidRequest JSON 引用。
    pub fn of(req: &'a Value) -> Self {
        Self {
            req,
            shared: None,
            imps: Vec::new(),
            gated: false,
            pinned: false,
            viewed: false,
            err: None,
        }
    }

    /// 步骤 1：结构 LightGate。
    pub fn light_gate(mut self) -> Self {
        if self.err.is_some() {
            return self;
        }
        if let Err(e) = light_gate_request(self.req) {
            self.err = Some(e);
            return self;
        }
        self.gated = true;
        self
    }

    /// 步骤 2：固定拍卖级共享视图。
    pub fn shared(mut self) -> Self {
        if self.err.is_some() {
            return self;
        }
        if self.pinned {
            return self;
        }
        if !self.gated {
            self.err = Some("pipeline: call light_gate first".into());
            return self;
        }
        self.shared = Some(request_shared(self.req));
        self.pinned = true;
        self
    }

    /// 步骤 3：构建 Imp 视图列表。
    pub fn imps(mut self) -> Self {
        if self.err.is_some() {
            return self;
        }
        if !self.pinned {
            self.err = Some("pipeline: call shared first".into());
            return self;
        }
        match imps(self.req) {
            Ok(imps) => {
                self.imps = imps;
                self.viewed = true;
            }
            Err(e) => self.err = Some(e),
        }
        self
    }

    /// 完成流水线；自动补跑尚未执行的步骤。
    pub fn snapshot(mut self) -> std::result::Result<RequestSnapshot<'a>, String> {
        if let Some(e) = self.err.take() {
            return Err(e);
        }
        if !self.gated {
            self = self.light_gate();
            if let Some(e) = self.err.take() {
                return Err(e);
            }
        }
        if !self.pinned {
            self = self.shared();
            if let Some(e) = self.err.take() {
                return Err(e);
            }
        }
        if !self.viewed {
            self = self.imps();
            if let Some(e) = self.err.take() {
                return Err(e);
            }
        }
        Ok(RequestSnapshot {
            shared: self.shared.expect("pinned"),
            imps: self.imps,
        })
    }
}

/// 一次性运行全部步骤，返回可查询的 [`RequestSnapshot`]。
pub fn run_request(req: &Value) -> std::result::Result<RequestSnapshot<'_>, String> {
    RequestPipeline::of(req)
        .light_gate()
        .shared()
        .imps()
        .snapshot()
}

/// 流水线完成后的可查询 BidRequest 快照。
#[derive(Clone, Debug)]
pub struct RequestSnapshot<'a> {
    /// 拍卖级共享视图。
    pub shared: RequestSharedView<'a>,
    /// 各 Imp 视图。
    pub imps: Vec<ImpView<'a>>,
}

impl<'a> RequestSnapshot<'a> {
    /// 拍卖 id。
    pub fn auction_id(&self) -> &str {
        self.shared.id
    }
    /// 拍卖类型 `at`。
    pub fn auction_type(&self) -> i64 {
        self.shared.at
    }
    /// 库存类型（[`Inventory`]，非 Content.Channel）。
    pub fn inventory(&self) -> Inventory {
        self.shared.inventory
    }
    /// 是否已超过 tmax 85% 截止时间。
    pub fn past_deadline(&self) -> bool {
        self.shared.past_deadline()
    }

    /// 按 id 查找 Imp。
    pub fn find_imp(&self, id: &str) -> Option<&ImpView<'a>> {
        self.imps.iter().find(|i| i.id == id)
    }

    /// 返回包含指定 [`MarkupMask`] 类型的 Imp 列表。
    pub fn imps_with(&self, flag: MarkupMask) -> Vec<&ImpView<'a>> {
        self.imps.iter().filter(|i| i.markup.has(flag)).collect()
    }

    /// 将各 Imp 压平为 [`ImpFact`] 便于下游消费。
    pub fn facts(&self) -> Vec<ImpFact<'a>> {
        self.imps.iter().map(ImpFact::from_view).collect()
    }
}

/// 单 Imp 的扁平事实结构。
#[derive(Clone, Debug)]
pub struct ImpFact<'a> {
    /// Imp id。
    pub id: &'a str,
    /// 展示类型掩码。
    pub markup: MarkupMask,
    /// 推断的 `Bid.mtype`（多格式时为 0）。
    pub mtype: i32,
    /// tag id。
    pub tagid: Option<&'a str>,
    /// 底价。
    pub bidfloor: f64,
    /// 底价货币。
    pub bidfloorcur: Option<&'a str>,
    /// secure 标志。
    pub secure: i64,
    /// 插屏标志。
    pub instl: i64,
    /// 激励标志。
    pub rwdd: i64,
    /// SSAI 标志。
    pub ssai: i64,
    /// Banner 宽（若有）。
    pub banner_w: Option<i64>,
    /// Banner 高（若有）。
    pub banner_h: Option<i64>,
    /// Native request 字符串（若有）。
    pub native_request: Option<&'a str>,
}

impl<'a> ImpFact<'a> {
    fn from_view(iv: &ImpView<'a>) -> Self {
        let banner_w = iv.banner.and_then(|b| b.get("w")).and_then(|v| v.as_i64());
        let banner_h = iv.banner.and_then(|b| b.get("h")).and_then(|v| v.as_i64());
        let native_request = iv
            .native
            .and_then(|n| n.get("request"))
            .and_then(|v| v.as_str())
            .filter(|s| !s.is_empty());
        Self {
            id: iv.id,
            markup: iv.markup,
            mtype: iv.markup.mtype(),
            tagid: iv.tagid,
            bidfloor: iv.bidfloor,
            bidfloorcur: iv.bidfloorcur,
            secure: iv.secure,
            instl: iv.instl,
            rwdd: iv.rwdd,
            ssai: iv.ssai,
            banner_w,
            banner_h,
            native_request,
        }
    }

    /// 是否含 Banner 类型。
    pub fn has_banner(&self) -> bool {
        self.markup.has_banner()
    }
    /// 是否含 Video 类型。
    pub fn has_video(&self) -> bool {
        self.markup.has_video()
    }
    /// 是否含 Audio 类型。
    pub fn has_audio(&self) -> bool {
        self.markup.has_audio()
    }
    /// 是否含 Native 类型。
    pub fn has_native(&self) -> bool {
        self.markup.has_native()
    }
}

// --- BidResponse view / pipeline ----------------------------------------

/// BidResponse 拍卖级共享视图。
#[derive(Clone, Debug)]
pub struct ResponseSharedView<'a> {
    /// 原始 BidResponse JSON。
    pub res: &'a Value,
    /// 回显的 BidRequest id。
    pub id: &'a str,
    /// DSP bid id（可选）。
    pub bidid: Option<&'a str>,
    /// 响应货币。
    pub cur: &'a str,
    /// No-Bid 原因码（若有）。
    pub nbr: i64,
    /// 自定义数据（可选）。
    pub customdata: Option<&'a str>,
    /// 是否无出价（空 seatbid 或缺失）。
    pub no_bid: bool,
}

/// 单个 SeatBid 及其嵌套 Bid 列表。
#[derive(Clone, Debug)]
pub struct SeatBidView<'a> {
    /// 原始 SeatBid JSON。
    pub seatbid: &'a Value,
    /// 买方 seat id。
    pub seat: Option<&'a str>,
    /// group 标志。
    pub group: i64,
    /// 该 seat 下的 Bid 视图。
    pub bids: Vec<BidView<'a>>,
}

/// 单条 Bid 视图（含由 mtype 推断的 [`MarkupMask`]）。
#[derive(Clone, Debug)]
pub struct BidView<'a> {
    /// 原始 Bid JSON。
    pub bid: &'a Value,
    /// Bid id。
    pub id: &'a str,
    /// 目标 Imp id。
    pub impid: &'a str,
    /// 所属 seat（来自父 SeatBid）。
    pub seat: Option<&'a str>,
    /// 出价价格。
    pub price: f64,
    /// 由 mtype 推断的展示类型。
    pub markup: MarkupMask,
    /// OpenRTB mtype 原值。
    pub mtype: i64,
    /// 创意 id。
    pub crid: Option<&'a str>,
    /// 活动 id。
    pub cid: Option<&'a str>,
    /// Deal id。
    pub dealid: Option<&'a str>,
    /// 创意宽。
    pub w: i64,
    /// 创意高。
    pub h: i64,
    /// 时长（秒）。
    pub dur: i64,
    /// adm markup。
    pub adm: Option<&'a str>,
    /// 胜出通知 URL。
    pub nurl: Option<&'a str>,
    /// 计费通知 URL。
    pub burl: Option<&'a str>,
    /// 败标通知 URL。
    pub lurl: Option<&'a str>,
    /// 广告主域名列表。
    pub adomain: &'a [Value],
}

/// 将 OpenRTB `Bid.mtype` 转为 [`MarkupMask`]；未知值 → [`MarkupMask::NONE`]。
pub fn markup_from_mtype(mtype: i64) -> MarkupMask {
    match mtype {
        1 => MarkupMask::BANNER,
        2 => MarkupMask::VIDEO,
        3 => MarkupMask::AUDIO,
        4 => MarkupMask::NATIVE,
        _ => MarkupMask::NONE,
    }
}

/// BidResponse 结构 LightGate（id/cur 必填；有 seatbid 时校验 bid 字段）。
pub fn light_gate_response(res: &Value) -> std::result::Result<(), String> {
    let obj = res
        .as_object()
        .ok_or_else(|| "view: BidResponse must be a JSON object".to_string())?;
    let id = obj.get("id").and_then(|v| v.as_str()).unwrap_or("").trim();
    if id.is_empty() {
        return Err("view: BidResponse.id is required".into());
    }
    let cur = obj.get("cur").and_then(|v| v.as_str()).unwrap_or("").trim();
    if cur.is_empty() {
        return Err("view: BidResponse.cur is required".into());
    }
    let seatbid_val = match obj.get("seatbid") {
        None => return Ok(()),
        Some(v) if v.is_null() => return Ok(()),
        Some(v) => v,
    };
    let Some(seatbids) = seatbid_val.as_array() else {
        return Err("view: BidResponse.seatbid must be an array".into());
    };
    if seatbids.is_empty() {
        return Ok(());
    }
    for (i, sb) in seatbids.iter().enumerate() {
        let bids = sb
            .get("bid")
            .and_then(|v| v.as_array())
            .ok_or_else(|| format!("view: seatbid[{i}] needs at least one bid"))?;
        if bids.is_empty() {
            return Err(format!("view: seatbid[{i}] needs at least one bid"));
        }
        for (j, bid) in bids.iter().enumerate() {
            let id = bid.get("id").and_then(|v| v.as_str()).unwrap_or("").trim();
            let impid = bid.get("impid").and_then(|v| v.as_str()).unwrap_or("").trim();
            if id.is_empty() || impid.is_empty() {
                return Err(format!(
                    "view: seatbid[{i}].bid[{j}] requires id and impid"
                ));
            }
            let price = bid.get("price").and_then(|v| v.as_f64()).unwrap_or(0.0);
            if price <= 0.0 {
                return Err(format!(
                    "view: seatbid[{i}].bid[{j}].price must be > 0"
                ));
            }
        }
    }
    Ok(())
}

/// LightGate 之后：构建 [`ResponseSharedView`]。
pub fn response_shared(res: &Value) -> ResponseSharedView<'_> {
    let obj = res.as_object().expect("light_gate ensured object");
    let seatbids = obj.get("seatbid").and_then(|v| v.as_array());
    ResponseSharedView {
        res,
        id: obj.get("id").and_then(|v| v.as_str()).unwrap_or(""),
        bidid: obj.get("bidid").and_then(|v| v.as_str()),
        cur: obj.get("cur").and_then(|v| v.as_str()).unwrap_or(""),
        nbr: obj.get("nbr").and_then(|v| v.as_i64()).unwrap_or(0),
        customdata: obj.get("customdata").and_then(|v| v.as_str()),
        no_bid: seatbids.map(|a| a.is_empty()).unwrap_or(true),
    }
}

/// 构建 SeatBid 视图列表及扁平 Bid 列表。
pub fn view_seatbids(
    res: &Value,
) -> std::result::Result<(Vec<SeatBidView<'_>>, Vec<BidView<'_>>), String> {
    let empty: &[Value] = &[];
    let seatbids = res
        .get("seatbid")
        .and_then(|v| v.as_array())
        .map(|a| a.as_slice())
        .unwrap_or(empty);
    let mut seats = Vec::with_capacity(seatbids.len());
    let mut flat = Vec::new();
    for sb in seatbids {
        let seat = sb.get("seat").and_then(|v| v.as_str());
        let group = sb.get("group").and_then(|v| v.as_i64()).unwrap_or(0);
        let bids_arr = sb
            .get("bid")
            .and_then(|v| v.as_array())
            .map(|a| a.as_slice())
            .unwrap_or(empty);
        let mut bids = Vec::with_capacity(bids_arr.len());
        for bid in bids_arr {
            let bv = view_bid(bid, seat)?;
            bids.push(bv.clone());
            flat.push(bv);
        }
        seats.push(SeatBidView {
            seatbid: sb,
            seat,
            group,
            bids,
        });
    }
    Ok((seats, flat))
}

fn view_bid<'a>(bid: &'a Value, seat: Option<&'a str>) -> std::result::Result<BidView<'a>, String> {
    let obj = bid
        .as_object()
        .ok_or_else(|| "view: bid not object".to_string())?;
    let mtype = obj.get("mtype").and_then(|v| v.as_i64()).unwrap_or(0);
    let empty: &[Value] = &[];
    Ok(BidView {
        bid,
        id: obj.get("id").and_then(|v| v.as_str()).unwrap_or(""),
        impid: obj.get("impid").and_then(|v| v.as_str()).unwrap_or(""),
        seat,
        price: obj.get("price").and_then(|v| v.as_f64()).unwrap_or(0.0),
        markup: markup_from_mtype(mtype),
        mtype,
        crid: obj.get("crid").and_then(|v| v.as_str()),
        cid: obj.get("cid").and_then(|v| v.as_str()),
        dealid: obj.get("dealid").and_then(|v| v.as_str()),
        w: obj.get("w").and_then(|v| v.as_i64()).unwrap_or(0),
        h: obj.get("h").and_then(|v| v.as_i64()).unwrap_or(0),
        dur: obj.get("dur").and_then(|v| v.as_i64()).unwrap_or(0),
        adm: obj.get("adm").and_then(|v| v.as_str()),
        nurl: obj.get("nurl").and_then(|v| v.as_str()),
        burl: obj.get("burl").and_then(|v| v.as_str()),
        lurl: obj.get("lurl").and_then(|v| v.as_str()),
        adomain: obj
            .get("adomain")
            .and_then(|v| v.as_array())
            .map(|a| a.as_slice())
            .unwrap_or(empty),
    })
}

/// BidResponse View 流水线：light_gate → shared → bids。
pub struct ResponsePipeline<'a> {
    res: &'a Value,
    shared: Option<ResponseSharedView<'a>>,
    seatbids: Vec<SeatBidView<'a>>,
    bids: Vec<BidView<'a>>,
    gated: bool,
    pinned: bool,
    viewed: bool,
    err: Option<String>,
}

impl<'a> ResponsePipeline<'a> {
    /// 绑定 BidResponse JSON 引用。
    pub fn of(res: &'a Value) -> Self {
        Self {
            res,
            shared: None,
            seatbids: Vec::new(),
            bids: Vec::new(),
            gated: false,
            pinned: false,
            viewed: false,
            err: None,
        }
    }

    /// 步骤 1：结构 LightGate。
    pub fn light_gate(mut self) -> Self {
        if self.err.is_some() {
            return self;
        }
        if let Err(e) = light_gate_response(self.res) {
            self.err = Some(e);
            return self;
        }
        self.gated = true;
        self
    }

    /// 步骤 2：固定响应级共享视图。
    pub fn shared(mut self) -> Self {
        if self.err.is_some() {
            return self;
        }
        if self.pinned {
            return self;
        }
        if !self.gated {
            self.err = Some("pipeline: call light_gate first".into());
            return self;
        }
        self.shared = Some(response_shared(self.res));
        self.pinned = true;
        self
    }

    /// 步骤 3：构建 SeatBid / Bid 视图。
    pub fn bids(mut self) -> Self {
        if self.err.is_some() {
            return self;
        }
        if !self.pinned {
            self.err = Some("pipeline: call shared first".into());
            return self;
        }
        match view_seatbids(self.res) {
            Ok((seats, bids)) => {
                self.seatbids = seats;
                self.bids = bids;
                self.viewed = true;
            }
            Err(e) => self.err = Some(e),
        }
        self
    }

    /// 完成流水线；自动补跑尚未执行的步骤。
    pub fn snapshot(mut self) -> std::result::Result<ResponseSnapshot<'a>, String> {
        if let Some(e) = self.err.take() {
            return Err(e);
        }
        if !self.gated {
            self = self.light_gate();
            if let Some(e) = self.err.take() {
                return Err(e);
            }
        }
        if !self.pinned {
            self = self.shared();
            if let Some(e) = self.err.take() {
                return Err(e);
            }
        }
        if !self.viewed {
            self = self.bids();
            if let Some(e) = self.err.take() {
                return Err(e);
            }
        }
        Ok(ResponseSnapshot {
            shared: self.shared.expect("pinned"),
            seatbids: self.seatbids,
            bids: self.bids,
        })
    }
}

/// 一次性运行 BidResponse 全部 view 步骤。
pub fn run_response(res: &Value) -> std::result::Result<ResponseSnapshot<'_>, String> {
    ResponsePipeline::of(res)
        .light_gate()
        .shared()
        .bids()
        .snapshot()
}

/// 流水线完成后的可查询 BidResponse 快照。
#[derive(Clone, Debug)]
pub struct ResponseSnapshot<'a> {
    /// 响应级共享视图。
    pub shared: ResponseSharedView<'a>,
    /// 各 SeatBid 视图。
    pub seatbids: Vec<SeatBidView<'a>>,
    /// 扁平化的全部 Bid。
    pub bids: Vec<BidView<'a>>,
}

impl<'a> ResponseSnapshot<'a> {
    /// 回显的 BidRequest id。
    pub fn request_id(&self) -> &str {
        self.shared.id
    }
    /// 响应货币。
    pub fn currency(&self) -> &str {
        self.shared.cur
    }
    /// 是否无出价。
    pub fn no_bid(&self) -> bool {
        self.shared.no_bid
    }
    /// No-Bid 原因码。
    pub fn nbr(&self) -> i64 {
        self.shared.nbr
    }

    /// 按 Bid id 查找。
    pub fn find_bid(&self, id: &str) -> Option<&BidView<'a>> {
        self.bids.iter().find(|b| b.id == id)
    }

    /// 返回 targeting 指定 Imp 的全部 Bid。
    pub fn bids_for_imp(&self, impid: &str) -> Vec<&BidView<'a>> {
        self.bids.iter().filter(|b| b.impid == impid).collect()
    }

    /// 返回包含指定 [`MarkupMask`] 类型的 Bid。
    pub fn bids_with(&self, flag: MarkupMask) -> Vec<&BidView<'a>> {
        self.bids.iter().filter(|b| b.markup.has(flag)).collect()
    }

    /// 将各 Bid 压平为 [`BidFact`]。
    pub fn facts(&self) -> Vec<BidFact<'a>> {
        self.bids.iter().map(BidFact::from_view).collect()
    }
}

/// 单 Bid 的扁平事实结构。
#[derive(Clone, Debug)]
pub struct BidFact<'a> {
    /// Bid id。
    pub id: &'a str,
    /// 目标 Imp id。
    pub impid: &'a str,
    /// 所属 seat。
    pub seat: Option<&'a str>,
    /// 出价价格。
    pub price: f64,
    /// 展示类型掩码。
    pub markup: MarkupMask,
    /// OpenRTB mtype。
    pub mtype: i64,
    /// 创意 id。
    pub crid: Option<&'a str>,
    /// Deal id。
    pub dealid: Option<&'a str>,
    /// 创意宽。
    pub w: i64,
    /// 创意高。
    pub h: i64,
    /// 时长。
    pub dur: i64,
    /// 是否有非空 adm。
    pub has_adm: bool,
    /// 广告主域名。
    pub adomain: &'a [Value],
}

impl<'a> BidFact<'a> {
    fn from_view(b: &BidView<'a>) -> Self {
        Self {
            id: b.id,
            impid: b.impid,
            seat: b.seat,
            price: b.price,
            markup: b.markup,
            mtype: b.mtype,
            crid: b.crid,
            dealid: b.dealid,
            w: b.w,
            h: b.h,
            dur: b.dur,
            has_adm: b.adm.map(|s| !s.is_empty()).unwrap_or(false),
            adomain: b.adomain,
        }
    }

    /// 是否 Banner 类型。
    pub fn has_banner(&self) -> bool {
        self.markup.has_banner()
    }
    /// 是否 Video 类型。
    pub fn has_video(&self) -> bool {
        self.markup.has_video()
    }
    /// 是否 Audio 类型。
    pub fn has_audio(&self) -> bool {
        self.markup.has_audio()
    }
    /// 是否 Native 类型。
    pub fn has_native(&self) -> bool {
        self.markup.has_native()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::build::{
        BannerImpBuilder, BidBuilder, BidRequestBuilder, BidResponseBuilder, DeviceBuilder,
        SiteBuilder,
    };

    #[test]
    fn view_banner_request() {
        let req = BidRequestBuilder::new("auction-1")
            .first_price()
            .tmax(120)
            .currency(&["USD"])
            .site(
                SiteBuilder::new()
                    .id("s1")
                    .domain("example.com")
                    .page("https://example.com/a")
                    .build(),
            )
            .device(
                DeviceBuilder::new()
                    .ua("Mozilla/5.0")
                    .ip("192.0.2.1")
                    .device_type(4)
                    .build(),
            )
            .add_imp(
                BannerImpBuilder::new("1")
                    .size(300, 250)
                    .floor(0.03, "USD")
                    .secure()
                    .build(),
            )
            .build()
            .expect("build");

        let res = run_request(&req).expect("view");
        assert_eq!(res.shared.id, "auction-1");
        assert_eq!(res.shared.at, 1);
        assert_eq!(res.shared.inventory, Inventory::Site);
        assert!(res.shared.deadline.is_some());
        assert!(res.shared.site.is_some());
        assert!(res.shared.device.is_some());
        assert_eq!(res.imps.len(), 1);
        let iv = &res.imps[0];
        assert!(iv.markup.has_banner());
        assert_eq!(iv.markup.count(), 1);
        assert_eq!(iv.markup.mtype(), 1);
        assert_eq!(iv.secure, 1);
        assert!((iv.bidfloor - 0.03).abs() < 1e-9);
        assert_eq!(iv.banner.unwrap()["w"], 300);
    }

    #[test]
    fn multi_format_and_mutex() {
        let mut req = serde_json::json!({
            "id": "m",
            "at": 2,
            "cur": ["USD"],
            "app": {"id": "a1", "bundle": "com.example.app"},
            "imp": [{
                "id": "1",
                "banner": {"w": 320, "h": 50},
                "video": {"mimes": ["video/mp4"]}
            }]
        });
        let res = run_request(&req).expect("view");
        assert_eq!(res.shared.inventory, Inventory::App);
        let f = res.imps[0].markup;
        assert!(f.has_banner() && f.has_video());
        assert_eq!(f.count(), 2);
        assert_eq!(f.mtype(), 0);

        req["site"] = serde_json::json!({"id": "s"});
        assert!(light_gate_request(&req).is_err());
    }

    #[test]
    fn pipeline_run_facts() {
        let req = BidRequestBuilder::new("auction-1")
            .first_price()
            .tmax(120)
            .currency(&["USD"])
            .site(
                SiteBuilder::new()
                    .id("s1")
                    .domain("example.com")
                    .page("https://example.com/a")
                    .build(),
            )
            .add_imp(
                BannerImpBuilder::new("1")
                    .size(300, 250)
                    .floor(0.03, "USD")
                    .secure()
                    .build(),
            )
            .build()
            .expect("build");

        let snap = run_request(&req).expect("run");
        assert_eq!(snap.auction_id(), "auction-1");
        assert_eq!(snap.inventory(), Inventory::Site);
        assert!(snap.find_imp("1").is_some());
        assert_eq!(snap.imps_with(MarkupMask::BANNER).len(), 1);
        let facts = snap.facts();
        assert_eq!(facts.len(), 1);
        assert!(facts[0].has_banner());
        assert_eq!(facts[0].mtype, 1);
        assert_eq!(facts[0].banner_w, Some(300));
        assert!((facts[0].bidfloor - 0.03).abs() < 1e-9);
    }

    #[test]
    fn response_pipeline_run_facts() {
        let res = BidResponseBuilder::new("auction-1")
            .currency("USD")
            .add_seat_bid(
                "512",
                vec![BidBuilder::new("1", "1", 1.23)
                    .banner()
                    .size(300, 250)
                    .adm("<img/>")
                    .crid("c1")
                    .adomain(&["adv.com"])
                    .build()],
            )
            .build()
            .expect("build");

        let snap = run_response(&res).expect("run_response");
        assert_eq!(snap.request_id(), "auction-1");
        assert!(!snap.no_bid());
        assert_eq!(snap.currency(), "USD");
        assert!(snap.find_bid("1").is_some());
        assert_eq!(snap.bids_for_imp("1").len(), 1);
        assert_eq!(snap.bids_with(MarkupMask::BANNER).len(), 1);
        let facts = snap.facts();
        assert_eq!(facts.len(), 1);
        assert!(facts[0].has_banner());
        assert_eq!(facts[0].mtype, 1);
        assert_eq!(facts[0].w, 300);
        assert!(facts[0].has_adm);
        assert!((facts[0].price - 1.23).abs() < 1e-9);
    }

    #[test]
    fn response_pipeline_no_bid() {
        let res = BidResponseBuilder::new("auction-1")
            .no_bid(2)
            .build()
            .expect("build");
        let snap = run_response(&res).expect("run");
        assert!(snap.no_bid());
        assert_eq!(snap.nbr(), 2);
        assert!(snap.bids.is_empty());
    }

    #[test]
    fn markup_mask_bits_and_mtype() {
        let m = MarkupMask::BANNER | MarkupMask::VIDEO;
        assert_eq!(m.count(), 2);
        assert!(m.has(MarkupMask::BANNER));
        assert_eq!(m.mtype(), 0);
        assert_eq!(MarkupMask::VIDEO.mtype(), 2);
        assert_eq!(markup_from_mtype(3), MarkupMask::AUDIO);
        assert_eq!(markup_from_mtype(99), MarkupMask::NONE);
    }

    #[test]
    fn inventory_dooh_and_as_str() {
        let req = serde_json::json!({
            "id": "d", "at": 1, "cur": ["USD"],
            "dooh": {"id": "screen-1"},
            "imp": [{"id": "1", "banner": {"w": 1, "h": 1}}]
        });
        let snap = run_request(&req).unwrap();
        assert_eq!(snap.inventory(), Inventory::Dooh);
        assert_eq!(Inventory::Dooh.as_str(), "dooh");
        assert_eq!(Inventory::None.as_str(), "none");
    }

    #[test]
    fn markup_mask_not_banner_format_array() {
        let imp = serde_json::json!({
            "id": "1",
            "banner": {"format": [{"w": 300, "h": 250}]}
        });
        let mask = markup_mask(&imp);
        assert!(mask.has_banner());
        assert_eq!(mask.count(), 1);
        let formats = imp["banner"]["format"].as_array().unwrap();
        assert_eq!(formats.len(), 1);
    }

    #[test]
    fn request_pipeline_step_order_error() {
        let req = serde_json::json!({
            "id": "x", "at": 1, "cur": ["USD"],
            "imp": [{"id": "1", "banner": {"w": 1, "h": 1}}]
        });
        let err = RequestPipeline::of(&req).shared().snapshot().unwrap_err();
        assert!(err.contains("light_gate"));
    }

    #[test]
    fn response_pipeline_step_order_error() {
        let res = serde_json::json!({"id": "r", "cur": "USD"});
        let err = ResponsePipeline::of(&res).shared().snapshot().unwrap_err();
        assert!(err.contains("light_gate"));
    }

    #[test]
    fn light_gate_response_missing_cur() {
        let res = serde_json::json!({"id": "r"});
        assert!(light_gate_response(&res).is_err());
    }

    #[test]
    fn light_gate_rejects_blank() {
        let mut req = serde_json::json!({
            "id": "x", "at": 1, "cur": ["USD"],
            "imp": [{"id": "1", "banner": {"w": 1, "h": 1}}]
        });
        req["id"] = serde_json::json!("   ");
        assert!(light_gate_request(&req).is_err());
        req["id"] = serde_json::json!("x");
        req["cur"] = serde_json::json!(["   "]);
        assert!(light_gate_request(&req).is_err());
        req["cur"] = serde_json::json!(["USD"]);
        req["imp"][0]["id"] = serde_json::json!("	");
        assert!(light_gate_request(&req).is_err());
    }

    #[test]
    fn light_gate_response_seatbid_non_array() {
        let res = serde_json::json!({"id": "r", "cur": "USD", "seatbid": {}});
        assert!(light_gate_response(&res).is_err());
    }

    #[test]
    fn shared_pin_once() {
        let req = serde_json::json!({
            "id": "x", "at": 1, "tmax": 200, "cur": ["USD"],
            "imp": [{"id": "1", "banner": {"w": 1, "h": 1}}]
        });
        let mut p = RequestPipeline::of(&req).light_gate().shared();
        let d1 = p.shared.as_ref().unwrap().deadline;
        std::thread::sleep(std::time::Duration::from_millis(3));
        p = p.shared();
        let d2 = p.shared.as_ref().unwrap().deadline;
        assert_eq!(d1, d2);
    }
}

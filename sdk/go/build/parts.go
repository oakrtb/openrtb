package build

import (
	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
)

// --- Imp factories -----------------------------------------------------------

// NewBannerImp 创建展示类 Imp（设置 Imp.banner）。
func NewBannerImp(id string) *ImpBuilder {
	return &ImpBuilder{imp: &openrtb.Imp{Id: id, Banner: &openrtb.Banner{}}}
}

// NewVideoImp 创建视频类 Imp（设置 Imp.video）。
func NewVideoImp(id string) *ImpBuilder {
	return &ImpBuilder{imp: &openrtb.Imp{Id: id, Video: &openrtb.Video{}}}
}

// NewAudioImp 创建音频类 Imp（设置 Imp.audio）。
func NewAudioImp(id string) *ImpBuilder {
	return &ImpBuilder{imp: &openrtb.Imp{Id: id, Audio: &openrtb.Audio{}}}
}

// NewNativeImp 创建原生类 Imp（设置 Imp.native）。
func NewNativeImp(id string) *ImpBuilder {
	return &ImpBuilder{imp: &openrtb.Imp{Id: id, Native: &openrtb.Native{Ver: "1.2"}}}
}

// ImpBuilder 配置单个 Imp 及其格式对象。
type ImpBuilder struct {
	imp *openrtb.Imp
}

// Floor 设置 CPM 底价及货币。
func (b *ImpBuilder) Floor(bidfloor float64, cur string) *ImpBuilder {
	b.imp.Bidfloor = bidfloor
	b.imp.Bidfloorcur = cur
	return b
}

// Secure 要求 HTTPS 创意（secure=1）。
func (b *ImpBuilder) Secure() *ImpBuilder {
	b.imp.Secure = 1
	return b
}

// TagID 设置发布商广告位 id。
func (b *ImpBuilder) TagID(tagid string) *ImpBuilder {
	b.imp.Tagid = tagid
	return b
}

// Interstitial 标记插屏（instl=1）。
func (b *ImpBuilder) Interstitial() *ImpBuilder {
	b.imp.Instl = 1
	return b
}

// Rewarded 标记激励库存（rwdd=1）。
func (b *ImpBuilder) Rewarded() *ImpBuilder {
	b.imp.Rwdd = 1
	return b
}

// Size 设置 Banner 或 Video 播放器宽高。
func (b *ImpBuilder) Size(w, h int32) *ImpBuilder {
	if b.imp.Banner != nil {
		b.imp.Banner.W = w
		b.imp.Banner.H = h
	}
	if b.imp.Video != nil {
		b.imp.Video.W = w
		b.imp.Video.H = h
	}
	return b
}

// BannerPos 设置 Banner.pos（AdPosition）。
func (b *ImpBuilder) BannerPos(pos int32) *ImpBuilder {
	if b.imp.Banner != nil {
		b.imp.Banner.Pos = pos
	}
	return b
}

// BannerMimes 设置 Banner.mimes。
func (b *ImpBuilder) BannerMimes(mimes ...string) *ImpBuilder {
	if b.imp.Banner != nil {
		b.imp.Banner.Mimes = append([]string{}, mimes...)
	}
	return b
}

// VideoMimes 设置必填的 Video.mimes。
func (b *ImpBuilder) VideoMimes(mimes ...string) *ImpBuilder {
	if b.imp.Video != nil {
		b.imp.Video.Mimes = append([]string{}, mimes...)
	}
	return b
}

// VideoDuration 设置最小时长/最大时长（秒）。
func (b *ImpBuilder) VideoDuration(min, max int32) *ImpBuilder {
	if b.imp.Video != nil {
		b.imp.Video.Minduration = min
		b.imp.Video.Maxduration = max
	}
	return b
}

// VideoProtocols 设置 Video.protocols（VAST/DAAST Protocol 枚举整型）。
func (b *ImpBuilder) VideoProtocols(protocols ...int32) *ImpBuilder {
	if b.imp.Video != nil {
		b.imp.Video.Protocols = append([]int32{}, protocols...)
	}
	return b
}

// StartDelay 设置 Video.startdelay（0 前贴 / -1 中贴 / -2 后贴 / >0 中贴延迟）。
func (b *ImpBuilder) StartDelay(v int32) *ImpBuilder {
	if b.imp.Video != nil {
		b.imp.Video.Startdelay = v
	}
	return b
}

// Plcmt 设置 Video.plcmt（VideoPlcmt）。
func (b *ImpBuilder) Plcmt(plcmt int32) *ImpBuilder {
	if b.imp.Video != nil {
		b.imp.Video.Plcmt = plcmt
	}
	return b
}

// Linearity 设置 Video.linearity。
func (b *ImpBuilder) Linearity(v int32) *ImpBuilder {
	if b.imp.Video != nil {
		b.imp.Video.Linearity = v
	}
	return b
}

// Skip 设置 Video.skip 及可选 skipafter 秒数。
func (b *ImpBuilder) Skip(skipafter int32) *ImpBuilder {
	if b.imp.Video != nil {
		b.imp.Video.Skip = 1
		b.imp.Video.Skipafter = skipafter
	}
	return b
}

// Pod 将视频/音频放入广告组（podid + slotinpod）。
func (b *ImpBuilder) Pod(podid string, slotinpod int32) *ImpBuilder {
	if b.imp.Video != nil {
		b.imp.Video.Podid = podid
		b.imp.Video.Slotinpod = slotinpod
	}
	if b.imp.Audio != nil {
		b.imp.Audio.Podid = podid
		b.imp.Audio.Slotinpod = slotinpod
	}
	return b
}

// PlaybackMethod 设置 Video.playbackmethod。
func (b *ImpBuilder) PlaybackMethod(methods ...int32) *ImpBuilder {
	if b.imp.Video != nil {
		b.imp.Video.Playbackmethod = append([]int32{}, methods...)
	}
	return b
}

// AudioMimes 设置必填的 Audio.mimes。
func (b *ImpBuilder) AudioMimes(mimes ...string) *ImpBuilder {
	if b.imp.Audio != nil {
		b.imp.Audio.Mimes = append([]string{}, mimes...)
	}
	return b
}

// AudioDuration 设置 Audio 最小时长/最大时长。
func (b *ImpBuilder) AudioDuration(min, max int32) *ImpBuilder {
	if b.imp.Audio != nil {
		b.imp.Audio.Minduration = min
		b.imp.Audio.Maxduration = max
	}
	return b
}

// AudioProtocols 设置 Audio.protocols。
func (b *ImpBuilder) AudioProtocols(protocols ...int32) *ImpBuilder {
	if b.imp.Audio != nil {
		b.imp.Audio.Protocols = append([]int32{}, protocols...)
	}
	return b
}

// Feed 设置 Audio.feed（FeedType）。
func (b *ImpBuilder) Feed(feed int32) *ImpBuilder {
	if b.imp.Audio != nil {
		b.imp.Audio.Feed = feed
	}
	return b
}

// NativeRequest 设置 Native.request JSON 字符串（Native 1.2 markup 请求）。
func (b *ImpBuilder) NativeRequest(requestJSON string) *ImpBuilder {
	if b.imp.Native != nil {
		b.imp.Native.Request = requestJSON
	}
	return b
}

// NativeVer 设置 Native.ver（默认 "1.2"）。
func (b *ImpBuilder) NativeVer(ver string) *ImpBuilder {
	if b.imp.Native != nil {
		b.imp.Native.Ver = ver
	}
	return b
}

// Build 返回配置完成的 Imp。
func (b *ImpBuilder) Build() *openrtb.Imp {
	return b.imp
}

// --- Inventory / device helpers ---------------------------------------------

// NewSite 创建 Site 构建器。
func NewSite() *SiteBuilder { return &SiteBuilder{s: &openrtb.Site{}} }

// SiteBuilder 配置 Site 对象。
type SiteBuilder struct{ s *openrtb.Site }

// ID 设置 Site.id。
func (b *SiteBuilder) ID(id string) *SiteBuilder { b.s.Id = id; return b }

// Name 设置 Site.name。
func (b *SiteBuilder) Name(name string) *SiteBuilder { b.s.Name = name; return b }

// Domain 设置 Site.domain。
func (b *SiteBuilder) Domain(domain string) *SiteBuilder { b.s.Domain = domain; return b }

// Page 设置 Site.page。
func (b *SiteBuilder) Page(page string) *SiteBuilder { b.s.Page = page; return b }

// Cat 设置 Site.cat 类别列表。
func (b *SiteBuilder) Cat(cats ...string) *SiteBuilder { b.s.Cat = append([]string{}, cats...); return b }

// Publisher 设置 Site.publisher。
func (b *SiteBuilder) Publisher(p *openrtb.Publisher) *SiteBuilder {
	b.s.Publisher = p
	return b
}

// Build 返回配置完成的 Site。
func (b *SiteBuilder) Build() *openrtb.Site { return b.s }

// NewApp 创建 App 构建器。
func NewApp() *AppBuilder { return &AppBuilder{a: &openrtb.App{}} }

// AppBuilder 配置 App 对象。
type AppBuilder struct{ a *openrtb.App }

// ID 设置 App.id。
func (b *AppBuilder) ID(id string) *AppBuilder { b.a.Id = id; return b }

// Name 设置 App.name。
func (b *AppBuilder) Name(name string) *AppBuilder { b.a.Name = name; return b }

// Bundle 设置 App.bundle。
func (b *AppBuilder) Bundle(bundle string) *AppBuilder { b.a.Bundle = bundle; return b }

// Domain 设置 App.domain。
func (b *AppBuilder) Domain(domain string) *AppBuilder { b.a.Domain = domain; return b }

// StoreURL 设置 App.storeurl。
func (b *AppBuilder) StoreURL(url string) *AppBuilder { b.a.Storeurl = url; return b }

// Publisher 设置 App.publisher。
func (b *AppBuilder) Publisher(p *openrtb.Publisher) *AppBuilder {
	b.a.Publisher = p
	return b
}

// Content 设置 App.content。
func (b *AppBuilder) Content(c *openrtb.Content) *AppBuilder {
	b.a.Content = c
	return b
}

// Build 返回配置完成的 App。
func (b *AppBuilder) Build() *openrtb.App { return b.a }

// NewDooh 创建 Dooh 构建器。
func NewDooh() *DoohBuilder { return &DoohBuilder{d: &openrtb.Dooh{}} }

// DoohBuilder 配置 Dooh 对象。
type DoohBuilder struct{ d *openrtb.Dooh }

// ID 设置 Dooh.id。
func (b *DoohBuilder) ID(id string) *DoohBuilder { b.d.Id = id; return b }

// Name 设置 Dooh.name。
func (b *DoohBuilder) Name(name string) *DoohBuilder { b.d.Name = name; return b }

// VenueType 设置 Dooh.venuetype。
func (b *DoohBuilder) VenueType(ids ...string) *DoohBuilder {
	b.d.Venuetype = append([]string{}, ids...)
	return b
}

// VenueTypeTax 设置 Dooh.venuetypetax。
func (b *DoohBuilder) VenueTypeTax(tax int32) *DoohBuilder { b.d.Venuetypetax = tax; return b }

// Publisher 设置 Dooh.publisher。
func (b *DoohBuilder) Publisher(p *openrtb.Publisher) *DoohBuilder {
	b.d.Publisher = p
	return b
}

// Build 返回配置完成的 Dooh。
func (b *DoohBuilder) Build() *openrtb.Dooh { return b.d }

// NewPublisher 创建 Publisher 构建器。
func NewPublisher() *PublisherBuilder { return &PublisherBuilder{p: &openrtb.Publisher{}} }

// PublisherBuilder 配置 Publisher 对象。
type PublisherBuilder struct{ p *openrtb.Publisher }

// ID 设置 Publisher.id。
func (b *PublisherBuilder) ID(id string) *PublisherBuilder { b.p.Id = id; return b }

// Name 设置 Publisher.name。
func (b *PublisherBuilder) Name(name string) *PublisherBuilder { b.p.Name = name; return b }

// Domain 设置 Publisher.domain。
func (b *PublisherBuilder) Domain(domain string) *PublisherBuilder { b.p.Domain = domain; return b }

// Build 返回配置完成的 Publisher。
func (b *PublisherBuilder) Build() *openrtb.Publisher { return b.p }

// NewDevice 创建 Device 构建器。
func NewDevice() *DeviceBuilder { return &DeviceBuilder{d: &openrtb.Device{}} }

// DeviceBuilder 配置 Device 对象。
type DeviceBuilder struct{ d *openrtb.Device }

// UA 设置 Device.ua。
func (b *DeviceBuilder) UA(ua string) *DeviceBuilder { b.d.Ua = ua; return b }

// IP 设置 Device.ip。
func (b *DeviceBuilder) IP(ip string) *DeviceBuilder { b.d.Ip = ip; return b }

// IPv6 设置 Device.ipv6。
func (b *DeviceBuilder) IPv6(ip string) *DeviceBuilder { b.d.Ipv6 = ip; return b }

// DeviceType 设置 Device.devicetype。
func (b *DeviceBuilder) DeviceType(t int32) *DeviceBuilder { b.d.Devicetype = t; return b }

// Make 设置 Device.make。
func (b *DeviceBuilder) Make(make string) *DeviceBuilder { b.d.Make = make; return b }

// Model 设置 Device.model。
func (b *DeviceBuilder) Model(model string) *DeviceBuilder { b.d.Model = model; return b }

// OS 设置 Device.os 与 Device.osv。
func (b *DeviceBuilder) OS(os, osv string) *DeviceBuilder { b.d.Os = os; b.d.Osv = osv; return b }

// IFA 设置 Device.ifa。
func (b *DeviceBuilder) IFA(ifa string) *DeviceBuilder { b.d.Ifa = ifa; return b }

// ConnectionType 设置 Device.connectiontype。
func (b *DeviceBuilder) ConnectionType(t int32) *DeviceBuilder { b.d.Connectiontype = t; return b }

// Geo 设置 Device.geo。
func (b *DeviceBuilder) Geo(g *openrtb.Geo) *DeviceBuilder { b.d.Geo = g; return b }

// Build 返回配置完成的 Device。
func (b *DeviceBuilder) Build() *openrtb.Device { return b.d }

// NewGeo 创建 Geo 构建器。
func NewGeo() *GeoBuilder { return &GeoBuilder{g: &openrtb.Geo{}} }

// GeoBuilder 配置 Geo 对象。
type GeoBuilder struct{ g *openrtb.Geo }

// LatLon 设置 Geo.lat 与 Geo.lon。
func (b *GeoBuilder) LatLon(lat, lon float64) *GeoBuilder { b.g.Lat = lat; b.g.Lon = lon; return b }

// Type 设置 Geo.type。
func (b *GeoBuilder) Type(t int32) *GeoBuilder { b.g.Type = t; return b }

// Country 设置 Geo.country。
func (b *GeoBuilder) Country(c string) *GeoBuilder { b.g.Country = c; return b }

// Region 设置 Geo.region。
func (b *GeoBuilder) Region(r string) *GeoBuilder { b.g.Region = r; return b }

// City 设置 Geo.city。
func (b *GeoBuilder) City(c string) *GeoBuilder { b.g.City = c; return b }

// Build 返回配置完成的 Geo。
func (b *GeoBuilder) Build() *openrtb.Geo { return b.g }

// NewContent 创建 Content 构建器。
func NewContent() *ContentBuilder { return &ContentBuilder{c: &openrtb.Content{}} }

// ContentBuilder 配置 Content 对象。
type ContentBuilder struct{ c *openrtb.Content }

// Title 设置 Content.title。
func (b *ContentBuilder) Title(t string) *ContentBuilder { b.c.Title = t; return b }

// Series 设置 Content.series。
func (b *ContentBuilder) Series(s string) *ContentBuilder { b.c.Series = s; return b }

// Season 设置 Content.season。
func (b *ContentBuilder) Season(s string) *ContentBuilder { b.c.Season = s; return b }

// Episode 设置 Content.episode。
func (b *ContentBuilder) Episode(n int32) *ContentBuilder { b.c.Episode = n; return b }

// Context 设置 Content.context。
func (b *ContentBuilder) Context(v int32) *ContentBuilder { b.c.Context = v; return b }

// LiveStream 设置 Content.livestream。
func (b *ContentBuilder) LiveStream(v int32) *ContentBuilder {
	b.c.Livestream = v
	return b
}

// Realtime 设置 Content.realtime。
func (b *ContentBuilder) Realtime(v int32) *ContentBuilder {
	b.c.Realtime = v
	return b
}

// Build 返回配置完成的 Content。
func (b *ContentBuilder) Build() *openrtb.Content { return b.c }

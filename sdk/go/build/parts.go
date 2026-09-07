package build

import (
	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
)

// --- Imp factories -----------------------------------------------------------

// NewBannerImp starts a display impression (sets Imp.banner).
func NewBannerImp(id string) *ImpBuilder {
	return &ImpBuilder{imp: &openrtb.Imp{Id: id, Banner: &openrtb.Banner{}}}
}

// NewVideoImp starts a video impression (sets Imp.video).
func NewVideoImp(id string) *ImpBuilder {
	return &ImpBuilder{imp: &openrtb.Imp{Id: id, Video: &openrtb.Video{}}}
}

// NewAudioImp starts an audio impression (sets Imp.audio).
func NewAudioImp(id string) *ImpBuilder {
	return &ImpBuilder{imp: &openrtb.Imp{Id: id, Audio: &openrtb.Audio{}}}
}

// NewNativeImp starts a native impression (sets Imp.native).
func NewNativeImp(id string) *ImpBuilder {
	return &ImpBuilder{imp: &openrtb.Imp{Id: id, Native: &openrtb.Native{Ver: "1.2"}}}
}

// ImpBuilder configures a single Imp and its format object.
type ImpBuilder struct {
	imp *openrtb.Imp
}

// Floor sets CPM floor and currency.
func (b *ImpBuilder) Floor(bidfloor float64, cur string) *ImpBuilder {
	b.imp.Bidfloor = bidfloor
	b.imp.Bidfloorcur = cur
	return b
}

// Secure requires HTTPS creatives (secure=1).
func (b *ImpBuilder) Secure() *ImpBuilder {
	b.imp.Secure = 1
	return b
}

// TagID sets publisher placement id.
func (b *ImpBuilder) TagID(tagid string) *ImpBuilder {
	b.imp.Tagid = tagid
	return b
}

// Interstitial marks instl=1.
func (b *ImpBuilder) Interstitial() *ImpBuilder {
	b.imp.Instl = 1
	return b
}

// Rewarded marks rwdd=1 (rewarded inventory).
func (b *ImpBuilder) Rewarded() *ImpBuilder {
	b.imp.Rwdd = 1
	return b
}

// Size sets banner or video player w/h.
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

// BannerPos sets Banner.pos (AdPosition).
func (b *ImpBuilder) BannerPos(pos int32) *ImpBuilder {
	if b.imp.Banner != nil {
		b.imp.Banner.Pos = pos
	}
	return b
}

// BannerMimes sets Banner.mimes.
func (b *ImpBuilder) BannerMimes(mimes ...string) *ImpBuilder {
	if b.imp.Banner != nil {
		b.imp.Banner.Mimes = append([]string{}, mimes...)
	}
	return b
}

// VideoMimes sets required Video.mimes.
func (b *ImpBuilder) VideoMimes(mimes ...string) *ImpBuilder {
	if b.imp.Video != nil {
		b.imp.Video.Mimes = append([]string{}, mimes...)
	}
	return b
}

// VideoDuration sets minduration / maxduration seconds.
func (b *ImpBuilder) VideoDuration(min, max int32) *ImpBuilder {
	if b.imp.Video != nil {
		b.imp.Video.Minduration = min
		b.imp.Video.Maxduration = max
	}
	return b
}

// VideoProtocols sets Video.protocols (VAST/DAAST Protocol enum ints).
func (b *ImpBuilder) VideoProtocols(protocols ...int32) *ImpBuilder {
	if b.imp.Video != nil {
		b.imp.Video.Protocols = append([]int32{}, protocols...)
	}
	return b
}

// StartDelay sets Video.startdelay (0 pre / -1 mid / -2 post / >0 mid delay).
func (b *ImpBuilder) StartDelay(v int32) *ImpBuilder {
	if b.imp.Video != nil {
		b.imp.Video.Startdelay = v
	}
	return b
}

// Plcmt sets Video.plcmt (VideoPlcmt).
func (b *ImpBuilder) Plcmt(plcmt int32) *ImpBuilder {
	if b.imp.Video != nil {
		b.imp.Video.Plcmt = plcmt
	}
	return b
}

// Linearity sets Video.linearity.
func (b *ImpBuilder) Linearity(v int32) *ImpBuilder {
	if b.imp.Video != nil {
		b.imp.Video.Linearity = v
	}
	return b
}

// Skip sets Video.skip and optional skipafter seconds.
func (b *ImpBuilder) Skip(skipafter int32) *ImpBuilder {
	if b.imp.Video != nil {
		b.imp.Video.Skip = 1
		b.imp.Video.Skipafter = skipafter
	}
	return b
}

// Pod places the video in an ad pod (podid + slotinpod).
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

// PlaybackMethod sets Video.playbackmethod.
func (b *ImpBuilder) PlaybackMethod(methods ...int32) *ImpBuilder {
	if b.imp.Video != nil {
		b.imp.Video.Playbackmethod = append([]int32{}, methods...)
	}
	return b
}

// AudioMimes sets required Audio.mimes.
func (b *ImpBuilder) AudioMimes(mimes ...string) *ImpBuilder {
	if b.imp.Audio != nil {
		b.imp.Audio.Mimes = append([]string{}, mimes...)
	}
	return b
}

// AudioDuration sets Audio minduration / maxduration.
func (b *ImpBuilder) AudioDuration(min, max int32) *ImpBuilder {
	if b.imp.Audio != nil {
		b.imp.Audio.Minduration = min
		b.imp.Audio.Maxduration = max
	}
	return b
}

// AudioProtocols sets Audio.protocols.
func (b *ImpBuilder) AudioProtocols(protocols ...int32) *ImpBuilder {
	if b.imp.Audio != nil {
		b.imp.Audio.Protocols = append([]int32{}, protocols...)
	}
	return b
}

// Feed sets Audio.feed (FeedType).
func (b *ImpBuilder) Feed(feed int32) *ImpBuilder {
	if b.imp.Audio != nil {
		b.imp.Audio.Feed = feed
	}
	return b
}

// NativeRequest sets Native.request JSON string (Native 1.2 markup request).
func (b *ImpBuilder) NativeRequest(requestJSON string) *ImpBuilder {
	if b.imp.Native != nil {
		b.imp.Native.Request = requestJSON
	}
	return b
}

// NativeVer sets Native.ver (default "1.2").
func (b *ImpBuilder) NativeVer(ver string) *ImpBuilder {
	if b.imp.Native != nil {
		b.imp.Native.Ver = ver
	}
	return b
}

// Build returns the Imp.
func (b *ImpBuilder) Build() *openrtb.Imp {
	return b.imp
}

// --- Inventory / device helpers ---------------------------------------------

// NewSite starts a Site object.
func NewSite() *SiteBuilder { return &SiteBuilder{s: &openrtb.Site{}} }

// SiteBuilder configures Site.
type SiteBuilder struct{ s *openrtb.Site }

func (b *SiteBuilder) ID(id string) *SiteBuilder          { b.s.Id = id; return b }
func (b *SiteBuilder) Name(name string) *SiteBuilder      { b.s.Name = name; return b }
func (b *SiteBuilder) Domain(domain string) *SiteBuilder  { b.s.Domain = domain; return b }
func (b *SiteBuilder) Page(page string) *SiteBuilder      { b.s.Page = page; return b }
func (b *SiteBuilder) Cat(cats ...string) *SiteBuilder    { b.s.Cat = append([]string{}, cats...); return b }
func (b *SiteBuilder) Publisher(p *openrtb.Publisher) *SiteBuilder {
	b.s.Publisher = p
	return b
}
func (b *SiteBuilder) Build() *openrtb.Site { return b.s }

// NewApp starts an App object.
func NewApp() *AppBuilder { return &AppBuilder{a: &openrtb.App{}} }

// AppBuilder configures App.
type AppBuilder struct{ a *openrtb.App }

func (b *AppBuilder) ID(id string) *AppBuilder             { b.a.Id = id; return b }
func (b *AppBuilder) Name(name string) *AppBuilder         { b.a.Name = name; return b }
func (b *AppBuilder) Bundle(bundle string) *AppBuilder     { b.a.Bundle = bundle; return b }
func (b *AppBuilder) Domain(domain string) *AppBuilder     { b.a.Domain = domain; return b }
func (b *AppBuilder) StoreURL(url string) *AppBuilder      { b.a.Storeurl = url; return b }
func (b *AppBuilder) Publisher(p *openrtb.Publisher) *AppBuilder {
	b.a.Publisher = p
	return b
}
func (b *AppBuilder) Content(c *openrtb.Content) *AppBuilder {
	b.a.Content = c
	return b
}
func (b *AppBuilder) Build() *openrtb.App { return b.a }

// NewDooh starts a Dooh object.
func NewDooh() *DoohBuilder { return &DoohBuilder{d: &openrtb.Dooh{}} }

// DoohBuilder configures Dooh.
type DoohBuilder struct{ d *openrtb.Dooh }

func (b *DoohBuilder) ID(id string) *DoohBuilder         { b.d.Id = id; return b }
func (b *DoohBuilder) Name(name string) *DoohBuilder     { b.d.Name = name; return b }
func (b *DoohBuilder) VenueType(ids ...string) *DoohBuilder {
	b.d.Venuetype = append([]string{}, ids...)
	return b
}
func (b *DoohBuilder) VenueTypeTax(tax int32) *DoohBuilder { b.d.Venuetypetax = tax; return b }
func (b *DoohBuilder) Publisher(p *openrtb.Publisher) *DoohBuilder {
	b.d.Publisher = p
	return b
}
func (b *DoohBuilder) Build() *openrtb.Dooh { return b.d }

// NewPublisher starts a Publisher object.
func NewPublisher() *PublisherBuilder { return &PublisherBuilder{p: &openrtb.Publisher{}} }

// PublisherBuilder configures Publisher.
type PublisherBuilder struct{ p *openrtb.Publisher }

func (b *PublisherBuilder) ID(id string) *PublisherBuilder         { b.p.Id = id; return b }
func (b *PublisherBuilder) Name(name string) *PublisherBuilder     { b.p.Name = name; return b }
func (b *PublisherBuilder) Domain(domain string) *PublisherBuilder { b.p.Domain = domain; return b }
func (b *PublisherBuilder) Build() *openrtb.Publisher              { return b.p }

// NewDevice starts a Device object.
func NewDevice() *DeviceBuilder { return &DeviceBuilder{d: &openrtb.Device{}} }

// DeviceBuilder configures Device.
type DeviceBuilder struct{ d *openrtb.Device }

func (b *DeviceBuilder) UA(ua string) *DeviceBuilder             { b.d.Ua = ua; return b }
func (b *DeviceBuilder) IP(ip string) *DeviceBuilder             { b.d.Ip = ip; return b }
func (b *DeviceBuilder) IPv6(ip string) *DeviceBuilder           { b.d.Ipv6 = ip; return b }
func (b *DeviceBuilder) DeviceType(t int32) *DeviceBuilder       { b.d.Devicetype = t; return b }
func (b *DeviceBuilder) Make(make string) *DeviceBuilder         { b.d.Make = make; return b }
func (b *DeviceBuilder) Model(model string) *DeviceBuilder       { b.d.Model = model; return b }
func (b *DeviceBuilder) OS(os, osv string) *DeviceBuilder        { b.d.Os = os; b.d.Osv = osv; return b }
func (b *DeviceBuilder) IFA(ifa string) *DeviceBuilder           { b.d.Ifa = ifa; return b }
func (b *DeviceBuilder) ConnectionType(t int32) *DeviceBuilder   { b.d.Connectiontype = t; return b }
func (b *DeviceBuilder) Geo(g *openrtb.Geo) *DeviceBuilder       { b.d.Geo = g; return b }
func (b *DeviceBuilder) Build() *openrtb.Device                  { return b.d }

// NewGeo starts a Geo object.
func NewGeo() *GeoBuilder { return &GeoBuilder{g: &openrtb.Geo{}} }

// GeoBuilder configures Geo.
type GeoBuilder struct{ g *openrtb.Geo }

func (b *GeoBuilder) LatLon(lat, lon float64) *GeoBuilder { b.g.Lat = lat; b.g.Lon = lon; return b }
func (b *GeoBuilder) Type(t int32) *GeoBuilder            { b.g.Type = t; return b }
func (b *GeoBuilder) Country(c string) *GeoBuilder        { b.g.Country = c; return b }
func (b *GeoBuilder) Region(r string) *GeoBuilder         { b.g.Region = r; return b }
func (b *GeoBuilder) City(c string) *GeoBuilder           { b.g.City = c; return b }
func (b *GeoBuilder) Build() *openrtb.Geo                 { return b.g }

// NewContent starts a Content object.
func NewContent() *ContentBuilder { return &ContentBuilder{c: &openrtb.Content{}} }

// ContentBuilder configures Content.
type ContentBuilder struct{ c *openrtb.Content }

func (b *ContentBuilder) Title(t string) *ContentBuilder   { b.c.Title = t; return b }
func (b *ContentBuilder) Series(s string) *ContentBuilder  { b.c.Series = s; return b }
func (b *ContentBuilder) Season(s string) *ContentBuilder  { b.c.Season = s; return b }
func (b *ContentBuilder) Episode(n int32) *ContentBuilder  { b.c.Episode = n; return b }
func (b *ContentBuilder) Context(v int32) *ContentBuilder  { b.c.Context = v; return b }
func (b *ContentBuilder) LiveStream(v int32) *ContentBuilder {
	b.c.Livestream = v
	return b
}
func (b *ContentBuilder) Realtime(v int32) *ContentBuilder {
	b.c.Realtime = v
	return b
}
func (b *ContentBuilder) Build() *openrtb.Content { return b.c }

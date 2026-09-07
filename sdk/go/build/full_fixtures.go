package build

import openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"

// Full fixtures: every message field set (except ext) with schema-friendly values.
// Generated for exhaustive set/get and JSON round-trip coverage.

func fullFormat() *openrtb.Format {
	return &openrtb.Format{
		W: 300,
		H: 250,
		Wratio: 16,
		Hratio: 9,
		Wmin: 100,
	}
}

func fullMetric() *openrtb.Metric {
	return &openrtb.Metric{
		Type: "Metric.type.v",
		Value: 3.14,
		Vendor: "Metric.vendor.v",
	}
}

func fullDurFloors() *openrtb.DurFloors {
	return &openrtb.DurFloors{
		Mindur: 1,
		Maxdur: 15,
		Bidfloor: 0.5,
		Bidfloorcur: "USD",
	}
}

func fullRefSettings() *openrtb.RefSettings {
	return &openrtb.RefSettings{
		Reftype: 3,
		Minint: 30,
	}
}

func fullRefresh() *openrtb.Refresh {
	return &openrtb.Refresh{
		Refsettings: []*openrtb.RefSettings{fullRefSettings()},
		Count: 2,
	}
}

func fullQty() *openrtb.Qty {
	return &openrtb.Qty{
		Multiplier: 2.5,
		Sourcetype: 1,
		Vendor: "Qty.vendor.v",
	}
}

func fullDeal() *openrtb.Deal {
	return &openrtb.Deal{
		Id: "Deal.id.v",
		Bidfloor: 0.5,
		Bidfloorcur: "USD",
		At: 1,
		Wseat: []string{"Deal.wseat.a", "Deal.wseat.b"},
		Wadomain: []string{"Deal.wadomain.a", "Deal.wadomain.b"},
		Guar: 1,
		Mincpmpersec: 3.14,
		Durfloors: []*openrtb.DurFloors{fullDurFloors()},
	}
}

func fullPmp() *openrtb.Pmp {
	return &openrtb.Pmp{
		PrivateAuction: 1,
		Deals: []*openrtb.Deal{fullDeal()},
	}
}

func fullBrandVersion() *openrtb.BrandVersion {
	return &openrtb.BrandVersion{
		Brand: "BrandVersion.brand.v",
		Version: []string{"BrandVersion.version.a", "BrandVersion.version.b"},
	}
}

func fullUserAgent() *openrtb.UserAgent {
	return &openrtb.UserAgent{
		Browsers: []*openrtb.BrandVersion{fullBrandVersion()},
		Platform: fullBrandVersion(),
		Mobile: 1,
		Architecture: "UserAgent.architecture.v",
		Bitness: "UserAgent.bitness.v",
		Model: "UserAgent.model.v",
		Source: 2,
	}
}

func fullGeo() *openrtb.Geo {
	return &openrtb.Geo{
		Lat: 37.77,
		Lon: -122.42,
		Type: 1,
		Accuracy: 50,
		Lastfix: 10,
		Ipservice: 3,
		Country: "USA",
		Region: "Geo.region.v",
		Metro: "Geo.metro.v",
		City: "Geo.city.v",
		Zip: "Geo.zip.v",
		Utcoffset: 480,
		Regionfips104: "Geo.regionfips104.v",
	}
}

func fullSegment() *openrtb.Segment {
	return &openrtb.Segment{
		Id: "Segment.id.v",
		Name: "Segment.name.v",
		Value: "Segment.value.v",
	}
}

func fullData() *openrtb.Data {
	return &openrtb.Data{
		Id: "Data.id.v",
		Name: "Data.name.v",
		Cids: []string{"Data.cids.a", "Data.cids.b"},
		Segment: []*openrtb.Segment{fullSegment()},
	}
}

func fullUID() *openrtb.UID {
	return &openrtb.UID{
		Id: "UID.id.v",
		Atype: 1,
	}
}

func fullEID() *openrtb.EID {
	return &openrtb.EID{
		Source: "EID.source.v",
		Uids: []*openrtb.UID{fullUID()},
		Inserter: "EID.inserter.v",
		Matcher: "EID.matcher.v",
		Mm: 2,
	}
}

func fullPublisher() *openrtb.Publisher {
	return &openrtb.Publisher{
		Id: "Publisher.id.v",
		Name: "Publisher.name.v",
		Cattax: 7,
		Cat: []string{"Publisher.cat.a", "Publisher.cat.b"},
		Domain: "Publisher.domain.v",
	}
}

func fullProducer() *openrtb.Producer {
	return &openrtb.Producer{
		Id: "Producer.id.v",
		Name: "Producer.name.v",
		Cattax: 7,
		Cat: []string{"Producer.cat.a", "Producer.cat.b"},
		Domain: "Producer.domain.v",
	}
}

func fullNetwork() *openrtb.Network {
	return &openrtb.Network{
		Id: "Network.id.v",
		Name: "Network.name.v",
		Domain: "Network.domain.v",
	}
}

func fullChannel() *openrtb.Channel {
	return &openrtb.Channel{
		Id: "Channel.id.v",
		Name: "Channel.name.v",
		Domain: "Channel.domain.v",
	}
}

func fullContent() *openrtb.Content {
	return &openrtb.Content{
		Id: "Content.id.v",
		Episode: 3,
		Title: "Content.title.v",
		Series: "Content.series.v",
		Season: "Content.season.v",
		Artist: "Content.artist.v",
		Genre: "Content.genre.v",
		Album: "Content.album.v",
		Isrc: "Content.isrc.v",
		Producer: fullProducer(),
		Url: "https://example.com/Content/url",
		Cattax: 6,
		Cat: []string{"Content.cat.a", "Content.cat.b"},
		Prodq: 1,
		Context: 1,
		Contentrating: "Content.contentrating.v",
		Userrating: "Content.userrating.v",
		Qagmediarating: 1,
		Keywords: "Content.keywords.v",
		Kwarray: []string{"Content.kwarray.a", "Content.kwarray.b"},
		Livestream: 1,
		Sourcerelationship: 1,
		Len: 1800,
		Language: "en",
		Langb: "en-US",
		Embeddable: 1,
		Data: []*openrtb.Data{fullData()},
		Network: fullNetwork(),
		Channel: fullChannel(),
		Gtax: 1,
		Genres: []string{"Content.genres.a", "Content.genres.b"},
		Realtime: 1,
		Firstbroadcast: 1,
	}
}

func fullBanner() *openrtb.Banner {
	return &openrtb.Banner{
		Format: []*openrtb.Format{fullFormat()},
		W: 300,
		H: 250,
		Btype: []int32{3, 4},
		Battr: []int32{1, 2},
		Pos: 1,
		Mimes: []string{"image/jpeg", "image/png"},
		Topframe: 1,
		Expdir: []int32{1, 2},
		Api: []int32{3, 5, 7},
		Id: "Banner.id.v",
		Vcm: 1,
	}
}

func fullVideo() *openrtb.Video {
	return &openrtb.Video{
		Mimes: []string{"video/mp4", "video/webm"},
		Minduration: 5,
		Maxduration: 30,
		Startdelay: -1,
		Maxseq: 3,
		Poddur: 90,
		Protocols: []int32{2, 3, 7},
		W: 1920,
		H: 1080,
		Podid: "Video.podid.v",
		Podseq: 1,
		Rqddurs: []int32{15, 30},
		Plcmt: 1,
		Linearity: 1,
		Skip: 1,
		Skipmin: 5,
		Skipafter: 5,
		Slotinpod: 1,
		Mincpmpersec: 3.14,
		Battr: []int32{1, 2},
		Maxextended: 15,
		Minbitrate: 200,
		Maxbitrate: 5000,
		Boxingallowed: 1,
		Playbackmethod: []int32{1, 2},
		Playbackend: 1,
		Delivery: []int32{1, 2},
		Pos: 1,
		Companionad: []*openrtb.Banner{fullBanner()},
		Api: []int32{3, 5, 7},
		Companiontype: []int32{1, 2},
		Placement: 1,
		Poddedupe: []int32{1, 3},
		Durfloors: []*openrtb.DurFloors{fullDurFloors()},
	}
}

func fullAudio() *openrtb.Audio {
	return &openrtb.Audio{
		Mimes: []string{"audio/mpeg"},
		Minduration: 5,
		Maxduration: 30,
		Poddur: 7,
		Protocols: []int32{2, 3, 7},
		Startdelay: 7,
		Rqddurs: []int32{15, 30},
		Podid: "Audio.podid.v",
		Podseq: 1,
		Slotinpod: 1,
		Mincpmpersec: 3.14,
		Battr: []int32{1, 2},
		Maxextended: 7,
		Minbitrate: 7,
		Maxbitrate: 7,
		Delivery: []int32{1, 2},
		Companionad: []*openrtb.Banner{fullBanner()},
		Api: []int32{3, 5, 7},
		Companiontype: []int32{1, 2},
		Maxseq: 7,
		Feed: 3,
		Stitched: 1,
		Nvol: 1,
		Durfloors: []*openrtb.DurFloors{fullDurFloors()},
	}
}

func fullNative() *openrtb.Native {
	return &openrtb.Native{
		Request: "{\"ver\":\"1.2\",\"assets\":[{\"id\":1,\"required\":1,\"title\":{\"len\":90}}]}",
		Ver: "1.2",
		Api: []int32{3, 5, 7},
		Battr: []int32{1, 2},
	}
}

func fullImp() *openrtb.Imp {
	return &openrtb.Imp{
		Id: "Imp.id.v",
		Metric: []*openrtb.Metric{fullMetric()},
		Banner: fullBanner(),
		Video: fullVideo(),
		Audio: fullAudio(),
		Native: fullNative(),
		Pmp: fullPmp(),
		Displaymanager: "Imp.displaymanager.v",
		Displaymanagerver: "Imp.displaymanagerver.v",
		Instl: 1,
		Tagid: "Imp.tagid.v",
		Bidfloor: 0.5,
		Bidfloorcur: "USD",
		Clickbrowser: 1,
		Secure: 1,
		Iframebuster: []string{"Imp.iframebuster.a", "Imp.iframebuster.b"},
		Rwdd: 1,
		Ssai: 2,
		Exp: 7,
		Qty: fullQty(),
		Dt: 3.14,
		Refresh: fullRefresh(),
	}
}

func fullSite() *openrtb.Site {
	return &openrtb.Site{
		Id: "Site.id.v",
		Name: "Site.name.v",
		Domain: "Site.domain.v",
		Cattax: 7,
		Cat: []string{"Site.cat.a", "Site.cat.b"},
		Sectioncat: []string{"Site.sectioncat.a", "Site.sectioncat.b"},
		Pagecat: []string{"Site.pagecat.a", "Site.pagecat.b"},
		Page: "https://example.com/Site/page",
		Ref: "https://example.com/Site/ref",
		Search: "Site.search.v",
		Mobile: 1,
		Privacypolicy: 1,
		Publisher: fullPublisher(),
		Content: fullContent(),
		Keywords: "Site.keywords.v",
		Kwarray: []string{"Site.kwarray.a", "Site.kwarray.b"},
		Inventorypartnerdomain: "Site.inventorypartnerdomain.v",
	}
}

func fullApp() *openrtb.App {
	return &openrtb.App{
		Id: "App.id.v",
		Name: "App.name.v",
		Bundle: "App.bundle.v",
		Domain: "App.domain.v",
		Storeurl: "https://example.com/App/storeurl",
		Cattax: 7,
		Cat: []string{"App.cat.a", "App.cat.b"},
		Sectioncat: []string{"App.sectioncat.a", "App.sectioncat.b"},
		Pagecat: []string{"App.pagecat.a", "App.pagecat.b"},
		Ver: "1.0",
		Privacypolicy: 1,
		Paid: 1,
		Publisher: fullPublisher(),
		Content: fullContent(),
		Keywords: "App.keywords.v",
		Kwarray: []string{"App.kwarray.a", "App.kwarray.b"},
		Inventorypartnerdomain: "App.inventorypartnerdomain.v",
	}
}

func fullDooh() *openrtb.Dooh {
	return &openrtb.Dooh{
		Id: "Dooh.id.v",
		Name: "Dooh.name.v",
		Venue: 1,
		Fixed: 1,
		Publisher: fullPublisher(),
		Domain: "Dooh.domain.v",
		Keywords: "Dooh.keywords.v",
		Kwarray: []string{"Dooh.kwarray.a", "Dooh.kwarray.b"},
		Content: fullContent(),
		Venuetype: []string{"Dooh.venuetype.a", "Dooh.venuetype.b"},
		Venuetypetax: 1,
	}
}

func fullDevice() *openrtb.Device {
	return &openrtb.Device{
		Geo: fullGeo(),
		Dnt: 1,
		Lmt: 1,
		Ua: "Device.ua.v",
		Sua: fullUserAgent(),
		Ip: "192.0.2.10",
		Ipv6: "2001:db8::1",
		Devicetype: 4,
		Make: "Device.make.v",
		Model: "Device.model.v",
		Os: "Device.os.v",
		Osv: "Device.osv.v",
		Hwv: "Device.hwv.v",
		H: 1920,
		W: 1080,
		Ppi: 400,
		Pxratio: 2.0,
		Js: 1,
		Geofetch: 1,
		Language: "en",
		Langb: "en-US",
		Carrier: "Device.carrier.v",
		Mccmnc: "Device.mccmnc.v",
		Connectiontype: 2,
		Ifa: "Device.ifa.v",
	}
}

func fullUser() *openrtb.User {
	return &openrtb.User{
		Id: "User.id.v",
		Buyeruid: "User.buyeruid.v",
		Keywords: "User.keywords.v",
		Kwarray: []string{"User.kwarray.a", "User.kwarray.b"},
		Customdata: "User.customdata.v",
		Geo: fullGeo(),
		Data: []*openrtb.Data{fullData()},
		Consent: "User.consent.v",
		Eids: []*openrtb.EID{fullEID()},
	}
}

func fullSupplyChainNode() *openrtb.SupplyChainNode {
	return &openrtb.SupplyChainNode{
		Asi: "SupplyChainNode.asi.v",
		Sid: "SupplyChainNode.sid.v",
		Rid: "SupplyChainNode.rid.v",
		Name: "SupplyChainNode.name.v",
		Domain: "SupplyChainNode.domain.v",
		Hp: 1,
	}
}

func fullSupplyChain() *openrtb.SupplyChain {
	return &openrtb.SupplyChain{
		Complete: 1,
		Nodes: []*openrtb.SupplyChainNode{fullSupplyChainNode()},
		Ver: "1.0",
	}
}

func fullSource() *openrtb.Source {
	return &openrtb.Source{
		Fd: 1,
		Tid: "Source.tid.v",
		Pchain: "Source.pchain.v",
		Schain: fullSupplyChain(),
	}
}

func fullRegs() *openrtb.Regs {
	return &openrtb.Regs{
		Coppa: 1,
		Gdpr: 1,
		UsPrivacy: "Regs.us_privacy.v",
		Gpp: "Regs.gpp.v",
		GppSid: []int32{2, 6},
	}
}

func fullBid() *openrtb.Bid {
	return &openrtb.Bid{
		Id: "Bid.id.v",
		Impid: "Bid.impid.v",
		Price: 1.23,
		Nurl: "https://example.com/Bid/nurl",
		Burl: "https://example.com/Bid/burl",
		Lurl: "https://example.com/Bid/lurl",
		Adm: "Bid.adm.v",
		Adid: "Bid.adid.v",
		Adomain: []string{"Bid.adomain.a", "Bid.adomain.b"},
		Bundle: "Bid.bundle.v",
		Iurl: "https://example.com/Bid/iurl",
		Cid: "Bid.cid.v",
		Crid: "Bid.crid.v",
		Tactic: "Bid.tactic.v",
		Cattax: 6,
		Cat: []string{"Bid.cat.a", "Bid.cat.b"},
		Attr: []int32{1, 2},
		Apis: []int32{3, 5, 7},
		Protocol: 3,
		Qagmediarating: 1,
		Language: "en",
		Langb: "en-US",
		Dealid: "Bid.dealid.v",
		W: 300,
		H: 250,
		Wratio: 16,
		Hratio: 9,
		Exp: 60,
		Dur: 15,
		Mtype: openrtb.MarkupType_MARKUP_TYPE_BANNER,
		Slotinpod: 1,
	}
}

func fullSeatBid() *openrtb.SeatBid {
	return &openrtb.SeatBid{
		Bid: []*openrtb.Bid{fullBid()},
		Seat: "SeatBid.seat.v",
		Group: 1,
	}
}

func fullBidResponse() *openrtb.BidResponse {
	return &openrtb.BidResponse{
		Id: "BidResponse.id.v",
		Seatbid: []*openrtb.SeatBid{fullSeatBid()},
		Bidid: "BidResponse.bidid.v",
		Cur: "USD",
		Customdata: "BidResponse.customdata.v",
		Nbr: 7,
	}
}

func fullBidRequestWithApp() *openrtb.BidRequest {
	return &openrtb.BidRequest{
		Id: "BidRequest.id.v",
		Imp: []*openrtb.Imp{fullImp()},
		App: fullApp(),
		Device: fullDevice(),
		User: fullUser(),
		Test: 1,
		At: 1,
		Tmax: 120,
		Wseat: []string{"BidRequest.wseat.a", "BidRequest.wseat.b"},
		Bseat: []string{"BidRequest.bseat.a", "BidRequest.bseat.b"},
		Allimps: 1,
		Cur: []string{"USD", "EUR"},
		Wlang: []string{"BidRequest.wlang.a", "BidRequest.wlang.b"},
		Wlangb: []string{"BidRequest.wlangb.a", "BidRequest.wlangb.b"},
		Acat: []string{"BidRequest.acat.a", "BidRequest.acat.b"},
		Bcat: []string{"BidRequest.bcat.a", "BidRequest.bcat.b"},
		Cattax: 6,
		Badv: []string{"BidRequest.badv.a", "BidRequest.badv.b"},
		Bapp: []string{"BidRequest.bapp.a", "BidRequest.bapp.b"},
		Source: fullSource(),
		Regs: fullRegs(),
	}
}

func fullBidRequestWithSite() *openrtb.BidRequest {
	req := fullBidRequestWithApp()
	req.App = nil
	req.Site = fullSite()
	return req
}

func fullBidRequestWithDooh() *openrtb.BidRequest {
	req := fullBidRequestWithApp()
	req.App = nil
	req.Dooh = fullDooh()
	return req
}

// FieldCounts maps message name → number of proto fields excluding ext.
var FieldCounts = map[string]int{
	"App": 17,
	"Audio": 24,
	"Banner": 12,
	"Bid": 31,
	"BidRequest": 23,
	"BidResponse": 6,
	"BrandVersion": 2,
	"Channel": 3,
	"Content": 33,
	"Data": 4,
	"Deal": 9,
	"Device": 25,
	"Dooh": 11,
	"DurFloors": 4,
	"EID": 5,
	"Format": 5,
	"Geo": 13,
	"Imp": 22,
	"Metric": 3,
	"Native": 4,
	"Network": 3,
	"Pmp": 2,
	"Producer": 5,
	"Publisher": 5,
	"Qty": 3,
	"RefSettings": 2,
	"Refresh": 2,
	"Regs": 5,
	"SeatBid": 3,
	"Segment": 3,
	"Site": 17,
	"Source": 4,
	"SupplyChain": 3,
	"SupplyChainNode": 6,
	"UID": 2,
	"User": 9,
	"UserAgent": 7,
	"Video": 34,
}


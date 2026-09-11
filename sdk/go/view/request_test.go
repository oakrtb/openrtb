package view

import (
	"testing"
	"time"

	"github.com/oakrtb/openrtb/sdk/go/build"
	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
)

func TestViewBannerRequest(t *testing.T) {
	req, err := build.NewBidRequest("auction-1").
		FirstPrice().
		Tmax(120).
		Currency("USD").
		Site(build.NewSite().ID("s1").Domain("example.com").Page("https://example.com/a").Build()).
		Device(build.NewDevice().UA("Mozilla/5.0").IP("192.0.2.1").DeviceType(4).Build()).
		AddImp(build.NewBannerImp("1").Size(300, 250).Floor(0.03, "USD").Secure().Build()).
		Build()
	if err != nil {
		t.Fatal(err)
	}

	res, err := RunRequest(req)
	if err != nil {
		t.Fatal(err)
	}
	if res.Shared.ID != "auction-1" || res.Shared.At != 1 {
		t.Fatalf("shared: %+v", res.Shared)
	}
	if res.Shared.Inventory != InventorySite {
		t.Fatalf("inventory=%v", res.Shared.Inventory)
	}
	if res.Shared.Device == nil || res.Shared.Site == nil {
		t.Fatal("expected site+device pointers")
	}
	if !res.Shared.Deadline.IsZero() {
		// tmax set → deadline computed
	} else {
		t.Fatal("expected deadline from tmax")
	}
	if len(res.Imps) != 1 {
		t.Fatalf("imps=%d", len(res.Imps))
	}
	iv := res.Imps[0]
	if !iv.Markup.HasBanner() || iv.Markup.Count() != 1 {
		t.Fatalf("formats=%v", iv.Markup)
	}
	if iv.Markup.Mtype() != 1 {
		t.Fatalf("mtype=%d", iv.Markup.Mtype())
	}
	if iv.Banner == nil || iv.Banner.W != 300 {
		t.Fatalf("banner=%v", iv.Banner)
	}
	if iv.Secure != 1 || iv.BidFloor != 0.03 {
		t.Fatalf("imp view=%+v", iv)
	}
}

func TestViewMultiFormat(t *testing.T) {
	req, err := build.NewBidRequest("m").
		SecondPricePlus().
		Currency("USD").
		App(build.NewApp().ID("a1").Bundle("com.example.app").Build()).
		AddImp(build.NewBannerImp("1").Size(320, 50).Build()).
		Build()
	if err != nil {
		t.Fatal(err)
	}
	// Attach video beside banner on same imp (multi-format).
	req.Imp[0].Video = &openrtb.Video{Mimes: []string{"video/mp4"}}

	res, err := RunRequest(req)
	if err != nil {
		t.Fatal(err)
	}
	if res.Shared.Inventory != InventoryApp {
		t.Fatalf("inventory=%v", res.Shared.Inventory)
	}
	f := res.Imps[0].Markup
	if !f.HasBanner() || !f.HasVideo() || f.Count() != 2 || f.Mtype() != 0 {
		t.Fatalf("formats=%v primary=%v mtype=%d", f, f.Primary(), f.Mtype())
	}
}

func TestLightGateRejects(t *testing.T) {
	req, err := build.NewBidRequest("x").FirstPrice().Currency("USD").
		Site(build.NewSite().ID("s").Build()).
		AddImp(build.NewBannerImp("1").Size(1, 1).Build()).
		Build()
	if err != nil {
		t.Fatal(err)
	}
	req.App = build.NewApp().ID("a").Build()
	if err := LightGateRequest(req); err == nil {
		t.Fatal("expected site/app mutex error")
	}

	req2, err := build.NewBidRequest("y").FirstPrice().Currency("USD").
		AddImp(build.NewBannerImp("1").Size(1, 1).Build()).
		Build()
	if err != nil {
		t.Fatal(err)
	}
	req2.Imp[0].Banner = nil
	if err := LightGateRequest(req2); err == nil {
		t.Fatal("expected no-format error")
	}
}

func TestFormatHelpers(t *testing.T) {
	f := MarkupBanner | MarkupNative
	if f.Count() != 2 || f.Primary() != MarkupNone || f.Mtype() != 0 {
		t.Fatalf("%v", f)
	}
	if !MarkupVideo.HasVideo() || MarkupVideo.Mtype() != 2 {
		t.Fatal(MarkupVideo)
	}
}

func TestLightGateRejectsBlank(t *testing.T) {
	req, err := build.NewBidRequest("x").FirstPrice().Currency("USD").
		AddImp(build.NewBannerImp("1").Size(1, 1).Build()).Build()
	if err != nil {
		t.Fatal(err)
	}
	req.Id = "   "
	if err := LightGateRequest(req); err == nil {
		t.Fatal("expected blank id")
	}
	req.Id = "x"
	req.Cur = []string{"   "}
	if err := LightGateRequest(req); err == nil {
		t.Fatal("expected blank cur entry")
	}
	req.Cur = []string{"USD"}
	req.Imp[0].Id = "	"
	if err := LightGateRequest(req); err == nil {
		t.Fatal("expected blank imp id")
	}
}

func TestSharedPinOnce(t *testing.T) {
	req, err := build.NewBidRequest("x").FirstPrice().Tmax(200).Currency("USD").
		AddImp(build.NewBannerImp("1").Size(1, 1).Build()).Build()
	if err != nil {
		t.Fatal(err)
	}
	p := OfRequest(req).LightGate().Shared()
	d1 := p.shared.Deadline
	time.Sleep(3 * time.Millisecond)
	p.Shared()
	if !p.shared.Deadline.Equal(d1) {
		t.Fatalf("deadline recomputed: %v vs %v", d1, p.shared.Deadline)
	}
}

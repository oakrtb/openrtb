package view

import (
	"testing"

	"github.com/oakrtb/openrtb/sdk/go/build"
)

func TestPipelineRunFacts(t *testing.T) {
	req, err := build.NewBidRequest("auction-1").
		FirstPrice().
		Tmax(120).
		Currency("USD").
		Site(build.NewSite().ID("s1").Domain("example.com").Page("https://example.com/a").Build()).
		AddImp(build.NewBannerImp("1").Size(300, 250).Floor(0.03, "USD").Secure().Build()).
		Build()
	if err != nil {
		t.Fatal(err)
	}

	snap, err := RunRequest(req)
	if err != nil {
		t.Fatal(err)
	}
	if snap.AuctionID() != "auction-1" || snap.Inventory() != InventorySite {
		t.Fatalf("%+v", snap)
	}
	if snap.FindImp("1") == nil {
		t.Fatal("missing imp")
	}
	if len(snap.ImpsWith(MarkupBanner)) != 1 {
		t.Fatal(snap.ImpsWith(MarkupBanner))
	}
	facts := snap.Facts()
	if len(facts) != 1 || !facts[0].HasBanner() || facts[0].Mtype != 1 {
		t.Fatalf("%+v", facts)
	}
	if facts[0].BannerW == nil || *facts[0].BannerW != 300 {
		t.Fatalf("w=%v", facts[0].BannerW)
	}
	if facts[0].BidFloor != 0.03 || facts[0].Secure != 1 {
		t.Fatalf("%+v", facts[0])
	}
}

func TestPipelineStepOrder(t *testing.T) {
	req, err := build.NewBidRequest("x").FirstPrice().Currency("USD").
		AddImp(build.NewBannerImp("1").Size(1, 1).Build()).Build()
	if err != nil {
		t.Fatal(err)
	}
	p := OfRequest(req)
	if _, err := p.Shared().Snapshot(); err == nil {
		t.Fatal("expected Shared before LightGate to fail")
	}
	snap, err := OfRequest(req).Snapshot() // auto-runs remaining
	if err != nil {
		t.Fatal(err)
	}
	if snap.AuctionID() != "x" {
		t.Fatal(snap.AuctionID())
	}
}

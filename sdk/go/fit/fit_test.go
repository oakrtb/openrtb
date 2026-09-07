package fit

import (
	"testing"

	"github.com/oakrtb/openrtb/sdk/go/build"
	"github.com/oakrtb/openrtb/sdk/go/inspect"
	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
)

func bannerSnap(t *testing.T) *inspect.RequestSnapshot {
	t.Helper()
	req, err := build.NewBidRequest("a1").
		FirstPrice().
		Currency("USD").
		Site(build.NewSite().ID("s1").Build()).
		AddImp(build.NewBannerImp("1").Size(300, 250).Floor(1.0, "USD").Build()).
		Build()
	if err != nil {
		t.Fatal(err)
	}
	snap, err := inspect.RunRequest(req)
	if err != nil {
		t.Fatal(err)
	}
	return snap
}

func TestImpReadyVideoMissingMimes(t *testing.T) {
	req := &openrtb.BidRequest{
		Id:  "x",
		At:  1,
		Cur: []string{"USD"},
		Imp: []*openrtb.Imp{{Id: "1", Video: &openrtb.Video{}}},
	}
	snap, err := inspect.RunRequest(req)
	if err != nil {
		t.Fatal(err)
	}
	r := ImpReadyMtype(&snap.Imps[0], 2)
	if r.OK() || !r.Has(CodeVideoMimesMissing) {
		t.Fatalf("got %+v", r)
	}
}

func TestMultiFormatNeedsMtype(t *testing.T) {
	req := &openrtb.BidRequest{
		Id:  "m",
		At:  1,
		Cur: []string{"USD"},
		Imp: []*openrtb.Imp{{
			Id:     "1",
			Banner: &openrtb.Banner{W: 320, H: 50},
			Video:  &openrtb.Video{Mimes: []string{"video/mp4"}},
		}},
	}
	snap, err := inspect.RunRequest(req)
	if err != nil {
		t.Fatal(err)
	}
	bid := &openrtb.Bid{Id: "b1", Impid: "1", Price: 2}
	r := BidFit(snap, bid)
	if r.OK() || !r.Has(CodeMtypeRequired) {
		t.Fatalf("got %+v", r)
	}
}

func TestMtypeMismatch(t *testing.T) {
	snap := bannerSnap(t)
	bid := &openrtb.Bid{
		Id: "b1", Impid: "1", Price: 2,
		Mtype: openrtb.MarkupType_MARKUP_TYPE_VIDEO,
	}
	r := BidFit(snap, bid)
	if r.OK() || !r.Has(CodeMtypeMismatch) {
		t.Fatalf("got %+v", r)
	}
}

func TestPriceBelowFloorWarn(t *testing.T) {
	snap := bannerSnap(t)
	bid := build.NewBid("b1", "1", 0.5).Banner().Build()
	r := BidFit(snap, bid)
	if !r.OK() || !r.Has(CodePriceBelowFloor) {
		t.Fatalf("got %+v", r)
	}
}

func TestAttrBlockedWarn(t *testing.T) {
	req := &openrtb.BidRequest{
		Id:  "x",
		At:  1,
		Cur: []string{"USD"},
		Imp: []*openrtb.Imp{{
			Id:     "1",
			Banner: &openrtb.Banner{W: 300, H: 250, Battr: []int32{1}},
		}},
	}
	snap, err := inspect.RunRequest(req)
	if err != nil {
		t.Fatal(err)
	}
	bid := &openrtb.Bid{
		Id: "b1", Impid: "1", Price: 2,
		Mtype: openrtb.MarkupType_MARKUP_TYPE_BANNER,
		Attr:  []int32{1},
	}
	r := BidFit(snap, bid)
	if !r.OK() || !r.Has(CodeAttrBlocked) {
		t.Fatalf("got %+v", r)
	}
}

func TestNoBidResponseFit(t *testing.T) {
	snap := bannerSnap(t)
	res, err := build.NewBidResponse("a1").Currency("USD").NoBid(0).Build()
	if err != nil {
		t.Fatal(err)
	}
	r := ResponseFit(snap, res)
	if !r.OK() {
		t.Fatalf("got %+v", r)
	}
}

func TestImpidNotFound(t *testing.T) {
	snap := bannerSnap(t)
	bid := build.NewBid("b1", "missing", 2).Banner().Build()
	r := BidFit(snap, bid)
	if r.OK() || !r.Has(CodeImpNotFound) {
		t.Fatalf("got %+v", r)
	}
}

func TestImpReadyMarkupBit(t *testing.T) {
	snap := bannerSnap(t)
	r := ImpReadyMarkup(&snap.Imps[0], inspect.MarkupBanner)
	if !r.OK() {
		t.Fatalf("got %+v", r)
	}
}

func TestResponseFitHappy(t *testing.T) {
	snap := bannerSnap(t)
	res, err := build.NewBidResponse("a1").
		Currency("USD").
		AddSeatBid("", build.NewBid("b1", "1", 2).Banner().Adm("<a/>").Build()).
		Build()
	if err != nil {
		t.Fatal(err)
	}
	r := ResponseFit(snap, res)
	if !r.OK() {
		t.Fatalf("got %+v", r)
	}
}

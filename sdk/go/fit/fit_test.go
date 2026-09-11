package fit

import (
	"testing"

	"github.com/oakrtb/openrtb/sdk/go/build"
	"github.com/oakrtb/openrtb/sdk/go/view"
	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
)

func bannerSnap(t *testing.T) *view.RequestSnapshot {
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
	snap, err := view.RunRequest(req)
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
	snap, err := view.RunRequest(req)
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
	snap, err := view.RunRequest(req)
	if err != nil {
		t.Fatal(err)
	}
	bid := &openrtb.Bid{Id: "b1", Impid: "1", Price: 2}
	r := Bid(snap, bid)
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
	r := Bid(snap, bid)
	if r.OK() || !r.Has(CodeMtypeMismatch) {
		t.Fatalf("got %+v", r)
	}
}

func TestPriceBelowFloorWarn(t *testing.T) {
	snap := bannerSnap(t)
	bid := build.NewBid("b1", "1", 0.5).Banner().Build()
	if Bid(snap, bid).Has(CodePriceBelowFloor) {
		t.Fatal("Fit.Bid without response cur must skip floor compare")
	}
	res, err := build.NewBidResponse("a1").Currency("USD").AddSeatBid("s1", bid).Build()
	if err != nil {
		t.Fatal(err)
	}
	r := Response(snap, res)
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
	snap, err := view.RunRequest(req)
	if err != nil {
		t.Fatal(err)
	}
	bid := &openrtb.Bid{
		Id: "b1", Impid: "1", Price: 2,
		Mtype: openrtb.MarkupType_MARKUP_TYPE_BANNER,
		Attr:  []int32{1},
	}
	r := Bid(snap, bid)
	if !r.OK() || !r.Has(CodeAttrBlocked) {
		t.Fatalf("got %+v", r)
	}
}

func TestNoBidResponse(t *testing.T) {
	snap := bannerSnap(t)
	res, err := build.NewBidResponse("a1").Currency("USD").NoBid(0).Build()
	if err != nil {
		t.Fatal(err)
	}
	r := Response(snap, res)
	if !r.OK() {
		t.Fatalf("got %+v", r)
	}
}

func TestImpidNotFound(t *testing.T) {
	snap := bannerSnap(t)
	bid := build.NewBid("b1", "missing", 2).Banner().Build()
	r := Bid(snap, bid)
	if r.OK() || !r.Has(CodeImpNotFound) {
		t.Fatalf("got %+v", r)
	}
}

func TestImpReadyMarkupBit(t *testing.T) {
	snap := bannerSnap(t)
	r := ImpReadyMarkup(&snap.Imps[0], view.MarkupBanner)
	if !r.OK() {
		t.Fatalf("got %+v", r)
	}
}

func TestResponseHappy(t *testing.T) {
	snap := bannerSnap(t)
	res, err := build.NewBidResponse("a1").
		Currency("USD").
		AddSeatBid("", build.NewBid("b1", "1", 2).Banner().Adm("<a/>").Build()).
		Build()
	if err != nil {
		t.Fatal(err)
	}
	r := Response(snap, res)
	if !r.OK() {
		t.Fatalf("got %+v", r)
	}
}

func TestFloorCurDiffSkipsCompare(t *testing.T) {
	req := &openrtb.BidRequest{
		Id: "a1", At: 1, Cur: []string{"USD"},
		Imp: []*openrtb.Imp{{
			Id: "1", Bidfloor: 1.0, Bidfloorcur: "EUR",
			Banner: &openrtb.Banner{W: 1, H: 1},
		}},
	}
	snap, err := view.RunRequest(req)
	if err != nil {
		t.Fatal(err)
	}
	bid := build.NewBid("b1", "1", 0.1).Banner().Build()
	res, err := build.NewBidResponse("a1").Currency("USD").AddSeatBid("s", bid).Build()
	if err != nil {
		t.Fatal(err)
	}
	r := Response(snap, res)
	if !r.OK() || !r.Has(CodeFloorCurDiff) || r.Has(CodePriceBelowFloor) {
		t.Fatalf("got %+v", r)
	}
}

func TestResponseCurWhitespaceTrimmed(t *testing.T) {
	snap := bannerSnap(t)
	bid := build.NewBid("b1", "1", 0.5).Banner().Build()
	res, err := build.NewBidResponse("a1").Currency("USD").AddSeatBid("s", bid).Build()
	if err != nil {
		t.Fatal(err)
	}
	res.Cur = "  USD  "
	r := Response(snap, res)
	if !r.OK() || r.Has(CodeCurNotAllowed) || !r.Has(CodePriceBelowFloor) {
		t.Fatalf("got %+v", r)
	}
}

func TestResponseNilMalformed(t *testing.T) {
	r := Response(nil, nil)
	if r.OK() || !r.Has(CodeMalformed) {
		t.Fatalf("got %+v", r)
	}
}

func TestResponseEmptyBidMalformed(t *testing.T) {
	snap := bannerSnap(t)
	res := &openrtb.BidResponse{Id: "a1", Cur: "USD", Seatbid: []*openrtb.SeatBid{{Seat: "s"}}}
	r := Response(snap, res)
	if r.OK() || !r.Has(CodeMalformed) {
		t.Fatalf("got %+v", r)
	}
}

func TestResponseBlankCurSkipsCurAndFloor(t *testing.T) {
	snap := bannerSnap(t)
	bid := build.NewBid("b1", "1", 0.5).Banner().Build()
	res, err := build.NewBidResponse("a1").Currency("USD").AddSeatBid("s", bid).Build()
	if err != nil {
		t.Fatal(err)
	}
	res.Cur = "   "
	r := Response(snap, res)
	if !r.OK() || r.Has(CodeCurNotAllowed) || r.Has(CodePriceBelowFloor) {
		t.Fatalf("got %+v", r)
	}
}

func TestCurNotAllowed(t *testing.T) {
	snap := bannerSnap(t)
	bid := build.NewBid("b1", "1", 2).Banner().Build()
	res, err := build.NewBidResponse("a1").Currency("EUR").AddSeatBid("s", bid).Build()
	if err != nil {
		t.Fatal(err)
	}
	r := Response(snap, res)
	if !r.OK() || !r.Has(CodeCurNotAllowed) {
		t.Fatalf("got %+v", r)
	}
}

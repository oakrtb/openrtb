package view

import (
	"testing"

	"github.com/oakrtb/openrtb/sdk/go/build"
)

func TestResponsePipelineRunFacts(t *testing.T) {
	res, err := build.NewBidResponse("auction-1").
		Currency("USD").
		AddSeatBid("512",
			build.NewBid("1", "1", 1.23).Banner().Size(300, 250).Adm("<img/>").Crid("c1").Adomain("adv.com").Build(),
		).
		Build()
	if err != nil {
		t.Fatal(err)
	}

	snap, err := RunResponse(res)
	if err != nil {
		t.Fatal(err)
	}
	if snap.RequestID() != "auction-1" || snap.NoBid() || snap.Currency() != "USD" {
		t.Fatalf("%+v", snap.Shared)
	}
	if snap.FindBid("1") == nil {
		t.Fatal("missing bid")
	}
	if len(snap.BidsForImp("1")) != 1 {
		t.Fatal(snap.BidsForImp("1"))
	}
	if len(snap.BidsWith(MarkupBanner)) != 1 {
		t.Fatal(snap.BidsWith(MarkupBanner))
	}
	facts := snap.Facts()
	if len(facts) != 1 || !facts[0].HasBanner() || facts[0].Mtype != 1 {
		t.Fatalf("%+v", facts)
	}
	if facts[0].Price != 1.23 || !facts[0].HasAdm || facts[0].W != 300 {
		t.Fatalf("%+v", facts[0])
	}
}

func TestResponsePipelineNoBid(t *testing.T) {
	res, err := build.NewBidResponse("auction-1").NoBid(2).Build()
	if err != nil {
		t.Fatal(err)
	}
	snap, err := RunResponse(res)
	if err != nil {
		t.Fatal(err)
	}
	if !snap.NoBid() || snap.Nbr() != 2 || len(snap.Bids) != 0 {
		t.Fatalf("%+v", snap)
	}
}

func TestResponseLightGateRejects(t *testing.T) {
	res, err := build.NewBidResponse("x").
		AddSeatBid("s", build.NewBid("1", "1", 1.0).Build()).
		Build()
	if err != nil {
		t.Fatal(err)
	}
	res.Seatbid[0].Bid[0].Price = 0
	if err := LightGateResponse(res); err == nil {
		t.Fatal("expected price error")
	}
}

func TestResponseLightGateRejectsBlankCur(t *testing.T) {
	res, err := build.NewBidResponse("x").Currency("USD").NoBid(0).Build()
	if err != nil {
		t.Fatal(err)
	}
	res.Cur = "   "
	if err := LightGateResponse(res); err == nil {
		t.Fatal("expected blank cur")
	}
}

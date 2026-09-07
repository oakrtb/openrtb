package build

import (
	"fmt"

	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
	"github.com/oakrtb/openrtb/sdk/go/validate"
)

// BidResponseBuilder assembles a BidResponse (bids or structured no-bid).
type BidResponseBuilder struct {
	res      *openrtb.BidResponse
	noBidSet bool
	err      error
}

// NewBidResponse starts a response for the given BidRequest.id.
func NewBidResponse(requestID string) *BidResponseBuilder {
	b := &BidResponseBuilder{res: &openrtb.BidResponse{Id: requestID, Cur: "USD"}}
	if requestID == "" {
		b.err = fmt.Errorf("build: BidResponse.id is required (echo BidRequest.id)")
	}
	return b
}

// BidID sets bidder-side response id.
func (b *BidResponseBuilder) BidID(bidid string) *BidResponseBuilder {
	if b.err != nil {
		return b
	}
	b.res.Bidid = bidid
	return b
}

// Currency sets BidResponse.cur (ISO-4217).
func (b *BidResponseBuilder) Currency(cur string) *BidResponseBuilder {
	if b.err != nil {
		return b
	}
	b.res.Cur = cur
	return b
}

// NoBid sets structured no-bid reason (nbr) and clears seatbid.
func (b *BidResponseBuilder) NoBid(nbr int32) *BidResponseBuilder {
	if b.err != nil {
		return b
	}
	b.res.Nbr = nbr
	b.res.Seatbid = nil
	b.noBidSet = true
	return b
}

// AddSeatBid appends a seat's bids.
func (b *BidResponseBuilder) AddSeatBid(seat string, bids ...*openrtb.Bid) *BidResponseBuilder {
	if b.err != nil {
		return b
	}
	if len(bids) == 0 {
		b.err = fmt.Errorf("build: SeatBid requires at least one Bid")
		return b
	}
	b.res.Seatbid = append(b.res.Seatbid, &openrtb.SeatBid{
		Seat: seat,
		Bid:  bids,
	})
	return b
}

// Build returns the BidResponse after structural checks.
func (b *BidResponseBuilder) Build() (*openrtb.BidResponse, error) {
	if b.err != nil {
		return nil, b.err
	}
	if b.res.Id == "" {
		return nil, fmt.Errorf("build: BidResponse.id is required")
	}
	if b.res.Cur == "" {
		return nil, fmt.Errorf("build: BidResponse.cur is required (ISO-4217; default USD if unset via Currency)")
	}
	if len(b.res.Seatbid) == 0 && !b.noBidSet {
		return nil, fmt.Errorf("build: BidResponse needs seatbid[] or NoBid(nbr)")
	}
	for i, sb := range b.res.Seatbid {
		for j, bid := range sb.Bid {
			if bid == nil {
				return nil, fmt.Errorf("build: seatbid[%d].bid[%d] is nil", i, j)
			}
			if bid.Id == "" || bid.Impid == "" {
				return nil, fmt.Errorf("build: seatbid[%d].bid[%d] requires id and impid", i, j)
			}
			if bid.Price <= 0 {
				return nil, fmt.Errorf("build: seatbid[%d].bid[%d].price must be > 0", i, j)
			}
		}
	}
	return b.res, nil
}

// MustBuild is Build but panics on error.
func (b *BidResponseBuilder) MustBuild() *openrtb.BidResponse {
	res, err := b.Build()
	if err != nil {
		panic(err)
	}
	return res
}

// BuildJSON builds and marshals to OpenRTB JSON.
func (b *BidResponseBuilder) BuildJSON() ([]byte, error) {
	res, err := b.Build()
	if err != nil {
		return nil, err
	}
	return MarshalJSON(res)
}

// BuildValidated builds, marshals, and runs JSON Schema validation.
func (b *BidResponseBuilder) BuildValidated() ([]byte, validate.ValidationResult, error) {
	raw, err := b.BuildJSON()
	if err != nil {
		return nil, validate.ValidationResult{}, err
	}
	return raw, validate.ValidateBidResponse(raw), nil
}

// --- Bid factory -------------------------------------------------------------

// NewBid starts a single Bid (id, impid, price required).
func NewBid(id, impid string, price float64) *BidBuilder {
	return &BidBuilder{b: &openrtb.Bid{Id: id, Impid: impid, Price: price}}
}

// BidBuilder configures a Bid.
type BidBuilder struct{ b *openrtb.Bid }

func (b *BidBuilder) Adm(adm string) *BidBuilder { b.b.Adm = adm; return b }
func (b *BidBuilder) NURL(u string) *BidBuilder  { b.b.Nurl = u; return b }
func (b *BidBuilder) BURL(u string) *BidBuilder  { b.b.Burl = u; return b }
func (b *BidBuilder) LURL(u string) *BidBuilder  { b.b.Lurl = u; return b }
func (b *BidBuilder) Crid(crid string) *BidBuilder {
	b.b.Crid = crid
	return b
}
func (b *BidBuilder) Cid(cid string) *BidBuilder { b.b.Cid = cid; return b }
func (b *BidBuilder) Adomain(domains ...string) *BidBuilder {
	b.b.Adomain = append([]string{}, domains...)
	return b
}
func (b *BidBuilder) Size(w, h int32) *BidBuilder { b.b.W = w; b.b.H = h; return b }
func (b *BidBuilder) DealID(id string) *BidBuilder {
	b.b.Dealid = id
	return b
}
func (b *BidBuilder) MarkupType(m openrtb.MarkupType) *BidBuilder {
	b.b.Mtype = m
	return b
}
func (b *BidBuilder) Banner() *BidBuilder {
	return b.MarkupType(openrtb.MarkupType_MARKUP_TYPE_BANNER)
}
func (b *BidBuilder) Video() *BidBuilder {
	return b.MarkupType(openrtb.MarkupType_MARKUP_TYPE_VIDEO)
}
func (b *BidBuilder) Audio() *BidBuilder {
	return b.MarkupType(openrtb.MarkupType_MARKUP_TYPE_AUDIO)
}
func (b *BidBuilder) Native() *BidBuilder {
	return b.MarkupType(openrtb.MarkupType_MARKUP_TYPE_NATIVE)
}
func (b *BidBuilder) Dur(seconds int32) *BidBuilder { b.b.Dur = seconds; return b }
func (b *BidBuilder) Build() *openrtb.Bid           { return b.b }

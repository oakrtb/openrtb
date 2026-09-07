package build

import (
	"fmt"

	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
	"github.com/oakrtb/openrtb/sdk/go/validate"
)

// BidRequestBuilder assembles a BidRequest with inventory + impressions.
type BidRequestBuilder struct {
	req *openrtb.BidRequest
	err error
}

// NewBidRequest starts a request builder. id is required (auction id).
func NewBidRequest(id string) *BidRequestBuilder {
	b := &BidRequestBuilder{req: &openrtb.BidRequest{Id: id}}
	if id == "" {
		b.err = fmt.Errorf("build: BidRequest.id is required")
	}
	return b
}

// FirstPrice sets at=1 (AuctionType first price).
func (b *BidRequestBuilder) FirstPrice() *BidRequestBuilder {
	return b.AuctionType(int32(openrtb.AuctionType_AUCTION_TYPE_FIRST_PRICE))
}

// SecondPricePlus sets at=2 (default OpenRTB auction type).
func (b *BidRequestBuilder) SecondPricePlus() *BidRequestBuilder {
	return b.AuctionType(int32(openrtb.AuctionType_AUCTION_TYPE_SECOND_PRICE_PLUS))
}

// AuctionType sets BidRequest.at.
func (b *BidRequestBuilder) AuctionType(at int32) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.At = at
	return b
}

// Tmax sets timeout milliseconds (includes network).
func (b *BidRequestBuilder) Tmax(ms int32) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Tmax = ms
	return b
}

// Currency sets acceptable bid currencies (ISO-4217), replacing prior list.
func (b *BidRequestBuilder) Currency(codes ...string) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Cur = append([]string{}, codes...)
	return b
}

// Test marks the request as test traffic (test=1).
func (b *BidRequestBuilder) Test() *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Test = 1
	return b
}

// Bcat sets blocked advertiser categories.
func (b *BidRequestBuilder) Bcat(cats ...string) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Bcat = append([]string{}, cats...)
	return b
}

// Badv sets blocked advertiser domains.
func (b *BidRequestBuilder) Badv(domains ...string) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Badv = append([]string{}, domains...)
	return b
}

// Site sets website inventory (clears app/dooh).
func (b *BidRequestBuilder) Site(site *openrtb.Site) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Site = site
	b.req.App = nil
	b.req.Dooh = nil
	return b
}

// App sets application inventory (clears site/dooh).
func (b *BidRequestBuilder) App(app *openrtb.App) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.App = app
	b.req.Site = nil
	b.req.Dooh = nil
	return b
}

// Dooh sets digital-out-of-home inventory (clears site/app).
func (b *BidRequestBuilder) Dooh(dooh *openrtb.Dooh) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Dooh = dooh
	b.req.Site = nil
	b.req.App = nil
	return b
}

// Device sets device context.
func (b *BidRequestBuilder) Device(device *openrtb.Device) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Device = device
	return b
}

// User sets user/audience context.
func (b *BidRequestBuilder) User(user *openrtb.User) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.User = user
	return b
}

// Regs sets privacy/regulation signals.
func (b *BidRequestBuilder) Regs(regs *openrtb.Regs) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Regs = regs
	return b
}

// Source sets upstream source / supply chain.
func (b *BidRequestBuilder) Source(source *openrtb.Source) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Source = source
	return b
}

// AddImp appends an impression opportunity.
func (b *BidRequestBuilder) AddImp(imp *openrtb.Imp) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	if imp == nil {
		b.err = fmt.Errorf("build: nil Imp")
		return b
	}
	b.req.Imp = append(b.req.Imp, imp)
	return b
}

// Build returns the BidRequest after structural checks (id + ≥1 imp with format).
func (b *BidRequestBuilder) Build() (*openrtb.BidRequest, error) {
	if b.err != nil {
		return nil, b.err
	}
	if b.req.Id == "" {
		return nil, fmt.Errorf("build: BidRequest.id is required")
	}
	if b.req.At == 0 {
		return nil, fmt.Errorf("build: BidRequest.at is required (use FirstPrice/SecondPricePlus/AuctionType)")
	}
	if len(b.req.Cur) == 0 {
		return nil, fmt.Errorf("build: BidRequest.cur is required (at least one ISO-4217 code)")
	}
	if len(b.req.Imp) == 0 {
		return nil, fmt.Errorf("build: BidRequest.imp requires at least one Imp")
	}
	for i, imp := range b.req.Imp {
		if err := checkImp(imp, i); err != nil {
			return nil, err
		}
	}
	n := 0
	if b.req.Site != nil {
		n++
	}
	if b.req.App != nil {
		n++
	}
	if b.req.Dooh != nil {
		n++
	}
	if n > 1 {
		return nil, fmt.Errorf("build: site/app/dooh are mutually exclusive")
	}
	return b.req, nil
}

// MustBuild is Build but panics on error.
func (b *BidRequestBuilder) MustBuild() *openrtb.BidRequest {
	req, err := b.Build()
	if err != nil {
		panic(err)
	}
	return req
}

// BuildJSON builds and marshals to OpenRTB JSON.
func (b *BidRequestBuilder) BuildJSON() ([]byte, error) {
	req, err := b.Build()
	if err != nil {
		return nil, err
	}
	return MarshalJSON(req)
}

// BuildValidated builds, marshals, and runs JSON Schema validation.
func (b *BidRequestBuilder) BuildValidated() ([]byte, validate.ValidationResult, error) {
	raw, err := b.BuildJSON()
	if err != nil {
		return nil, validate.ValidationResult{}, err
	}
	return raw, validate.ValidateBidRequest(raw), nil
}

func checkImp(imp *openrtb.Imp, i int) error {
	if imp == nil {
		return fmt.Errorf("build: imp[%d] is nil", i)
	}
	if imp.Id == "" {
		return fmt.Errorf("build: imp[%d].id is required", i)
	}
	formats := 0
	if imp.Banner != nil {
		formats++
		if imp.Banner.W == 0 && imp.Banner.H == 0 && len(imp.Banner.Format) == 0 {
			return fmt.Errorf("build: imp[%d].banner needs w/h or format[]", i)
		}
	}
	if imp.Video != nil {
		formats++
		if len(imp.Video.Mimes) == 0 {
			return fmt.Errorf("build: imp[%d].video.mimes is required", i)
		}
	}
	if imp.Audio != nil {
		formats++
		if len(imp.Audio.Mimes) == 0 {
			return fmt.Errorf("build: imp[%d].audio.mimes is required", i)
		}
	}
	if imp.Native != nil {
		formats++
		if imp.Native.Request == "" {
			return fmt.Errorf("build: imp[%d].native.request is required", i)
		}
	}
	if formats == 0 {
		return fmt.Errorf("build: imp[%d] needs banner, video, audio, or native", i)
	}
	return nil
}

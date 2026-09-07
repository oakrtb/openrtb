package build

import (
	"fmt"

	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
	"github.com/oakrtb/openrtb/sdk/go/validate"
)

// BidResponseBuilder 组装 BidResponse（出价或结构化 no-bid）。
type BidResponseBuilder struct {
	res      *openrtb.BidResponse
	noBidSet bool
	err      error
}

// NewBidResponse 为给定 BidRequest.id 创建响应构建器。
func NewBidResponse(requestID string) *BidResponseBuilder {
	b := &BidResponseBuilder{res: &openrtb.BidResponse{Id: requestID, Cur: "USD"}}
	if requestID == "" {
		b.err = fmt.Errorf("build: BidResponse.id is required (echo BidRequest.id)")
	}
	return b
}

// BidID 设置买方侧响应 id。
func (b *BidResponseBuilder) BidID(bidid string) *BidResponseBuilder {
	if b.err != nil {
		return b
	}
	b.res.Bidid = bidid
	return b
}

// Currency 设置 BidResponse.cur（ISO-4217）。
func (b *BidResponseBuilder) Currency(cur string) *BidResponseBuilder {
	if b.err != nil {
		return b
	}
	b.res.Cur = cur
	return b
}

// NoBid 设置结构化不出价原因（nbr）并清除 seatbid。
func (b *BidResponseBuilder) NoBid(nbr int32) *BidResponseBuilder {
	if b.err != nil {
		return b
	}
	b.res.Nbr = nbr
	b.res.Seatbid = nil
	b.noBidSet = true
	return b
}

// AddSeatBid 追加一个 seat 的出价列表；会清除先前的 NoBid 状态。
func (b *BidResponseBuilder) AddSeatBid(seat string, bids ...*openrtb.Bid) *BidResponseBuilder {
	if b.err != nil {
		return b
	}
	if len(bids) == 0 {
		b.err = fmt.Errorf("build: SeatBid requires at least one Bid")
		return b
	}
	b.noBidSet = false
	b.res.Nbr = 0
	b.res.Seatbid = append(b.res.Seatbid, &openrtb.SeatBid{
		Seat: seat,
		Bid:  bids,
	})
	return b
}

// Build 在结构校验后返回 BidResponse。
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

// MustBuild 同 Build，出错时 panic。
func (b *BidResponseBuilder) MustBuild() *openrtb.BidResponse {
	res, err := b.Build()
	if err != nil {
		panic(err)
	}
	return res
}

// BuildJSON 构建并序列化为 OpenRTB JSON。
func (b *BidResponseBuilder) BuildJSON() ([]byte, error) {
	res, err := b.Build()
	if err != nil {
		return nil, err
	}
	return MarshalJSON(res)
}

// BuildValidated 构建、序列化并执行 JSON Schema 校验。
func (b *BidResponseBuilder) BuildValidated() ([]byte, validate.ValidationResult, error) {
	raw, err := b.BuildJSON()
	if err != nil {
		return nil, validate.ValidationResult{}, err
	}
	return raw, validate.ValidateBidResponse(raw), nil
}

// --- Bid factory -------------------------------------------------------------

// NewBid 创建单个 Bid（id、impid、price 必填）。
func NewBid(id, impid string, price float64) *BidBuilder {
	return &BidBuilder{b: &openrtb.Bid{Id: id, Impid: impid, Price: price}}
}

// BidBuilder 配置单个 Bid。
type BidBuilder struct{ b *openrtb.Bid }

// Adm 设置广告 markup。
func (b *BidBuilder) Adm(adm string) *BidBuilder { b.b.Adm = adm; return b }

// NURL 设置 win notice URL。
func (b *BidBuilder) NURL(u string) *BidBuilder { b.b.Nurl = u; return b }

// BURL 设置 billing notice URL。
func (b *BidBuilder) BURL(u string) *BidBuilder { b.b.Burl = u; return b }

// LURL 设置 loss notice URL。
func (b *BidBuilder) LURL(u string) *BidBuilder { b.b.Lurl = u; return b }

// Crid 设置创意 id。
func (b *BidBuilder) Crid(crid string) *BidBuilder {
	b.b.Crid = crid
	return b
}

// Cid 设置 Campaign id。
func (b *BidBuilder) Cid(cid string) *BidBuilder { b.b.Cid = cid; return b }

// Adomain 设置广告主域名列表。
func (b *BidBuilder) Adomain(domains ...string) *BidBuilder {
	b.b.Adomain = append([]string{}, domains...)
	return b
}

// Size 设置创意宽高。
func (b *BidBuilder) Size(w, h int32) *BidBuilder { b.b.W = w; b.b.H = h; return b }

// DealID 设置 PMP deal id。
func (b *BidBuilder) DealID(id string) *BidBuilder {
	b.b.Dealid = id
	return b
}

// MarkupType 设置 Bid.mtype。
func (b *BidBuilder) MarkupType(m openrtb.MarkupType) *BidBuilder {
	b.b.Mtype = m
	return b
}

// Banner 将 mtype 设为 Banner（1）。
func (b *BidBuilder) Banner() *BidBuilder {
	return b.MarkupType(openrtb.MarkupType_MARKUP_TYPE_BANNER)
}

// Video 将 mtype 设为 Video（2）。
func (b *BidBuilder) Video() *BidBuilder {
	return b.MarkupType(openrtb.MarkupType_MARKUP_TYPE_VIDEO)
}

// Audio 将 mtype 设为 Audio（3）。
func (b *BidBuilder) Audio() *BidBuilder {
	return b.MarkupType(openrtb.MarkupType_MARKUP_TYPE_AUDIO)
}

// Native 将 mtype 设为 Native（4）。
func (b *BidBuilder) Native() *BidBuilder {
	return b.MarkupType(openrtb.MarkupType_MARKUP_TYPE_NATIVE)
}

// Dur 设置创意时长（秒）。
func (b *BidBuilder) Dur(seconds int32) *BidBuilder { b.b.Dur = seconds; return b }

// Build 返回配置完成的 Bid。
func (b *BidBuilder) Build() *openrtb.Bid { return b.b }

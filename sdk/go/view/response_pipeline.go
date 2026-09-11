package view

import (
	"fmt"

	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
)

// ResponsePipeline 按序编排 LightGate → SharedView → BidViews（BidResponse）。
type ResponsePipeline struct {
	res      *openrtb.BidResponse
	shared   ResponseSharedView
	seatBids []SeatBidView
	bids     []BidView
	gated    bool
	pinned   bool
	viewed   bool
	err      error
}

// OfResponse 对已解码的 BidResponse 启动流水线编排。
func OfResponse(res *openrtb.BidResponse) *ResponsePipeline {
	return &ResponsePipeline{res: res}
}

// RunResponse 执行全部响应步骤并返回 ResponseSnapshot。
func RunResponse(res *openrtb.BidResponse) (*ResponseSnapshot, error) {
	return OfResponse(res).LightGate().Shared().Bids().Snapshot()
}

// LightGate 为第 1 步：轻量结构检查。
func (p *ResponsePipeline) LightGate() *ResponsePipeline {
	if p.err != nil {
		return p
	}
	if err := LightGateResponse(p.res); err != nil {
		p.err = err
		return p
	}
	p.gated = true
	return p
}

// Shared 为第 2 步：构建拍卖级 ResponseSharedView。
func (p *ResponsePipeline) Shared() *ResponsePipeline {
	if p.err != nil {
		return p
	}
	if p.pinned {
		return p
	}
	if !p.gated {
		p.err = fmt.Errorf("pipeline: call lightGate first")
		return p
	}
	p.shared = responseShared(p.res)
	p.pinned = true
	return p
}

// Bids 为第 3 步：构建 SeatBidView 与扁平 BidView 列表。
func (p *ResponsePipeline) Bids() *ResponsePipeline {
	if p.err != nil {
		return p
	}
	if !p.pinned {
		p.err = fmt.Errorf("pipeline: call shared first")
		return p
	}
	p.seatBids, p.bids = viewSeatBids(p.res)
	p.viewed = true
	return p
}

// Snapshot 完成流水线（自动按序补跑未执行的步骤）。
func (p *ResponsePipeline) Snapshot() (*ResponseSnapshot, error) {
	if p.err != nil {
		return nil, p.err
	}
	if !p.gated {
		p.LightGate()
		if p.err != nil {
			return nil, p.err
		}
	}
	if !p.pinned {
		p.Shared()
		if p.err != nil {
			return nil, p.err
		}
	}
	if !p.viewed {
		p.Bids()
		if p.err != nil {
			return nil, p.err
		}
	}
	return &ResponseSnapshot{Shared: p.shared, SeatBids: p.seatBids, Bids: p.bids}, nil
}

// ResponseSnapshot 是已完成响应流水线的可查询输出。
type ResponseSnapshot struct {
	Shared   ResponseSharedView
	SeatBids []SeatBidView
	Bids     []BidView
}

// RequestID 返回对应请求的 id（BidResponse.id）。
func (s *ResponseSnapshot) RequestID() string { return s.Shared.ID }

// BidID 返回买方响应 id（BidResponse.bidid）。
func (s *ResponseSnapshot) BidID() string { return s.Shared.BidID }

// Currency 返回响应货币（BidResponse.cur）。
func (s *ResponseSnapshot) Currency() string { return s.Shared.Cur }

// NoBid 报告是否为 no-bid（无 seatbid）。
func (s *ResponseSnapshot) NoBid() bool { return s.Shared.NoBid }

// Nbr 返回结构化不出价原因码（BidResponse.nbr）。
func (s *ResponseSnapshot) Nbr() int32 { return s.Shared.Nbr }

// Response 返回底层 BidResponse 指针。
func (s *ResponseSnapshot) Response() *openrtb.BidResponse { return s.Shared.Res }

// FindBid 按 bid id 查找 BidView，未找到时返回 nil。
func (s *ResponseSnapshot) FindBid(id string) *BidView {
	for i := range s.Bids {
		if s.Bids[i].ID == id {
			return &s.Bids[i]
		}
	}
	return nil
}

// BidsForImp 返回指定 impid 的全部 BidView。
func (s *ResponseSnapshot) BidsForImp(impid string) []BidView {
	var out []BidView
	for _, b := range s.Bids {
		if b.ImpID == impid {
			out = append(out, b)
		}
	}
	return out
}

// BidsWith 返回包含指定 markup 标志的 BidView 列表。
func (s *ResponseSnapshot) BidsWith(flag MarkupMask) []BidView {
	var out []BidView
	for _, b := range s.Bids {
		if b.Markup.Has(flag) {
			out = append(out, b)
		}
	}
	return out
}

// Facts 返回用于下游处理的紧凑 per-bid 摘要。
func (s *ResponseSnapshot) Facts() []BidFact {
	out := make([]BidFact, 0, len(s.Bids))
	for _, b := range s.Bids {
		out = append(out, bidFactFrom(b))
	}
	return out
}

// BidFact 是从 BidView 提取的扁平摘要，供调用方使用。
type BidFact struct {
	ID      string
	ImpID   string
	Seat    string
	Price   float64
	Markup  MarkupMask
	Mtype   int32
	Crid    string
	DealID  string
	W       int32
	H       int32
	Dur     int32
	HasAdm  bool
	Adomain []string
}

func bidFactFrom(b BidView) BidFact {
	return BidFact{
		ID:      b.ID,
		ImpID:   b.ImpID,
		Seat:    b.Seat,
		Price:   b.Price,
		Markup:  b.Markup,
		Mtype:   b.Mtype,
		Crid:    b.Crid,
		DealID:  b.DealID,
		W:       b.W,
		H:       b.H,
		Dur:     b.Dur,
		HasAdm:  b.Adm != "",
		Adomain: b.Adomain,
	}
}

// HasBanner 报告 BidFact 是否为 Banner markup。
func (f BidFact) HasBanner() bool { return f.Markup.HasBanner() }

// HasVideo 报告 BidFact 是否为 Video markup。
func (f BidFact) HasVideo() bool { return f.Markup.HasVideo() }

// HasAudio 报告 BidFact 是否为 Audio markup。
func (f BidFact) HasAudio() bool { return f.Markup.HasAudio() }

// HasNative 报告 BidFact 是否为 Native markup。
func (f BidFact) HasNative() bool { return f.Markup.HasNative() }

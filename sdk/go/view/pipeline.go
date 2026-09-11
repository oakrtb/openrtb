package view

import (
	"fmt"

	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
)

// RequestPipeline 按序编排 LightGate → RequestSharedView → ImpView。
// 一次性使用请用 RunRequest；分步调用请 OfRequest 后链式执行各步骤 → Snapshot。
type RequestPipeline struct {
	req    *openrtb.BidRequest
	shared RequestSharedView
	imps   []ImpView
	gated  bool
	pinned bool
	viewed bool
	err    error
}

// OfRequest 对已解码的 BidRequest 启动流水线编排。
func OfRequest(req *openrtb.BidRequest) *RequestPipeline {
	return &RequestPipeline{req: req}
}

// RunRequest 按序执行全部步骤并返回可查询的 RequestSnapshot。
func RunRequest(req *openrtb.BidRequest) (*RequestSnapshot, error) {
	return OfRequest(req).LightGate().Shared().Imps().Snapshot()
}

// LightGate 为第 1 步：轻量结构检查。
func (p *RequestPipeline) LightGate() *RequestPipeline {
	if p.err != nil {
		return p
	}
	if err := LightGateRequest(p.req); err != nil {
		p.err = err
		return p
	}
	p.gated = true
	return p
}

// Shared 为第 2 步：构建拍卖级 RequestSharedView。
func (p *RequestPipeline) Shared() *RequestPipeline {
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
	p.shared = shared(p.req)
	p.pinned = true
	return p
}

// Imps 为第 3 步：构建带 markup 位掩码的 ImpView 列表。
func (p *RequestPipeline) Imps() *RequestPipeline {
	if p.err != nil {
		return p
	}
	if !p.pinned {
		p.err = fmt.Errorf("pipeline: call shared first")
		return p
	}
	imps := make([]ImpView, 0, len(p.req.Imp))
	for _, imp := range p.req.Imp {
		imps = append(imps, viewImp(imp))
	}
	p.imps = imps
	p.viewed = true
	return p
}

// Snapshot 完成流水线（自动按序补跑未执行的步骤）。
func (p *RequestPipeline) Snapshot() (*RequestSnapshot, error) {
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
		p.Imps()
		if p.err != nil {
			return nil, p.err
		}
	}
	return &RequestSnapshot{Shared: p.shared, Imps: p.imps}, nil
}

// RequestSnapshot 是已完成请求流水线的可查询输出。
type RequestSnapshot struct {
	Shared RequestSharedView
	Imps   []ImpView
}

// AuctionID 返回拍卖 id（BidRequest.id）。
func (s *RequestSnapshot) AuctionID() string { return s.Shared.ID }

// AuctionType 返回拍卖类型（BidRequest.at）。
func (s *RequestSnapshot) AuctionType() int32 { return s.Shared.At }

// Inventory 返回库存面（site/app/dooh）；与 proto Content.Channel 不同。
func (s *RequestSnapshot) Inventory() Inventory { return s.Shared.Inventory }

// Currencies 返回可接受货币列表（BidRequest.cur）。
func (s *RequestSnapshot) Currencies() []string { return s.Shared.Cur }

// PastDeadline 报告 view 预算是否已超时。
func (s *RequestSnapshot) PastDeadline() bool { return s.Shared.PastDeadline() }

// Request 返回底层 BidRequest 指针。
func (s *RequestSnapshot) Request() *openrtb.BidRequest { return s.Shared.Req }

// FindImp 按 id 查找 ImpView，未找到时返回 nil。
func (s *RequestSnapshot) FindImp(id string) *ImpView {
	for i := range s.Imps {
		if s.Imps[i].ID == id {
			return &s.Imps[i]
		}
	}
	return nil
}

// ImpsWith 返回包含指定 markup 标志的 ImpView 列表。
func (s *RequestSnapshot) ImpsWith(flag MarkupMask) []ImpView {
	var out []ImpView
	for _, iv := range s.Imps {
		if iv.Markup.Has(flag) {
			out = append(out, iv)
		}
	}
	return out
}

// Facts 返回用于匹配/出价的紧凑 per-imp 摘要。
func (s *RequestSnapshot) Facts() []ImpFact {
	out := make([]ImpFact, 0, len(s.Imps))
	for _, iv := range s.Imps {
		out = append(out, factFrom(iv))
	}
	return out
}

// ImpFact 是从 ImpView 提取的扁平摘要，供调用方使用。
type ImpFact struct {
	ID            string
	Markup        MarkupMask
	Mtype         int32
	TagID         string
	BidFloor      float64
	BidFloorCur   string
	Secure        int32
	Instl         int32
	Rwdd          int32
	Ssai          int32
	BannerW       *int32
	BannerH       *int32
	NativeRequest string // 无 native 时为空
}

func factFrom(iv ImpView) ImpFact {
	f := ImpFact{
		ID:          iv.ID,
		Markup:      iv.Markup,
		Mtype:       iv.Markup.Mtype(),
		TagID:       iv.TagID,
		BidFloor:    iv.BidFloor,
		BidFloorCur: iv.BidFloorCur,
		Secure:      iv.Secure,
		Instl:       iv.Instl,
		Rwdd:        iv.Rwdd,
		Ssai:        iv.Ssai,
	}
	if iv.Banner != nil {
		if iv.Banner.W != 0 {
			w := iv.Banner.W
			f.BannerW = &w
		}
		if iv.Banner.H != 0 {
			h := iv.Banner.H
			f.BannerH = &h
		}
	}
	if iv.Native != nil && iv.Native.Request != "" {
		f.NativeRequest = iv.Native.Request
	}
	return f
}

// HasBanner 报告 ImpFact 是否包含 Banner markup。
func (f ImpFact) HasBanner() bool { return f.Markup.HasBanner() }

// HasVideo 报告 ImpFact 是否包含 Video markup。
func (f ImpFact) HasVideo() bool { return f.Markup.HasVideo() }

// HasAudio 报告 ImpFact 是否包含 Audio markup。
func (f ImpFact) HasAudio() bool { return f.Markup.HasAudio() }

// HasNative 报告 ImpFact 是否包含 Native markup。
func (f ImpFact) HasNative() bool { return f.Markup.HasNative() }

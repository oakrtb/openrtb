package view

import (
	"fmt"
	"strings"
	"time"

	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
)

// RequestSharedView 是对拍卖级 BidRequest 字段的零拷贝只读视图。
type RequestSharedView struct {
	Req *openrtb.BidRequest

	ID        string
	At        int32
	Cur       []string
	Tmax      int32
	Test      int32
	Inventory Inventory
	Deadline  time.Time // tmax 未设时为零；否则为 now + 85% tmax

	Site   *openrtb.Site
	App    *openrtb.App
	Dooh   *openrtb.Dooh
	Device *openrtb.Device
	User   *openrtb.User
	Regs   *openrtb.Regs
	Source *openrtb.Source

	Bcat []string
	Badv []string
	Bapp []string
}

// ImpView 是对单个 Imp 及其 markup 位掩码的零拷贝只读视图。
type ImpView struct {
	Imp *openrtb.Imp

	ID          string
	Markup      MarkupMask
	TagID       string
	BidFloor    float64
	BidFloorCur string
	Instl       int32
	Secure      int32
	Rwdd        int32
	Ssai        int32

	Banner *openrtb.Banner
	Video  *openrtb.Video
	Audio  *openrtb.Audio
	Native *openrtb.Native
	Pmp    *openrtb.Pmp
}


// MarkupMaskOf 返回 Imp 的 markup 位掩码（不做完整校验）。
func MarkupMaskOf(imp *openrtb.Imp) MarkupMask {
	if imp == nil {
		return MarkupNone
	}
	var f MarkupMask
	if imp.Banner != nil {
		f |= MarkupBanner
	}
	if imp.Video != nil {
		f |= MarkupVideo
	}
	if imp.Audio != nil {
		f |= MarkupAudio
	}
	if imp.Native != nil {
		f |= MarkupNative
	}
	return f
}

// LightGateRequest 执行轻量结构检查（非完整 JSON Schema）。
func LightGateRequest(req *openrtb.BidRequest) error {
	if req == nil {
		return fmt.Errorf("view: nil BidRequest")
	}
	if strings.TrimSpace(req.Id) == "" {
		return fmt.Errorf("view: BidRequest.id is required")
	}
	if req.At == 0 {
		return fmt.Errorf("view: BidRequest.at is required")
	}
	if len(req.Cur) == 0 {
		return fmt.Errorf("view: BidRequest.cur is required")
	}
	for i, c := range req.Cur {
		if strings.TrimSpace(c) == "" {
			return fmt.Errorf("view: BidRequest.cur[%d] is blank", i)
		}
	}
	if len(req.Imp) == 0 {
		return fmt.Errorf("view: BidRequest.imp requires at least one Imp")
	}
	n := 0
	if req.Site != nil {
		n++
	}
	if req.App != nil {
		n++
	}
	if req.Dooh != nil {
		n++
	}
	if n > 1 {
		return fmt.Errorf("view: site/app/dooh are mutually exclusive")
	}
	for i, imp := range req.Imp {
		if imp == nil {
			return fmt.Errorf("view: imp[%d] is nil", i)
		}
		if strings.TrimSpace(imp.Id) == "" {
			return fmt.Errorf("view: imp[%d].id is required", i)
		}
		if MarkupMaskOf(imp) == MarkupNone {
			return fmt.Errorf("view: imp[%d] needs banner, video, audio, or native", i)
		}
	}
	return nil
}


func shared(req *openrtb.BidRequest) RequestSharedView {
	ch := InventoryNone
	switch {
	case req.Site != nil:
		ch = InventorySite
	case req.App != nil:
		ch = InventoryApp
	case req.Dooh != nil:
		ch = InventoryDooh
	}
	var deadline time.Time
	if req.Tmax > 0 {
		deadline = time.Now().Add(time.Duration(req.Tmax) * time.Millisecond * 85 / 100)
	}
	return RequestSharedView{
		Req:       req,
		ID:        req.Id,
		At:        req.At,
		Cur:       req.Cur,
		Tmax:      req.Tmax,
		Test:      req.Test,
		Inventory: ch,
		Deadline:  deadline,
		Site:      req.Site,
		App:       req.App,
		Dooh:      req.Dooh,
		Device:    req.Device,
		User:      req.User,
		Regs:      req.Regs,
		Source:    req.Source,
		Bcat:      req.Bcat,
		Badv:      req.Badv,
		Bapp:      req.Bapp,
	}
}

func viewImp(imp *openrtb.Imp) ImpView {
	return ImpView{
		Imp:         imp,
		ID:          imp.Id,
		Markup:      MarkupMaskOf(imp),
		TagID:       imp.Tagid,
		BidFloor:    imp.Bidfloor,
		BidFloorCur: imp.Bidfloorcur,
		Instl:       imp.Instl,
		Secure:      imp.Secure,
		Rwdd:        imp.Rwdd,
		Ssai:        imp.Ssai,
		Banner:      imp.Banner,
		Video:       imp.Video,
		Audio:       imp.Audio,
		Native:      imp.Native,
		Pmp:         imp.Pmp,
	}
}

// PastDeadline 报告 view 预算（tmax 的 85%）是否已超时。
func (s RequestSharedView) PastDeadline() bool {
	return !s.Deadline.IsZero() && time.Now().After(s.Deadline)
}

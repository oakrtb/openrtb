package view

import (
	"fmt"
	"strings"

	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
)

// ResponseSharedView 是对拍卖级 BidResponse 字段的零拷贝只读视图。
type ResponseSharedView struct {
	Res *openrtb.BidResponse

	ID         string
	BidID      string
	Cur        string
	Nbr        int32
	Customdata string
	NoBid      bool // seatbid 为空时为 true（结构化或空 no-bid）
}

// SeatBidView 是一个 SeatBid 及其嵌套 BidView 列表。
type SeatBidView struct {
	SeatBid *openrtb.SeatBid

	Seat  string
	Group int32
	Bids  []BidView
}

// BidView 是对单个 Bid 及由 mtype 推导的 markup 的零拷贝只读视图。
type BidView struct {
	Bid *openrtb.Bid

	ID      string
	ImpID   string
	Seat    string // 父 seat
	Price   float64
	Markup  MarkupMask // 由 Bid.mtype 推导
	Mtype   int32      // 原始 MarkupType 数值 0–4
	Crid    string
	Cid     string
	DealID  string
	W       int32
	H       int32
	Dur     int32
	Adm     string
	NURL    string
	BURL    string
	LURL    string
	Adomain []string
}


// MarkupFromMtype 将 Bid.mtype / MarkupType 映射为 MarkupMask 位（或 MarkupNone）。
func MarkupFromMtype(m openrtb.MarkupType) MarkupMask {
	switch m {
	case openrtb.MarkupType_MARKUP_TYPE_BANNER:
		return MarkupBanner
	case openrtb.MarkupType_MARKUP_TYPE_VIDEO:
		return MarkupVideo
	case openrtb.MarkupType_MARKUP_TYPE_AUDIO:
		return MarkupAudio
	case openrtb.MarkupType_MARKUP_TYPE_NATIVE:
		return MarkupNative
	default:
		return MarkupNone
	}
}

// LightGateResponse 对 BidResponse 执行轻量结构检查。
func LightGateResponse(res *openrtb.BidResponse) error {
	if res == nil {
		return fmt.Errorf("view: nil BidResponse")
	}
	if strings.TrimSpace(res.Id) == "" {
		return fmt.Errorf("view: BidResponse.id is required")
	}
	if strings.TrimSpace(res.Cur) == "" {
		return fmt.Errorf("view: BidResponse.cur is required")
	}
	if len(res.Seatbid) == 0 {
		return nil // no-bid / 空体等价
	}
	for i, sb := range res.Seatbid {
		if sb == nil {
			return fmt.Errorf("view: seatbid[%d] is nil", i)
		}
		if len(sb.Bid) == 0 {
			return fmt.Errorf("view: seatbid[%d] needs at least one bid", i)
		}
		for j, bid := range sb.Bid {
			if bid == nil {
				return fmt.Errorf("view: seatbid[%d].bid[%d] is nil", i, j)
			}
			if strings.TrimSpace(bid.Id) == "" || strings.TrimSpace(bid.Impid) == "" {
				return fmt.Errorf("view: seatbid[%d].bid[%d] requires id and impid", i, j)
			}
			if bid.Price <= 0 {
				return fmt.Errorf("view: seatbid[%d].bid[%d].price must be > 0", i, j)
			}
		}
	}
	return nil
}


func responseShared(res *openrtb.BidResponse) ResponseSharedView {
	return ResponseSharedView{
		Res:        res,
		ID:         res.Id,
		BidID:      res.Bidid,
		Cur:        res.Cur,
		Nbr:        res.Nbr,
		Customdata: res.Customdata,
		NoBid:      len(res.Seatbid) == 0,
	}
}

func viewSeatBids(res *openrtb.BidResponse) ([]SeatBidView, []BidView) {
	seats := make([]SeatBidView, 0, len(res.Seatbid))
	var flat []BidView
	for _, sb := range res.Seatbid {
		sv := SeatBidView{SeatBid: sb, Seat: sb.Seat, Group: sb.Group}
		for _, bid := range sb.Bid {
			bv := viewBid(bid, sb.Seat)
			sv.Bids = append(sv.Bids, bv)
			flat = append(flat, bv)
		}
		seats = append(seats, sv)
	}
	return seats, flat
}

func viewBid(bid *openrtb.Bid, seat string) BidView {
	return BidView{
		Bid:     bid,
		ID:      bid.Id,
		ImpID:   bid.Impid,
		Seat:    seat,
		Price:   bid.Price,
		Markup:  MarkupFromMtype(bid.Mtype),
		Mtype:   int32(bid.Mtype),
		Crid:    bid.Crid,
		Cid:     bid.Cid,
		DealID:  bid.Dealid,
		W:       bid.W,
		H:       bid.H,
		Dur:     bid.Dur,
		Adm:     bid.Adm,
		NURL:    bid.Nurl,
		BURL:    bid.Burl,
		LURL:    bid.Lurl,
		Adomain: bid.Adomain,
	}
}

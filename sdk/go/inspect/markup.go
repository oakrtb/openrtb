package inspect

// MarkupMask 是 Imp 上 markup 类型（banner/video/audio/native）的位掩码。
// 注意：与 oakrtb.v2.Format（Banner 尺寸条目）不是同一概念。
type MarkupMask uint8

const (
	// MarkupNone 表示未设置任何 markup 类型。
	MarkupNone MarkupMask = 0
	// MarkupBanner 对应 imp.banner，Bid.mtype 为 1。
	MarkupBanner MarkupMask = 1 << 0
	// MarkupVideo 对应 imp.video，Bid.mtype 为 2。
	MarkupVideo MarkupMask = 1 << 1
	// MarkupAudio 对应 imp.audio，Bid.mtype 为 3。
	MarkupAudio MarkupMask = 1 << 2
	// MarkupNative 对应 imp.native，Bid.mtype 为 4。
	MarkupNative MarkupMask = 1 << 3
)

// Has 报告位掩码是否包含指定标志。
func (f MarkupMask) Has(flag MarkupMask) bool { return f&flag != 0 }

// HasBanner 报告是否包含 Banner markup。
func (f MarkupMask) HasBanner() bool { return f.Has(MarkupBanner) }

// HasVideo 报告是否包含 Video markup。
func (f MarkupMask) HasVideo() bool { return f.Has(MarkupVideo) }

// HasAudio 报告是否包含 Audio markup。
func (f MarkupMask) HasAudio() bool { return f.Has(MarkupAudio) }

// HasNative 报告是否包含 Native markup。
func (f MarkupMask) HasNative() bool { return f.Has(MarkupNative) }

// Count 返回位掩码中已置位的 markup 类型数量。
func (f MarkupMask) Count() int {
	n := 0
	for x := f; x != 0; x >>= 1 {
		n += int(x & 1)
	}
	return n
}

// Primary 在恰好一种 markup 置位时返回该位；否则返回 MarkupNone。
func (f MarkupMask) Primary() MarkupMask {
	if f.Count() == 1 {
		return f
	}
	return MarkupNone
}

// Mtype 将单一 markup 位映射为 Bid.mtype（1–4）；多选或空时返回 0。
func (f MarkupMask) Mtype() int32 {
	switch f.Primary() {
	case MarkupBanner:
		return 1
	case MarkupVideo:
		return 2
	case MarkupAudio:
		return 3
	case MarkupNative:
		return 4
	default:
		return 0
	}
}

// Inventory 表示 BidRequest 的库存面（site/app/dooh，互斥）。
// 注意：与 proto Content.Channel（内容分发渠道）不是同一概念。
type Inventory uint8

const (
	// InventoryNone 表示未设置 site/app/dooh。
	InventoryNone Inventory = iota
	// InventorySite 表示网站库存（BidRequest.site）。
	InventorySite
	// InventoryApp 表示应用库存（BidRequest.app）。
	InventoryApp
	// InventoryDooh 表示数字户外库存（BidRequest.dooh）。
	InventoryDooh
)

// String 返回库存面的短名称（site/app/dooh/none）。
func (c Inventory) String() string {
	switch c {
	case InventorySite:
		return "site"
	case InventoryApp:
		return "app"
	case InventoryDooh:
		return "dooh"
	default:
		return "none"
	}
}

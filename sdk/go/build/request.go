package build

import (
	"fmt"
	"strings"

	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
	"github.com/oakrtb/openrtb/sdk/go/schema"
)

// BidRequestBuilder 组装带库存与展示位的 BidRequest。
type BidRequestBuilder struct {
	req *openrtb.BidRequest
	err error
}

// NewBidRequest 创建请求构建器；id 为必填（拍卖标识）。
func NewBidRequest(id string) *BidRequestBuilder {
	b := &BidRequestBuilder{req: &openrtb.BidRequest{Id: id}}
	if strings.TrimSpace(id) == "" {
		b.err = fmt.Errorf("build: BidRequest.id is required")
	}
	return b
}

// FirstPrice 设置 at=1（第一价格拍卖）。
func (b *BidRequestBuilder) FirstPrice() *BidRequestBuilder {
	return b.AuctionType(int32(openrtb.AuctionType_AUCTION_TYPE_FIRST_PRICE))
}

// SecondPricePlus 设置 at=2（OpenRTB 默认拍卖类型）。
func (b *BidRequestBuilder) SecondPricePlus() *BidRequestBuilder {
	return b.AuctionType(int32(openrtb.AuctionType_AUCTION_TYPE_SECOND_PRICE_PLUS))
}

// AuctionType 设置 BidRequest.at。
func (b *BidRequestBuilder) AuctionType(at int32) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.At = at
	return b
}

// Tmax 设置超时毫秒数（含网络延迟）。
func (b *BidRequestBuilder) Tmax(ms int32) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Tmax = ms
	return b
}

// Currency 设置可接受的出价货币（ISO-4217），替换已有列表。
func (b *BidRequestBuilder) Currency(codes ...string) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Cur = append([]string{}, codes...)
	return b
}

// Test 标记为测试流量（test=1）。
func (b *BidRequestBuilder) Test() *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Test = 1
	return b
}

// Bcat 设置屏蔽的广告主类别。
func (b *BidRequestBuilder) Bcat(cats ...string) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Bcat = append([]string{}, cats...)
	return b
}

// Badv 设置屏蔽的广告主域名。
func (b *BidRequestBuilder) Badv(domains ...string) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Badv = append([]string{}, domains...)
	return b
}

// Site 设置网站库存（清除 app/dooh）。
func (b *BidRequestBuilder) Site(site *openrtb.Site) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Site = site
	b.req.App = nil
	b.req.Dooh = nil
	return b
}

// App 设置应用库存（清除 site/dooh）。
func (b *BidRequestBuilder) App(app *openrtb.App) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.App = app
	b.req.Site = nil
	b.req.Dooh = nil
	return b
}

// Dooh 设置数字户外库存（清除 site/app）。
func (b *BidRequestBuilder) Dooh(dooh *openrtb.Dooh) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Dooh = dooh
	b.req.Site = nil
	b.req.App = nil
	return b
}

// Device 设置设备上下文。
func (b *BidRequestBuilder) Device(device *openrtb.Device) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Device = device
	return b
}

// User 设置用户/受众上下文。
func (b *BidRequestBuilder) User(user *openrtb.User) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.User = user
	return b
}

// Regs 设置隐私/法规信号。
func (b *BidRequestBuilder) Regs(regs *openrtb.Regs) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Regs = regs
	return b
}

// Source 设置上游来源/供应链。
func (b *BidRequestBuilder) Source(source *openrtb.Source) *BidRequestBuilder {
	if b.err != nil {
		return b
	}
	b.req.Source = source
	return b
}

// AddImp 追加一个展示机会。
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

// Build 在结构校验后返回 BidRequest（需 id 及至少一个带格式的 imp）。
func (b *BidRequestBuilder) Build() (*openrtb.BidRequest, error) {
	if b.err != nil {
		return nil, b.err
	}
	if strings.TrimSpace(b.req.Id) == "" {
		return nil, fmt.Errorf("build: BidRequest.id is required")
	}
	if b.req.At == 0 {
		return nil, fmt.Errorf("build: BidRequest.at is required")
	}
	if len(b.req.Cur) == 0 {
		return nil, fmt.Errorf("build: BidRequest.cur is required (at least one ISO-4217 code)")
	}
	for i, c := range b.req.Cur {
		if strings.TrimSpace(c) == "" {
			return nil, fmt.Errorf("build: BidRequest.cur[%d] is blank", i)
		}
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

// MustBuild 同 Build，出错时 panic。
func (b *BidRequestBuilder) MustBuild() *openrtb.BidRequest {
	req, err := b.Build()
	if err != nil {
		panic(err)
	}
	return req
}

// BuildJSON 构建并序列化为 OpenRTB JSON。
func (b *BidRequestBuilder) BuildJSON() ([]byte, error) {
	req, err := b.Build()
	if err != nil {
		return nil, err
	}
	return MarshalJSON(req)
}

// BuildValidated 构建、序列化并执行 JSON Schema 校验。
func (b *BidRequestBuilder) BuildValidated() ([]byte, schema.Report, error) {
	raw, err := b.BuildJSON()
	if err != nil {
		return nil, schema.Report{}, err
	}
	return raw, schema.Request(raw), nil
}

func checkImp(imp *openrtb.Imp, i int) error {
	if imp == nil {
		return fmt.Errorf("build: imp[%d] is nil", i)
	}
	if strings.TrimSpace(imp.Id) == "" {
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
		if strings.TrimSpace(imp.Native.Request) == "" {
			return fmt.Errorf("build: imp[%d].native.request is required", i)
		}
	}
	if formats == 0 {
		return fmt.Errorf("build: imp[%d] needs banner, video, audio, or native", i)
	}
	return nil
}

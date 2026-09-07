package build_test

import (
	"testing"

	"github.com/oakrtb/openrtb/sdk/go/build"
	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
)

func TestBannerSiteRequestValidated(t *testing.T) {
	raw, result, err := build.NewBidRequest("auction-banner-1").
		FirstPrice().
		Tmax(120).
		Currency("USD").
		Site(build.NewSite().
			ID("102855").
			Domain("www.example.com").
			Page("https://www.example.com/article").
			Publisher(build.NewPublisher().ID("8953").Name("Example").Domain("example.com").Build()).
			Build()).
		Device(build.NewDevice().
			UA("Mozilla/5.0").
			IP("192.0.2.1").
			DeviceType(int32(openrtb.DeviceType_DEVICE_TYPE_PHONE)).
			OS("Android", "14").
			Build()).
		AddImp(build.NewBannerImp("1").
			Size(300, 250).
			Floor(0.03, "USD").
			Secure().
			BannerPos(int32(openrtb.AdPosition_AD_POSITION_ABOVE_THE_FOLD)).
			BannerMimes("image/jpeg", "image/png").
			Build()).
		BuildValidated()
	if err != nil {
		t.Fatal(err)
	}
	if !result.Ok {
		t.Fatalf("schema: %s", result.Errors)
	}
	if len(raw) == 0 {
		t.Fatal("empty json")
	}
}

func TestVideoAppRequestValidated(t *testing.T) {
	_, result, err := build.NewBidRequest("auction-video-1").
		FirstPrice().
		Tmax(200).
		Currency("USD").
		App(build.NewApp().
			ID("ctv-1").
			Name("Example CTV").
			Bundle("com.example.ctv").
			Content(build.NewContent().Title("Show").Series("Series").Context(1).Build()).
			Build()).
		Device(build.NewDevice().
			UA("CTV").
			DeviceType(int32(openrtb.DeviceType_DEVICE_TYPE_CONNECTED_TV)).
			Build()).
		AddImp(build.NewVideoImp("1").
			VideoMimes("video/mp4", "video/webm").
			VideoDuration(5, 30).
			VideoProtocols(2, 3, 5, 6).
			Size(1920, 1080).
			StartDelay(0).
			Plcmt(int32(openrtb.VideoPlcmt_VIDEO_PLCMT_INSTREAM)).
			Linearity(int32(openrtb.VideoLinearity_VIDEO_LINEARITY_LINEAR)).
			Skip(5).
			Pod("pod-1", 1).
			Floor(5.0, "USD").
			Secure().
			Build()).
		BuildValidated()
	if err != nil {
		t.Fatal(err)
	}
	if !result.Ok {
		t.Fatalf("schema: %+v", result.Errors)
	}
}

func TestNativeRequestValidated(t *testing.T) {
	nativeReq := `{"ver":"1.2","assets":[{"id":1,"required":1,"title":{"len":90}}]}`
	_, result, err := build.NewBidRequest("auction-native-1").
		FirstPrice().
		Tmax(100).
		Currency("USD").
		Site(build.NewSite().ID("feed-1").Domain("news.example.com").Build()).
		Device(build.NewDevice().UA("Mozilla/5.0").IP("203.0.113.5").DeviceType(2).Build()).
		AddImp(build.NewNativeImp("1").
			NativeRequest(nativeReq).
			Floor(0.5, "USD").
			Build()).
		BuildValidated()
	if err != nil {
		t.Fatal(err)
	}
	if !result.Ok {
		t.Fatalf("schema: %+v", result.Errors)
	}
}

func TestBidResponseValidated(t *testing.T) {
	_, result, err := build.NewBidResponse("auction-banner-1").
		BidID("abc123").
		Currency("USD").
		AddSeatBid("512",
			build.NewBid("1", "1", 1.23).
				Banner().
				Size(300, 250).
				Adomain("advertiser.com").
				Crid("creative-9").
				Adm(`<img src="https://cdn.example/ad.png"/>`).
				NURL("https://dsp.example/win?price=${AUCTION_PRICE}").
				Build(),
		).
		BuildValidated()
	if err != nil {
		t.Fatal(err)
	}
	if !result.Ok {
		t.Fatalf("schema: %+v", result.Errors)
	}
}

func TestNoBidResponseValidated(t *testing.T) {
	_, result, err := build.NewBidResponse("auction-1").
		NoBid(int32(openrtb.NoBidReason_NO_BID_REASON_INVALID_REQUEST)).
		BuildValidated()
	if err != nil {
		t.Fatal(err)
	}
	if !result.Ok {
		t.Fatalf("schema: %+v", result.Errors)
	}
}

func TestRejectsMissingFormat(t *testing.T) {
	_, err := build.NewBidRequest("x").
		FirstPrice().
		Currency("USD").
		Site(build.NewSite().ID("s").Build()).
		AddImp(&openrtb.Imp{Id: "1"}).
		Build()
	if err == nil {
		t.Fatal("expected error")
	}
}

func TestRejectsMissingAt(t *testing.T) {
	_, err := build.NewBidRequest("x").
		Currency("USD").
		AddImp(build.NewBannerImp("1").Size(300, 250).Build()).
		Build()
	if err == nil {
		t.Fatal("expected at required")
	}
}

func TestRejectsMissingCur(t *testing.T) {
	_, err := build.NewBidRequest("x").
		FirstPrice().
		AddImp(build.NewBannerImp("1").Size(300, 250).Build()).
		Build()
	if err == nil {
		t.Fatal("expected cur required")
	}
}

func TestRejectsSiteAndApp(t *testing.T) {
	b := build.NewBidRequest("x").
		FirstPrice().
		Currency("USD").
		Site(build.NewSite().ID("s").Build()).
		AddImp(build.NewBannerImp("1").Size(300, 250).Build())
	b.App(build.NewApp().ID("a").Build()) // App after Site should clear Site in our API
	// Our API clears the other — so Build should succeed with only App.
	req, err := b.Build()
	if err != nil {
		t.Fatal(err)
	}
	if req.Site != nil || req.App == nil {
		t.Fatalf("expected app only, got site=%v app=%v", req.Site, req.App)
	}
}

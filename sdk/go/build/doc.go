// Package build provides fluent builders for OakRTB BidRequest / BidResponse.
//
// Builders assemble protobuf models with the fields each ad format needs, then
// MarshalJSON emits OpenRTB-compatible JSON (enum as numbers). Prefer
// BuildValidated to run JSON Schema checks before sending.
//
// Typical flow:
//
//	req := build.NewBidRequest("auction-1").
//		FirstPrice().
//		Tmax(120).
//		Currency("USD").
//		Site(build.NewSite().ID("s1").Domain("example.com").Page("https://example.com/a")).
//		Device(build.NewDevice().UA("Mozilla/5.0").IP("192.0.2.1").DeviceType(4)).
//		AddImp(build.NewBannerImp("1").Size(300, 250).Floor(0.03, "USD").Secure()).
//		MustBuild()
//	raw, err := build.MarshalJSON(req)
package build

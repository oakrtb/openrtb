// Package build 提供 OakRTB BidRequest / BidResponse 的流式构建器。
//
// 构建器组装 protobuf 模型并填充各广告格式所需字段，MarshalJSON 输出
// OpenRTB 兼容 JSON（枚举为数字）。发送前优先使用 BuildValidated 做 JSON Schema 校验。
//
// 典型流程：
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

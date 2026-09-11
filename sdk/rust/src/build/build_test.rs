use crate::build::{
    AppBuilder, BannerImpBuilder, BidBuilder, BidRequestBuilder, BidResponseBuilder, ContentBuilder,
    DeviceBuilder, NativeImpBuilder, PublisherBuilder, SiteBuilder, VideoImpBuilder,
};

#[test]
fn banner_site_request_validates() {
    let v = BidRequestBuilder::new("auction-banner-1")
        .first_price()
        .tmax(120)
        .currency(&["USD"])
        .site(
            SiteBuilder::new()
                .id("102855")
                .domain("www.example.com")
                .page("https://www.example.com/article")
                .publisher(
                    PublisherBuilder::new()
                        .id("8953")
                        .name("Example")
                        .domain("example.com")
                        .build(),
                )
                .build(),
        )
        .device(
            DeviceBuilder::new()
                .ua("Mozilla/5.0")
                .ip("192.0.2.1")
                .device_type(4)
                .os("Android", "14")
                .build(),
        )
        .add_imp(
            BannerImpBuilder::new("1")
                .size(300, 250)
                .floor(0.03, "USD")
                .secure()
                .pos(1)
                .mimes(&["image/jpeg", "image/png"])
                .build(),
        )
        .build_validated()
        .expect("build");
    assert!(v.ok(), "{:?}", v.result);
}

#[test]
fn video_app_request_validates() {
    let v = BidRequestBuilder::new("auction-video-1")
        .first_price()
        .tmax(200)
        .currency(&["USD"])
        .app(
            AppBuilder::new()
                .id("ctv-1")
                .name("Example CTV")
                .bundle("com.example.ctv")
                .content(
                    ContentBuilder::new()
                        .title("Show")
                        .series("Series")
                        .context(1)
                        .build(),
                )
                .build(),
        )
        .device(DeviceBuilder::new().ua("CTV").device_type(3).build())
        .add_imp(
            VideoImpBuilder::new("1")
                .mimes(&["video/mp4", "video/webm"])
                .duration(5, 30)
                .protocols(&[2, 3, 5, 6])
                .size(1920, 1080)
                .start_delay(0)
                .plcmt(1)
                .linearity(1)
                .skip(5)
                .pod("pod-1", 1)
                .floor(5.0, "USD")
                .secure()
                .build(),
        )
        .build_validated()
        .expect("build");
    assert!(v.ok(), "{:?}", v.result);
}

#[test]
fn native_request_validates() {
    let native_req = r#"{"ver":"1.2","assets":[{"id":1,"required":1,"title":{"len":90}}]}"#;
    let v = BidRequestBuilder::new("auction-native-1")
        .first_price()
        .tmax(100)
        .currency(&["USD"])
        .site(
            SiteBuilder::new()
                .id("feed-1")
                .domain("news.example.com")
                .build(),
        )
        .device(
            DeviceBuilder::new()
                .ua("Mozilla/5.0")
                .ip("203.0.113.5")
                .device_type(2)
                .build(),
        )
        .add_imp(
            NativeImpBuilder::new("1")
                .request(native_req)
                .floor(0.5, "USD")
                .build(),
        )
        .build_validated()
        .expect("build");
    assert!(v.ok(), "{:?}", v.result);
}

#[test]
fn bid_response_validates() {
    let v = BidResponseBuilder::new("auction-banner-1")
        .bid_id("abc123")
        .currency("USD")
        .add_seat_bid(
            "512",
            vec![BidBuilder::new("1", "1", 1.23)
                .banner()
                .size(300, 250)
                .adomain(&["advertiser.com"])
                .crid("creative-9")
                .adm(r#"<img src="https://cdn.example/ad.png"/>"#)
                .nurl("https://dsp.example/win?price=${AUCTION_PRICE}")
                .build()],
        )
        .build_validated()
        .expect("build");
    assert!(v.ok(), "{:?}", v.result);
}

#[test]
fn no_bid_validates() {
    let v = BidResponseBuilder::new("auction-1")
        .no_bid(2)
        .build_validated()
        .expect("build");
    assert!(v.ok(), "{:?}", v.result);
}

#[test]
fn add_seat_bid_clears_no_bid() {
    let v = BidResponseBuilder::new("auction-1")
        .no_bid(2)
        .add_seat_bid(
            "512",
            vec![BidBuilder::new("1", "1", 1.0).banner().build()],
        )
        .build()
        .expect("build");
    assert!(v.get("nbr").is_none());
    assert_eq!(v["seatbid"].as_array().unwrap().len(), 1);
}

#[test]
fn no_bid_clears_seat_bid() {
    let v = BidResponseBuilder::new("auction-1")
        .add_seat_bid("512", vec![BidBuilder::new("1", "1", 1.0).build()])
        .no_bid(7)
        .build()
        .expect("build");
    assert_eq!(v["nbr"], 7);
    assert!(v.get("seatbid").is_none() || v["seatbid"].as_array().unwrap().is_empty());
}

#[test]
fn bid_response_build_empty_id() {
    let err = BidResponseBuilder::new("").build().unwrap_err();
    assert!(err.contains("BidResponse.id"));
}

#[test]
fn bid_response_build_needs_seatbid_or_no_bid() {
    let err = BidResponseBuilder::new("auction-1")
        .build()
        .unwrap_err();
    assert!(err.contains("seatbid") || err.contains("noBid"));
}

#[test]
fn bid_response_build_empty_seat_bids() {
    let err = BidResponseBuilder::new("auction-1")
        .add_seat_bid("512", vec![])
        .build()
        .unwrap_err();
    assert!(err.contains("SeatBid"));
}

#[test]
fn bid_response_build_zero_price() {
    let err = BidResponseBuilder::new("auction-1")
        .add_seat_bid(
            "512",
            vec![BidBuilder::new("1", "1", 0.0).banner().build()],
        )
        .build()
        .unwrap_err();
    assert!(err.contains("price"));
}

#[test]
fn bid_response_build_missing_impid() {
    let mut bid = BidBuilder::new("1", "1", 1.0).banner().build();
    bid.as_object_mut().unwrap().insert("impid".into(), serde_json::json!(""));
    let err = BidResponseBuilder::new("auction-1")
        .add_seat_bid("512", vec![bid])
        .build()
        .unwrap_err();
    assert!(err.contains("impid"));
}

#[test]
fn bid_request_build_missing_at() {
    let err = BidRequestBuilder::new("auction-1")
        .currency(&["USD"])
        .add_imp(BannerImpBuilder::new("1").size(300, 250).build())
        .build()
        .unwrap_err();
    assert!(err.contains("at"));
}

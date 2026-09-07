package com.oakrtb.sdk.build;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BuilderTest {
  @Test
  void bannerSiteRequestValidates() {
    var payload =
        BidRequestBuilder.create("auction-banner-1")
            .firstPrice()
            .tmax(120)
            .currency("USD")
            .site(
                Parts.site()
                    .id("102855")
                    .domain("www.example.com")
                    .page("https://www.example.com/article")
                    .publisher(Parts.publisher().id("8953").name("Example").domain("example.com").build())
                    .build())
            .device(Parts.device().ua("Mozilla/5.0").ip("192.0.2.1").deviceType(4).os("Android", "14").build())
            .addImp(
                ImpBuilders.banner("1")
                    .size(300, 250)
                    .floor(0.03, "USD")
                    .secure()
                    .pos(1)
                    .mimes("image/jpeg", "image/png")
                    .build())
            .buildValidated();
    assertTrue(payload.ok(), () -> payload.result().toJson());
  }

  @Test
  void videoAppRequestValidates() {
    var payload =
        BidRequestBuilder.create("auction-video-1")
            .firstPrice()
            .tmax(200)
            .currency("USD")
            .app(
                Parts.app()
                    .id("ctv-1")
                    .name("Example CTV")
                    .bundle("com.example.ctv")
                    .content(Parts.content().title("Show").series("Series").context(1).build())
                    .build())
            .device(Parts.device().ua("CTV").deviceType(3).build())
            .addImp(
                ImpBuilders.video("1")
                    .mimes("video/mp4", "video/webm")
                    .duration(5, 30)
                    .protocols(2, 3, 5, 6)
                    .size(1920, 1080)
                    .startDelay(0)
                    .plcmt(1)
                    .linearity(1)
                    .skip(5)
                    .pod("pod-1", 1)
                    .floor(5.0, "USD")
                    .secure()
                    .build())
            .buildValidated();
    assertTrue(payload.ok(), () -> payload.result().toJson());
  }

  @Test
  void bidResponseValidates() {
    var payload =
        BidResponseBuilder.create("auction-banner-1")
            .bidId("abc123")
            .currency("USD")
            .addSeatBid(
                "512",
                BidResponseBuilder.bid("1", "1", 1.23)
                    .banner()
                    .size(300, 250)
                    .adomain("advertiser.com")
                    .crid("creative-9")
                    .adm("<img src=\"https://cdn.example/ad.png\"/>")
                    .nurl("https://dsp.example/win?price=${AUCTION_PRICE}")
                    .build())
            .buildValidated();
    assertTrue(payload.ok(), () -> payload.result().toJson());
  }

  @Test
  void noBidValidates() {
    var payload = BidResponseBuilder.create("auction-1").noBid(2).buildValidated();
    assertTrue(payload.ok(), () -> payload.result().toJson());
  }
}

package com.oakrtb.sdk.inspect;

import com.oakrtb.openrtb.v2.Banner;
import com.oakrtb.openrtb.v2.BidRequest;
import com.oakrtb.openrtb.v2.Imp;
import com.oakrtb.openrtb.v2.Video;
import com.oakrtb.sdk.build.BidRequestBuilder;
import com.oakrtb.sdk.build.ImpBuilders;
import com.oakrtb.sdk.build.Parts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestInspectTest {
  @Test
  void inspectBannerRequest() {
    BidRequest req =
        BidRequestBuilder.create("auction-1")
            .firstPrice()
            .tmax(120)
            .currency("USD")
            .site(Parts.site().id("s1").domain("example.com").page("https://example.com/a").build())
            .device(Parts.device().ua("Mozilla/5.0").ip("192.0.2.1").deviceType(4).build())
            .addImp(ImpBuilders.banner("1").size(300, 250).floor(0.03, "USD").secure().build())
            .build();

    RequestInspect.Result res = RequestInspect.inspect(req);
    assertEquals("auction-1", res.shared().id());
    assertEquals(1, res.shared().at());
    assertEquals(Inventory.SITE, res.shared().inventory());
    assertNotNull(res.shared().deadline());
    assertNotNull(res.shared().site());
    assertNotNull(res.shared().device());
    assertEquals(1, res.imps().size());
    RequestInspect.ImpView iv = res.imps().get(0);
    assertTrue(iv.markup().hasBanner());
    assertEquals(1, iv.markup().count());
    assertEquals(1, iv.markup().mtype());
    assertEquals(300, iv.banner().getW());
    assertEquals(1, iv.secure());
    assertEquals(0.03, iv.bidFloor(), 1e-9);
  }

  @Test
  void multiFormatAppInventory() {
    Imp multi =
        Imp.newBuilder()
            .setId("1")
            .setBanner(Banner.newBuilder().setW(320).setH(50).build())
            .setVideo(Video.newBuilder().addMimes("video/mp4").build())
            .build();
    BidRequest req =
        BidRequest.newBuilder()
            .setId("m")
            .setAt(2)
            .addCur("USD")
            .setApp(Parts.app().id("a1").bundle("com.example.app").build())
            .addImp(multi)
            .build();

    RequestInspect.Result res = RequestInspect.inspect(req);
    assertEquals(Inventory.APP, res.shared().inventory());
    MarkupMask f = res.imps().get(0).markup();
    assertTrue(f.hasBanner() && f.hasVideo());
    assertEquals(2, f.count());
    assertEquals(0, f.mtype());
  }

  @Test
  void lightGateRejectsSiteAppMutex() {
    BidRequest both =
        BidRequest.newBuilder()
            .setId("x")
            .setAt(1)
            .addCur("USD")
            .setSite(Parts.site().id("s").build())
            .setApp(Parts.app().id("a").build())
            .addImp(ImpBuilders.banner("1").size(1, 1).build())
            .build();
    assertThrows(IllegalArgumentException.class, () -> RequestInspect.lightGate(both));
  }
}

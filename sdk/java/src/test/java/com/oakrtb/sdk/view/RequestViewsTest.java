package com.oakrtb.sdk.view;

import com.oakrtb.openrtb.v2.Banner;
import com.oakrtb.openrtb.v2.BidRequest;
import com.oakrtb.openrtb.v2.Imp;
import com.oakrtb.openrtb.v2.Video;
import com.oakrtb.sdk.build.BidRequestBuilder;
import com.oakrtb.sdk.build.ImpBuilders;
import com.oakrtb.sdk.build.Parts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestViewsTest {
  @Test
  void runBannerRequest() {
    BidRequest req =
        BidRequestBuilder.create("auction-1")
            .firstPrice()
            .tmax(120)
            .currency("USD")
            .site(Parts.site().id("s1").domain("example.com").page("https://example.com/a").build())
            .device(Parts.device().ua("Mozilla/5.0").ip("192.0.2.1").deviceType(4).build())
            .addImp(ImpBuilders.banner("1").size(300, 250).floor(0.03, "USD").secure().build())
            .build();

    RequestPipeline.Snapshot snap = RequestPipeline.run(req);
    assertEquals("auction-1", snap.shared().id());
    assertEquals(1, snap.shared().at());
    assertEquals(Inventory.SITE, snap.shared().inventory());
    assertNotNull(snap.shared().deadline());
    assertNotNull(snap.shared().site());
    assertNotNull(snap.shared().device());
    assertEquals(1, snap.imps().size());
    RequestViews.ImpView iv = snap.imps().get(0);
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

    RequestPipeline.Snapshot snap = RequestPipeline.run(req);
    assertEquals(Inventory.APP, snap.shared().inventory());
    MarkupMask f = snap.imps().get(0).markup();
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
    assertThrows(IllegalArgumentException.class, () -> RequestViews.lightGate(both));
  }

  @Test
  void lightGateRejectsBlank() {
    BidRequest base =
        BidRequestBuilder.create("x")
            .firstPrice()
            .currency("USD")
            .addImp(ImpBuilders.banner("1").size(1, 1).build())
            .build();
    assertThrows(
        IllegalArgumentException.class,
        () -> RequestViews.lightGate(base.toBuilder().setId("   ").build()));
    assertThrows(
        IllegalArgumentException.class,
        () -> RequestViews.lightGate(base.toBuilder().clearCur().addCur("   ").build()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            RequestViews.lightGate(
                base.toBuilder()
                    .setImp(0, base.getImp(0).toBuilder().setId("\t").build())
                    .build()));
  }
}

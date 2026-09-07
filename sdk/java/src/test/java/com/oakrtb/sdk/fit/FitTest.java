package com.oakrtb.sdk.fit;

import com.oakrtb.openrtb.v2.Banner;
import com.oakrtb.openrtb.v2.Bid;
import com.oakrtb.openrtb.v2.BidRequest;
import com.oakrtb.openrtb.v2.BidResponse;
import com.oakrtb.openrtb.v2.Imp;
import com.oakrtb.openrtb.v2.MarkupType;
import com.oakrtb.openrtb.v2.Video;
import com.oakrtb.sdk.build.BidRequestBuilder;
import com.oakrtb.sdk.build.BidResponseBuilder;
import com.oakrtb.sdk.build.ImpBuilders;
import com.oakrtb.sdk.build.Parts;
import com.oakrtb.sdk.inspect.MarkupMask;
import com.oakrtb.sdk.inspect.RequestPipeline;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FitTest {
  private static RequestPipeline.Snapshot bannerSnap() {
    BidRequest req =
        BidRequestBuilder.create("a1")
            .firstPrice()
            .currency("USD")
            .site(Parts.site().id("s1").build())
            .addImp(ImpBuilders.banner("1").size(300, 250).floor(1.0, "USD").build())
            .build();
    return RequestPipeline.run(req);
  }

  @Test
  void impReadyVideoMissingMimes() {
    Imp imp =
        Imp.newBuilder()
            .setId("1")
            .setVideo(Video.newBuilder().build())
            .build();
    BidRequest req =
        BidRequest.newBuilder().setId("x").setAt(1).addCur("USD").addImp(imp).build();
    var snap = RequestPipeline.run(req);
    FitResult r = Fit.impReady(snap.imps().get(0), 2);
    assertFalse(r.ok());
    assertTrue(r.has(IssueCode.VIDEO_MIMES_MISSING));
  }

  @Test
  void multiFormatNeedsMtype() {
    Imp multi =
        Imp.newBuilder()
            .setId("1")
            .setBanner(Banner.newBuilder().setW(320).setH(50).build())
            .setVideo(Video.newBuilder().addMimes("video/mp4").build())
            .build();
    BidRequest req =
        BidRequest.newBuilder().setId("m").setAt(1).addCur("USD").addImp(multi).build();
    var snap = RequestPipeline.run(req);
    Bid bid = Bid.newBuilder().setId("b1").setImpid("1").setPrice(2.0).build();
    FitResult r = Fit.bidFit(snap, bid);
    assertFalse(r.ok());
    assertTrue(r.has(IssueCode.MTYPE_REQUIRED));
  }

  @Test
  void mtypeMismatch() {
    var snap = bannerSnap();
    Bid bid =
        Bid.newBuilder()
            .setId("b1")
            .setImpid("1")
            .setPrice(2.0)
            .setMtype(MarkupType.MARKUP_TYPE_VIDEO)
            .build();
    FitResult r = Fit.bidFit(snap, bid);
    assertFalse(r.ok());
    assertTrue(r.has(IssueCode.MTYPE_MISMATCH));
  }

  @Test
  void priceBelowFloorIsWarn() {
    var snap = bannerSnap();
    Bid bid = BidResponseBuilder.bid("b1", "1", 0.5).banner().build();
    FitResult r = Fit.bidFit(snap, bid);
    assertTrue(r.ok());
    assertTrue(r.has(IssueCode.PRICE_BELOW_FLOOR));
  }

  @Test
  void attrBlockedIsWarn() {
    Imp imp =
        Imp.newBuilder()
            .setId("1")
            .setBanner(Banner.newBuilder().setW(300).setH(250).addBattr(1).build())
            .build();
    BidRequest req =
        BidRequest.newBuilder().setId("x").setAt(1).addCur("USD").addImp(imp).build();
    var snap = RequestPipeline.run(req);
    Bid bid =
        Bid.newBuilder()
            .setId("b1")
            .setImpid("1")
            .setPrice(2.0)
            .setMtype(MarkupType.MARKUP_TYPE_BANNER)
            .addAttr(1)
            .build();
    FitResult r = Fit.bidFit(snap, bid);
    assertTrue(r.ok());
    assertTrue(r.has(IssueCode.ATTR_BLOCKED));
  }

  @Test
  void noBidResponseFitOk() {
    var snap = bannerSnap();
    BidResponse res = BidResponseBuilder.create("a1").currency("USD").noBid(0).build();
    FitResult r = Fit.responseFit(snap, res);
    assertTrue(r.ok());
  }

  @Test
  void impidNotFound() {
    var snap = bannerSnap();
    Bid bid = BidResponseBuilder.bid("b1", "missing", 2.0).banner().build();
    FitResult r = Fit.bidFit(snap, bid);
    assertFalse(r.ok());
    assertTrue(r.has(IssueCode.IMP_NOT_FOUND));
  }

  @Test
  void impReadyFormatBit() {
    var snap = bannerSnap();
    FitResult r = Fit.impReady(snap.imps().get(0), MarkupMask.of(MarkupMask.BANNER));
    assertTrue(r.ok());
  }

  @Test
  void responseFitHappyPath() {
    var snap = bannerSnap();
    BidResponse res =
        BidResponseBuilder.create("a1")
            .currency("USD")
            .addSeatBid("", BidResponseBuilder.bid("b1", "1", 2.0).banner().adm("<a/>").build())
            .build();
    FitResult r = Fit.responseFit(snap, res);
    assertTrue(r.ok());
  }
}

package com.oakrtb.sdk.build;

import com.oakrtb.openrtb.v2.Bid;
import com.oakrtb.openrtb.v2.BidResponse;
import com.oakrtb.openrtb.v2.MarkupType;
import com.oakrtb.sdk.schema.Report;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BidResponseBuilderTest {

  private static Bid validBid() {
    return BidResponseBuilder.bid("b1", "1", 1.23).banner().build();
  }

  @Test
  void missingIdFailsOnBuild() {
    assertThrows(IllegalStateException.class, () -> BidResponseBuilder.create("").build());
    assertThrows(IllegalStateException.class, () -> BidResponseBuilder.create(null).build());
  }

  @Test
  void emptyCurrencyFailsOnBuild() {
    assertThrows(
        IllegalStateException.class,
        () ->
            BidResponseBuilder.create("req-1")
                .currency("")
                .addSeatBid("512", validBid())
                .build());
  }

  @Test
  void noSeatBidAndNoNoBidFails() {
    assertThrows(IllegalStateException.class, () -> BidResponseBuilder.create("req-1").build());
  }

  @Test
  void bidMissingIdFails() {
    Bid noId = Bid.newBuilder().setImpid("1").setPrice(1.0).build();
    assertThrows(
        IllegalStateException.class,
        () -> BidResponseBuilder.create("req-1").addSeatBid("512", noId).build());
  }

  @Test
  void bidMissingImpidFails() {
    Bid noImpid = Bid.newBuilder().setId("b1").setPrice(1.0).build();
    assertThrows(
        IllegalStateException.class,
        () -> BidResponseBuilder.create("req-1").addSeatBid("512", noImpid).build());
  }

  @Test
  void bidPriceZeroFails() {
    assertThrows(
        IllegalStateException.class,
        () ->
            BidResponseBuilder.create("req-1")
                .addSeatBid("512", BidResponseBuilder.bid("b1", "1", 0).build())
                .build());
    assertThrows(
        IllegalStateException.class,
        () ->
            BidResponseBuilder.create("req-1")
                .addSeatBid("512", BidResponseBuilder.bid("b1", "1", -0.01).build())
                .build());
  }

  @Test
  void bidIdAndCurrency() {
    BidResponse res =
        BidResponseBuilder.create("req-1")
            .bidId("response-bid-99")
            .currency("EUR")
            .addSeatBid("512", validBid())
            .build();
    assertEquals("response-bid-99", res.getBidid());
    assertEquals("EUR", res.getCur());
  }

  @Test
  void bidBuilderChainSetsAllFields() {
    Bid bid =
        BidResponseBuilder.bid("b1", "imp-1", 2.5)
            .adm("<html/>")
            .nurl("https://example.com/nurl")
            .burl("https://example.com/burl")
            .crid("cr-1")
            .cid("camp-1")
            .adomain("adv.com", "www.adv.com")
            .size(300, 250)
            .dealId("deal-42")
            .markupType(MarkupType.MARKUP_TYPE_VIDEO)
            .dur(15)
            .build();
    assertEquals("<html/>", bid.getAdm());
    assertEquals("https://example.com/nurl", bid.getNurl());
    assertEquals("https://example.com/burl", bid.getBurl());
    assertEquals("cr-1", bid.getCrid());
    assertEquals("camp-1", bid.getCid());
    assertEquals(2, bid.getAdomainCount());
    assertEquals(300, bid.getW());
    assertEquals(250, bid.getH());
    assertEquals("deal-42", bid.getDealid());
    assertEquals(MarkupType.MARKUP_TYPE_VIDEO, bid.getMtype());
    assertEquals(15, bid.getDur());
  }

  @Test
  void bidBuilderFormatShortcuts() {
    assertEquals(
        MarkupType.MARKUP_TYPE_BANNER,
        BidResponseBuilder.bid("1", "1", 1.0).banner().build().getMtype());
    assertEquals(
        MarkupType.MARKUP_TYPE_VIDEO,
        BidResponseBuilder.bid("1", "1", 1.0).video().build().getMtype());
    assertEquals(
        MarkupType.MARKUP_TYPE_AUDIO,
        BidResponseBuilder.bid("1", "1", 1.0).audio().build().getMtype());
    assertEquals(
        MarkupType.MARKUP_TYPE_NATIVE,
        BidResponseBuilder.bid("1", "1", 1.0).nativeAd().build().getMtype());
  }

  @Test
  void addSeatBidEmptyBidsThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> BidResponseBuilder.create("req-1").addSeatBid("512"));
    assertThrows(
        IllegalArgumentException.class,
        () -> BidResponseBuilder.create("req-1").addSeatBid("512", (Bid[]) null));
  }

  @Test
  void buildJsonReturnsNonEmptyBytes() {
    byte[] json =
        BidResponseBuilder.create("req-1")
            .addSeatBid("512", validBid())
            .buildJson();
    assertNotNull(json);
    assertTrue(json.length > 0);
    String s = new String(json);
    assertTrue(s.contains("\"id\":\"req-1\""));
    assertTrue(s.contains("seatbid"));
  }

  @Test
  void noBidBuilds() {
    BidResponse res = BidResponseBuilder.create("req-1").noBid(2).build();
    assertEquals(2, res.getNbr());
    assertEquals(0, res.getSeatbidCount());
  }
}

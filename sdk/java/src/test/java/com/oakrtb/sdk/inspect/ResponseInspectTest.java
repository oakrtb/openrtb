package com.oakrtb.sdk.inspect;

import com.oakrtb.openrtb.v2.Bid;
import com.oakrtb.openrtb.v2.BidResponse;
import com.oakrtb.openrtb.v2.MarkupType;
import com.oakrtb.openrtb.v2.SeatBid;
import com.oakrtb.sdk.build.BidResponseBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseInspectTest {

  @Test
  void markupFromMtypeMapsKnownTypes() {
    assertTrue(ResponseInspect.markupFromMtype(MarkupType.MARKUP_TYPE_BANNER).hasBanner());
    assertTrue(ResponseInspect.markupFromMtype(MarkupType.MARKUP_TYPE_VIDEO).hasVideo());
    assertTrue(ResponseInspect.markupFromMtype(MarkupType.MARKUP_TYPE_AUDIO).hasAudio());
    assertTrue(ResponseInspect.markupFromMtype(MarkupType.MARKUP_TYPE_NATIVE).hasNative());
    assertEquals(0, ResponseInspect.markupFromMtype(null).bits());
  }

  @Test
  void markupFromMtypeValue() {
    assertTrue(ResponseInspect.markupFromMtypeValue(1).hasBanner());
    assertTrue(ResponseInspect.markupFromMtypeValue(4).hasNative());
    assertEquals(0, ResponseInspect.markupFromMtypeValue(0).bits());
    assertEquals(0, ResponseInspect.markupFromMtypeValue(99).bits());
  }

  @Test
  void pinSharedNoBid() {
    BidResponse res = BidResponseBuilder.create("req-1").noBid(3).build();
    ResponseInspect.SharedView sv = ResponseInspect.pinShared(res);
    assertEquals("req-1", sv.id());
    assertTrue(sv.noBid());
    assertEquals(3, sv.nbr());
  }

  @Test
  void viewBidsFlattensSeatBids() {
    BidResponse res =
        BidResponseBuilder.create("req-1")
            .addSeatBid(
                "seat-a",
                BidResponseBuilder.bid("b1", "1", 1.0).banner().build(),
                BidResponseBuilder.bid("b2", "2", 2.0).video().build())
            .build();
    var bids = ResponseInspect.viewBids(res);
    assertEquals(2, bids.size());
    assertEquals("b1", bids.get(0).id());
    assertEquals("seat-a", bids.get(0).seat());
    assertTrue(bids.get(0).markup().hasBanner());
    assertTrue(bids.get(1).markup().hasVideo());
  }

  @Test
  void viewsAfterGateSkipsLightGate() {
    BidResponse res =
        BidResponse.newBuilder()
            .setId("req-1")
            .setCur("USD")
            .addSeatbid(
                SeatBid.newBuilder()
                    .setSeat("s")
                    .addBid(Bid.newBuilder().setId("b").setImpid("1").setPrice(1.0))
                    .build())
            .build();
    ResponseInspect.Result result = ResponseInspect.viewsAfterGate(res);
    assertEquals(1, result.bids().size());
    assertFalse(result.shared().noBid());
  }

  @Test
  void viewBidLists() {
    BidResponse res =
        BidResponseBuilder.create("req-1")
            .addSeatBid("512", BidResponseBuilder.bid("b1", "1", 1.5).build())
            .build();
    ResponseInspect.BidLists lists = ResponseInspect.viewBidLists(res);
    assertEquals(1, lists.seatBids().size());
    assertEquals(1, lists.bids().size());
    assertEquals("512", lists.seatBids().get(0).seat());
  }
}

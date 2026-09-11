package com.oakrtb.sdk.view;

import com.oakrtb.openrtb.v2.BidResponse;
import com.oakrtb.openrtb.v2.MarkupType;
import com.oakrtb.sdk.build.BidResponseBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseViewsTest {

  @Test
  void markupFromMtypeMapsKnownTypes() {
    assertTrue(ResponseViews.markupFromMtype(MarkupType.MARKUP_TYPE_BANNER).hasBanner());
    assertTrue(ResponseViews.markupFromMtype(MarkupType.MARKUP_TYPE_VIDEO).hasVideo());
    assertTrue(ResponseViews.markupFromMtype(MarkupType.MARKUP_TYPE_AUDIO).hasAudio());
    assertTrue(ResponseViews.markupFromMtype(MarkupType.MARKUP_TYPE_NATIVE).hasNative());
    assertEquals(0, ResponseViews.markupFromMtype(null).bits());
  }

  @Test
  void markupFromMtypeValue() {
    assertTrue(ResponseViews.markupFromMtypeValue(1).hasBanner());
    assertTrue(ResponseViews.markupFromMtypeValue(4).hasNative());
    assertEquals(0, ResponseViews.markupFromMtypeValue(0).bits());
    assertEquals(0, ResponseViews.markupFromMtypeValue(99).bits());
  }

  @Test
  void sharedNoBid() {
    BidResponse res = BidResponseBuilder.create("req-1").noBid(3).build();
    ResponseViews.SharedView sv = ResponseViews.shared(res);
    assertEquals("req-1", sv.id());
    assertTrue(sv.noBid());
    assertEquals(3, sv.nbr());
  }

  @Test
  void pipelineBidsFlattensSeatBids() {
    BidResponse res =
        BidResponseBuilder.create("req-1")
            .addSeatBid(
                "seat-a",
                BidResponseBuilder.bid("b1", "1", 1.0).banner().build(),
                BidResponseBuilder.bid("b2", "2", 2.0).video().build())
            .build();
    ResponsePipeline.Snapshot snap = ResponsePipeline.run(res);
    var bids = snap.bids();
    assertEquals(2, bids.size());
    assertEquals("b1", bids.get(0).id());
    assertEquals("seat-a", bids.get(0).seat());
    assertTrue(bids.get(0).markup().hasBanner());
    assertTrue(bids.get(1).markup().hasVideo());
  }
}

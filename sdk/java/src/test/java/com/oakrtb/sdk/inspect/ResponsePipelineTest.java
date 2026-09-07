package com.oakrtb.sdk.inspect;

import com.oakrtb.openrtb.v2.BidResponse;
import com.oakrtb.sdk.build.BidResponseBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponsePipelineTest {
  @Test
  void runFacts() {
    BidResponse res =
        BidResponseBuilder.create("auction-1")
            .currency("USD")
            .addSeatBid(
                "512",
                BidResponseBuilder.bid("1", "1", 1.23)
                    .banner()
                    .size(300, 250)
                    .adm("<img/>")
                    .crid("c1")
                    .adomain("adv.com")
                    .build())
            .build();

    ResponsePipeline.Snapshot snap = ResponsePipeline.run(res);
    assertEquals("auction-1", snap.requestId());
    assertFalse(snap.noBid());
    assertEquals("USD", snap.currency());
    assertTrue(snap.findBid("1").isPresent());
    assertEquals(1, snap.bidsForImp("1").size());
    assertEquals(1, snap.bidsWith(MarkupMask.BANNER).size());

    var facts = snap.facts();
    assertEquals(1, facts.size());
    ResponsePipeline.BidFact f = facts.get(0);
    assertTrue(f.hasBanner());
    assertEquals(1, f.mtype());
    assertEquals(300, f.w());
    assertTrue(f.hasAdm());
    assertEquals(1.23, f.price(), 1e-9);
  }

  @Test
  void noBid() {
    BidResponse res = BidResponseBuilder.create("auction-1").noBid(2).build();
    ResponsePipeline.Snapshot snap = ResponsePipeline.run(res);
    assertTrue(snap.noBid());
    assertEquals(2, snap.nbr());
    assertTrue(snap.bids().isEmpty());
  }

  @Test
  void lightGateRejectsBadPrice() {
    BidResponse res =
        BidResponseBuilder.create("x")
            .addSeatBid("s", BidResponseBuilder.bid("1", "1", 1.0).build())
            .build()
            .toBuilder()
            .clearSeatbid()
            .addSeatbid(
                com.oakrtb.openrtb.v2.SeatBid.newBuilder()
                    .setSeat("s")
                    .addBid(
                        com.oakrtb.openrtb.v2.Bid.newBuilder()
                            .setId("1")
                            .setImpid("1")
                            .setPrice(0)
                            .build())
                    .build())
            .build();
    assertThrows(IllegalArgumentException.class, () -> ResponseInspect.lightGate(res));
  }
}

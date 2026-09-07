package com.oakrtb.sdk.inspect;

import com.oakrtb.openrtb.v2.BidRequest;
import com.oakrtb.sdk.build.BidRequestBuilder;
import com.oakrtb.sdk.build.ImpBuilders;
import com.oakrtb.sdk.build.Parts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestPipelineTest {
  @Test
  void runFacts() {
    BidRequest req =
        BidRequestBuilder.create("auction-1")
            .firstPrice()
            .tmax(120)
            .currency("USD")
            .site(Parts.site().id("s1").domain("example.com").page("https://example.com/a").build())
            .addImp(ImpBuilders.banner("1").size(300, 250).floor(0.03, "USD").secure().build())
            .build();

    RequestPipeline.Snapshot snap = RequestPipeline.run(req);
    assertEquals("auction-1", snap.auctionId());
    assertEquals(Inventory.SITE, snap.inventory());
    assertTrue(snap.findImp("1").isPresent());
    assertEquals(1, snap.impsWith(MarkupMask.BANNER).size());

    var facts = snap.facts();
    assertEquals(1, facts.size());
    RequestPipeline.ImpFact f = facts.get(0);
    assertTrue(f.hasBanner());
    assertEquals(1, f.mtype());
    assertEquals(300, f.bannerW());
    assertEquals(0.03, f.bidFloor(), 1e-9);
    assertEquals(1, f.secure());
  }

  @Test
  void stepOrderEnforced() {
    BidRequest req =
        BidRequestBuilder.create("x")
            .firstPrice()
            .currency("USD")
            .addImp(ImpBuilders.banner("1").size(1, 1).build())
            .build();

    assertThrows(IllegalStateException.class, () -> RequestPipeline.of(req).pinShared());
    RequestPipeline.Snapshot snap = RequestPipeline.of(req).snapshot(); // auto-fill steps
    assertEquals("x", snap.auctionId());
  }
}

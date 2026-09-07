package com.oakrtb.sdk.build;

import com.oakrtb.openrtb.v2.Imp;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BidRequestBuilderValidationTest {

  private static Imp minimalBannerImp() {
    return ImpBuilders.banner("1").size(300, 250).build();
  }

  @Test
  void emptyIdFails() {
    assertThrows(IllegalStateException.class, () -> BidRequestBuilder.create("").build());
    assertThrows(IllegalStateException.class, () -> BidRequestBuilder.create(null).build());
  }

  @Test
  void noImpFails() {
    assertThrows(
        IllegalStateException.class,
        () ->
            BidRequestBuilder.create("req-1")
                .firstPrice()
                .currency("USD")
                .build());
  }

  @Test
  void noCurFails() {
    assertThrows(
        IllegalStateException.class,
        () ->
            BidRequestBuilder.create("req-1")
                .firstPrice()
                .addImp(minimalBannerImp())
                .build());
  }

  @Test
  void noAtFails() {
    assertThrows(
        IllegalStateException.class,
        () ->
            BidRequestBuilder.create("req-1")
                .currency("USD")
                .addImp(minimalBannerImp())
                .build());
  }
}

package com.oakrtb.sdk.inspect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryTest {

  @Test
  void toStringWireValues() {
    assertEquals("none", Inventory.NONE.toString());
    assertEquals("site", Inventory.SITE.toString());
    assertEquals("app", Inventory.APP.toString());
    assertEquals("dooh", Inventory.DOOH.toString());
  }
}

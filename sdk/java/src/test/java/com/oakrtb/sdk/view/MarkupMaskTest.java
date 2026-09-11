package com.oakrtb.sdk.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarkupMaskTest {

  @Test
  void bitsAndHasMethods() {
    MarkupMask banner = MarkupMask.of(MarkupMask.BANNER);
    assertEquals(MarkupMask.BANNER, banner.bits());
    assertTrue(banner.has(MarkupMask.BANNER));
    assertTrue(banner.hasBanner());
    assertFalse(banner.hasVideo());
    assertFalse(banner.hasAudio());
    assertFalse(banner.hasNative());
  }

  @Test
  void combinedMask() {
    int bits = MarkupMask.BANNER | MarkupMask.VIDEO;
    MarkupMask mask = MarkupMask.of(bits);
    assertTrue(mask.hasBanner());
    assertTrue(mask.hasVideo());
    assertTrue(mask.has(MarkupMask.BANNER));
    assertTrue(mask.has(MarkupMask.VIDEO));
    assertFalse(mask.has(MarkupMask.AUDIO));
    assertEquals(2, mask.count());
  }

  @Test
  void primarySingleFormat() {
    MarkupMask video = MarkupMask.of(MarkupMask.VIDEO);
    assertEquals(MarkupMask.VIDEO, video.primary());
    assertEquals(2, video.mtype());
  }

  @Test
  void primaryMultiFormatReturnsNone() {
    MarkupMask multi = MarkupMask.of(MarkupMask.BANNER | MarkupMask.NATIVE);
    assertEquals(MarkupMask.NONE, multi.primary());
    assertEquals(0, multi.mtype());
    assertEquals(2, multi.count());
  }

  @Test
  void emptyMask() {
    MarkupMask none = MarkupMask.of(MarkupMask.NONE);
    assertEquals(0, none.count());
    assertEquals(MarkupMask.NONE, none.primary());
    assertEquals(0, none.mtype());
    assertFalse(none.hasBanner());
  }

  @Test
  void mtypeForEachFormat() {
    assertEquals(1, MarkupMask.of(MarkupMask.BANNER).mtype());
    assertEquals(2, MarkupMask.of(MarkupMask.VIDEO).mtype());
    assertEquals(3, MarkupMask.of(MarkupMask.AUDIO).mtype());
    assertEquals(4, MarkupMask.of(MarkupMask.NATIVE).mtype());
  }

  @Test
  void toStringContainsBits() {
    MarkupMask mask = MarkupMask.of(MarkupMask.BANNER | MarkupMask.VIDEO);
    assertEquals("MarkupMask(" + mask.bits() + ")", mask.toString());
  }

  @Test
  void equalsAndHashCode() {
    MarkupMask a = MarkupMask.of(MarkupMask.AUDIO);
    MarkupMask b = MarkupMask.of(MarkupMask.AUDIO);
    MarkupMask c = MarkupMask.of(MarkupMask.VIDEO);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, c);
  }
}

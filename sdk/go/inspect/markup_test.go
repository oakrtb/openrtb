package inspect

import (
	"testing"

	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
)

func TestMarkupMaskOf(t *testing.T) {
	if MarkupMaskOf(nil) != MarkupNone {
		t.Fatal("nil imp should be MarkupNone")
	}
	imp := &openrtb.Imp{
		Id:     "1",
		Banner: &openrtb.Banner{},
		Video:  &openrtb.Video{},
		Audio:  &openrtb.Audio{},
		Native: &openrtb.Native{},
	}
	mask := MarkupMaskOf(imp)
	if mask.Count() != 4 || !mask.HasBanner() || !mask.HasVideo() || !mask.HasAudio() || !mask.HasNative() {
		t.Fatalf("expected all four bits, got %v", mask)
	}
}

func TestMarkupFromMtype(t *testing.T) {
	cases := []struct {
		mtype openrtb.MarkupType
		want  MarkupMask
		m     int32
	}{
		{openrtb.MarkupType_MARKUP_TYPE_BANNER, MarkupBanner, 1},
		{openrtb.MarkupType_MARKUP_TYPE_VIDEO, MarkupVideo, 2},
		{openrtb.MarkupType_MARKUP_TYPE_AUDIO, MarkupAudio, 3},
		{openrtb.MarkupType_MARKUP_TYPE_NATIVE, MarkupNative, 4},
		{openrtb.MarkupType(0), MarkupNone, 0},
		{openrtb.MarkupType(99), MarkupNone, 0},
	}
	for _, c := range cases {
		got := MarkupFromMtype(c.mtype)
		if got != c.want {
			t.Fatalf("mtype %v: got %v want %v", c.mtype, got, c.want)
		}
		if got.Mtype() != c.m {
			t.Fatalf("mtype %v: Mtype()=%d want %d", c.mtype, got.Mtype(), c.m)
		}
	}
}

func TestInventoryString(t *testing.T) {
	cases := []struct {
		inv  Inventory
		want string
	}{
		{InventoryNone, "none"},
		{InventorySite, "site"},
		{InventoryApp, "app"},
		{InventoryDooh, "dooh"},
	}
	for _, c := range cases {
		if c.inv.String() != c.want {
			t.Fatalf("%v.String()=%q want %q", c.inv, c.inv.String(), c.want)
		}
	}
}

func TestMarkupMaskHasAndPrimary(t *testing.T) {
	audioNative := MarkupAudio | MarkupNative
	if !audioNative.Has(MarkupAudio) || !audioNative.HasNative() {
		t.Fatalf("Has helpers: %v", audioNative)
	}
	if audioNative.Primary() != MarkupNone || audioNative.Mtype() != 0 {
		t.Fatalf("multi-bit primary/mtype: %v", audioNative)
	}
	if MarkupAudio.Primary() != MarkupAudio || MarkupAudio.Mtype() != 3 {
		t.Fatalf("single audio: primary=%v mtype=%d", MarkupAudio.Primary(), MarkupAudio.Mtype())
	}
}

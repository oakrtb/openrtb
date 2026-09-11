package build

import (
	"testing"

	"google.golang.org/protobuf/reflect/protoreflect"

	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
	"github.com/oakrtb/openrtb/sdk/go/schema"
)

func TestFullBidRequestApp_AllFieldsSet(t *testing.T) {
	req := fullBidRequestWithApp()
	unset := unsetFields(req.ProtoReflect(), "ext", "site", "dooh")
	if len(unset) > 0 {
		n := len(unset)
		if n > 20 {
			n = 20
		}
		t.Fatalf("unset BidRequest tree fields (%d): %v", len(unset), unset[:n])
	}
	raw, err := MarshalJSON(req)
	if err != nil {
		t.Fatal(err)
	}
	result := schema.Request(raw)
	if !result.Ok {
		t.Fatalf("schema: %+v\njson=%s", result.Errors, raw)
	}
	back, err := UnmarshalBidRequest(raw)
	if err != nil {
		t.Fatal(err)
	}
	assertKeyScalars(t, req, back)
}

func TestFullBidRequestSite_AllFieldsSet(t *testing.T) {
	req := fullBidRequestWithSite()
	if req.Site == nil || req.App != nil || req.Dooh != nil {
		t.Fatal("expected site-only inventory")
	}
	unset := unsetFields(req.Site.ProtoReflect(), "ext")
	if len(unset) > 0 {
		t.Fatalf("unset Site fields: %v", unset)
	}
	raw, err := MarshalJSON(req)
	if err != nil {
		t.Fatal(err)
	}
	if r := schema.Request(raw); !r.Ok {
		t.Fatalf("schema: %+v", r.Errors)
	}
}

func TestFullBidRequestDooh_AllFieldsSet(t *testing.T) {
	req := fullBidRequestWithDooh()
	if req.Dooh == nil || req.App != nil || req.Site != nil {
		t.Fatal("expected dooh-only inventory")
	}
	unset := unsetFields(req.Dooh.ProtoReflect(), "ext")
	if len(unset) > 0 {
		t.Fatalf("unset Dooh fields: %v", unset)
	}
	raw, err := MarshalJSON(req)
	if err != nil {
		t.Fatal(err)
	}
	if r := schema.Request(raw); !r.Ok {
		t.Fatalf("schema: %+v", r.Errors)
	}
}

func TestFullBidResponse_AllFieldsSet(t *testing.T) {
	res := fullBidResponse()
	unset := unsetFields(res.ProtoReflect(), "ext")
	if len(unset) > 0 {
		n := len(unset)
		if n > 20 {
			n = 20
		}
		t.Fatalf("unset BidResponse tree fields (%d): %v", len(unset), unset[:n])
	}
	raw, err := MarshalJSON(res)
	if err != nil {
		t.Fatal(err)
	}
	result := schema.Response(raw)
	if !result.Ok {
		t.Fatalf("schema: %+v\njson=%s", result.Errors, raw)
	}
	back, err := UnmarshalBidResponse(raw)
	if err != nil {
		t.Fatal(err)
	}
	if back.GetId() != res.GetId() || back.GetBidid() != res.GetBidid() {
		t.Fatalf("round-trip ids mismatch")
	}
	if len(back.GetSeatbid()) == 0 || len(back.GetSeatbid()[0].GetBid()) == 0 {
		t.Fatal("missing seatbid/bid after round-trip")
	}
	if back.GetSeatbid()[0].GetBid()[0].GetPrice() != res.GetSeatbid()[0].GetBid()[0].GetPrice() {
		t.Fatal("price round-trip mismatch")
	}
}

func TestExtFieldCanBeSetOnProto(t *testing.T) {
	req := &openrtb.BidRequest{Id: "x", Ext: `{"k":"v"}`}
	if req.GetExt() != `{"k":"v"}` {
		t.Fatal(req.GetExt())
	}
	imp := &openrtb.Imp{Id: "1", Ext: `{"placement":9}`}
	if imp.GetExt() != `{"placement":9}` {
		t.Fatal(imp.GetExt())
	}
}

func TestFieldCountsMatchProto(t *testing.T) {
	for msg, n := range FieldCounts {
		if n <= 0 {
			t.Fatalf("%s count %d", msg, n)
		}
	}
	if FieldCounts["BidRequest"] < 20 || FieldCounts["Video"] < 30 || FieldCounts["Bid"] < 25 {
		t.Fatalf("unexpected counts: %+v", FieldCounts)
	}
}

func assertKeyScalars(t *testing.T, a, b *openrtb.BidRequest) {
	t.Helper()
	if a.GetId() != b.GetId() || a.GetAt() != b.GetAt() || a.GetTmax() != b.GetTmax() {
		t.Fatalf("request scalars mismatch")
	}
	if len(a.GetImp()) != len(b.GetImp()) {
		t.Fatal("imp len")
	}
	ai, bi := a.GetImp()[0], b.GetImp()[0]
	if ai.GetBanner().GetW() != bi.GetBanner().GetW() {
		t.Fatal("banner.w")
	}
	if ai.GetVideo().GetMinduration() != bi.GetVideo().GetMinduration() {
		t.Fatal("video.minduration")
	}
	if ai.GetAudio().GetFeed() != bi.GetAudio().GetFeed() {
		t.Fatal("audio.feed")
	}
	if ai.GetNative().GetRequest() == "" || ai.GetNative().GetRequest() != bi.GetNative().GetRequest() {
		t.Fatal("native.request")
	}
}

// unsetFields walks a message tree and returns proto paths that are unset
// (skipping listed leaf field names such as ext).
func unsetFields(m protoreflect.Message, skipLeafNames ...string) []string {
	skip := map[string]bool{}
	for _, n := range skipLeafNames {
		skip[n] = true
	}
	var out []string
	walkUnset(m, "", skip, &out)
	return out
}

func walkUnset(m protoreflect.Message, prefix string, skip map[string]bool, out *[]string) {
	if !m.IsValid() {
		*out = append(*out, prefix+"(invalid)")
		return
	}
	fields := m.Descriptor().Fields()
	for i := 0; i < fields.Len(); i++ {
		fd := fields.Get(i)
		name := string(fd.Name())
		path := name
		if prefix != "" {
			path = prefix + "." + name
		}
		if skip[name] {
			continue
		}
		if fd.IsList() {
			list := m.Get(fd).List()
			if list.Len() == 0 {
				*out = append(*out, path)
				continue
			}
			if fd.Kind() == protoreflect.MessageKind {
				for j := 0; j < list.Len(); j++ {
					walkUnset(list.Get(j).Message(), path+"[]", skip, out)
				}
			}
			continue
		}
		if fd.Kind() == protoreflect.MessageKind {
			if !m.Has(fd) {
				*out = append(*out, path)
				continue
			}
			walkUnset(m.Get(fd).Message(), path, skip, out)
			continue
		}
		if !m.Has(fd) {
			*out = append(*out, path)
		}
	}
}

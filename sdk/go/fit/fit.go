// Package fit 在 RequestInspect 与 ResponseInspect 之间提供可选的 bid↔request 匹配检查。
//
// 非 JSON Schema，也非 LightGate。返回软性的 FitResult 发现项（业务不匹配不强制 error）。
// 调用方自行决定是否因 ERROR 拒绝或依据 WARN 处理。
package fit

import (
	"strings"

	"github.com/oakrtb/openrtb/sdk/go/inspect"
	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
)

// ImpReadyMtype 检查所选 markup 类型是否存在于 Imp 且具备构建器级规格。
func ImpReadyMtype(imp *inspect.ImpView, mtype int32) FitResult {
	if imp == nil {
		return FitResult{Issues: []FitIssue{{
			Code: CodeImpNotFound, Severity: SeverityError, Path: "imp", Message: "nil ImpView",
		}}}
	}
	path := "imp[" + imp.ID + "]"
	if mtype < 1 || mtype > 4 {
		return FitResult{Issues: []FitIssue{{
			Code: CodeMtypeUnknown, Severity: SeverityError, Path: path + ".mtype",
			Message: "mtype must be 1–4 for ImpReady",
		}}}
	}
	return impReadyChosen(imp, inspect.MarkupFromMtype(openrtb.MarkupType(mtype)), path)
}

// ImpReadyMarkup 与 ImpReadyMtype 类似，但使用单比特 MarkupMask 指定类型。
func ImpReadyMarkup(imp *inspect.ImpView, chosen inspect.MarkupMask) FitResult {
	if imp == nil {
		return FitResult{Issues: []FitIssue{{
			Code: CodeImpNotFound, Severity: SeverityError, Path: "imp", Message: "nil ImpView",
		}}}
	}
	path := "imp[" + imp.ID + "]"
	if chosen.Count() != 1 {
		return FitResult{Issues: []FitIssue{{
			Code: CodeMultiNeedsChoice, Severity: SeverityError, Path: path,
			Message: "chosen MarkupMask must be exactly one bit",
		}}}
	}
	return impReadyChosen(imp, chosen, path)
}

func impReadyChosen(imp *inspect.ImpView, chosen inspect.MarkupMask, path string) FitResult {
	var issues []FitIssue
	if !imp.Markup.Has(chosen) {
		return FitResult{Issues: []FitIssue{{
			Code: CodeFormatNotOnImp, Severity: SeverityError, Path: path,
			Message: "chosen format is not present on Imp",
		}}}
	}
	if chosen.HasBanner() {
		b := imp.Banner
		sizeOK := b != nil && ((b.W > 0 && b.H > 0) || len(b.Format) > 0)
		if !sizeOK {
			issues = append(issues, FitIssue{
				Code: CodeBannerSizeMissing, Severity: SeverityError, Path: path + ".banner",
				Message: "banner needs w/h or format[]",
			})
		}
	}
	if chosen.HasVideo() {
		v := imp.Video
		if v == nil || len(v.Mimes) == 0 {
			issues = append(issues, FitIssue{
				Code: CodeVideoMimesMissing, Severity: SeverityError, Path: path + ".video.mimes",
				Message: "video.mimes is required",
			})
		} else {
			if v.Minduration == 0 && v.Maxduration == 0 {
				issues = append(issues, FitIssue{
					Code: CodeVideoDurationUnset, Severity: SeverityWarn, Path: path + ".video",
					Message: "video minduration/maxduration unset",
				})
			}
			if len(v.Protocols) == 0 {
				issues = append(issues, FitIssue{
					Code: CodeVideoProtocolsUnset, Severity: SeverityWarn, Path: path + ".video.protocols",
					Message: "video.protocols unset",
				})
			}
		}
	}
	if chosen.HasAudio() {
		a := imp.Audio
		if a == nil || len(a.Mimes) == 0 {
			issues = append(issues, FitIssue{
				Code: CodeAudioMimesMissing, Severity: SeverityError, Path: path + ".audio.mimes",
				Message: "audio.mimes is required",
			})
		}
	}
	if chosen.HasNative() {
		n := imp.Native
		if n == nil || strings.TrimSpace(n.Request) == "" {
			issues = append(issues, FitIssue{
				Code: CodeNativeRequestMissing, Severity: SeverityError, Path: path + ".native.request",
				Message: "native.request is required",
			})
		}
	}
	return FitResult{Issues: issues}
}

// BidFit 将单个 Bid 与请求快照进行匹配检查。
func BidFit(req *inspect.RequestSnapshot, bid *openrtb.Bid) FitResult {
	return bidFit(req, bid, "", "bid")
}

// ResponseFit 对整个 BidResponse 做匹配检查；空 seatbid 视为通过（可附加 PAST_DEADLINE 警告）。
func ResponseFit(req *inspect.RequestSnapshot, res *openrtb.BidResponse) FitResult {
	var issues []FitIssue
	if req == nil || res == nil {
		return FitResult{Issues: []FitIssue{{
			Code: CodeImpNotFound, Severity: SeverityError, Path: "", Message: "nil req or res",
		}}}
	}
	if req.PastDeadline() {
		issues = append(issues, FitIssue{
			Code: CodePastDeadline, Severity: SeverityWarn, Path: "tmax",
			Message: "past request deadline (85% of tmax)",
		})
	}
	if len(res.Seatbid) == 0 {
		return FitResult{Issues: issues}
	}
	resCur := res.Cur
	if resCur != "" {
		allowed := false
		for _, c := range req.Currencies() {
			if strings.EqualFold(resCur, c) {
				allowed = true
				break
			}
		}
		if !allowed {
			issues = append(issues, FitIssue{
				Code: CodeCurNotAllowed, Severity: SeverityWarn, Path: "BidResponse.cur",
				Message: "response currency not in BidRequest.cur",
			})
		}
	}
	for i, sb := range res.Seatbid {
		if sb == nil {
			continue
		}
		for j, bid := range sb.Bid {
			if bid == nil {
				continue
			}
			path := "seatbid[" + itoa(i) + "].bid[" + itoa(j) + "]"
			one := bidFit(req, bid, resCur, path)
			issues = append(issues, one.Issues...)
		}
	}
	return FitResult{Issues: issues}
}

func bidFit(req *inspect.RequestSnapshot, bid *openrtb.Bid, responseCur, path string) FitResult {
	var issues []FitIssue
	if req == nil || bid == nil {
		return FitResult{Issues: []FitIssue{{
			Code: CodeImpNotFound, Severity: SeverityError, Path: path, Message: "nil req or bid",
		}}}
	}
	imp := req.FindImp(bid.Impid)
	if imp == nil {
		return FitResult{Issues: []FitIssue{{
			Code: CodeImpNotFound, Severity: SeverityError, Path: path + ".impid",
			Message: "impid not found in request",
		}}}
	}
	mtype := int32(bid.GetMtype())
	markup := imp.Markup

	if mtype >= 500 {
		issues = append(issues, FitIssue{
			Code: CodeMtypeVendor, Severity: SeverityWarn, Path: path + ".mtype",
			Message: "vendor mtype >=500",
		})
	} else if mtype != 0 && (mtype < 1 || mtype > 4) {
		issues = append(issues, FitIssue{
			Code: CodeMtypeUnknown, Severity: SeverityError, Path: path + ".mtype",
			Message: "mtype must be 0–4 or >=500",
		})
	} else if mtype == 0 && markup.Count() > 1 {
		issues = append(issues, FitIssue{
			Code: CodeMtypeRequired, Severity: SeverityError, Path: path + ".mtype",
			Message: "multi-format Imp requires Bid.mtype",
		})
	} else if mtype >= 1 && mtype <= 4 {
		chosen := inspect.MarkupFromMtype(openrtb.MarkupType(mtype))
		if !markup.Has(chosen) {
			issues = append(issues, FitIssue{
				Code: CodeMtypeMismatch, Severity: SeverityError, Path: path + ".mtype",
				Message: "mtype does not match Imp formats",
			})
		}
	}

	eff := inspect.MarkupNone
	if mtype >= 1 && mtype <= 4 {
		eff = inspect.MarkupFromMtype(openrtb.MarkupType(mtype))
	} else if markup.Count() == 1 {
		eff = markup
	}

	issues = append(issues, checkFloor(imp, bid, responseCur, path)...)
	issues = append(issues, checkAttr(imp, bid, eff, path)...)
	issues = append(issues, checkBlocks(req, bid, path)...)
	return FitResult{Issues: issues}
}

func checkFloor(imp *inspect.ImpView, bid *openrtb.Bid, responseCur, path string) []FitIssue {
	floor := imp.BidFloor
	if floor <= 0 {
		return nil
	}
	floorCur := imp.BidFloorCur
	if responseCur != "" && floorCur != "" && !strings.EqualFold(responseCur, floorCur) {
		return []FitIssue{{
			Code: CodeFloorCurDiff, Severity: SeverityWarn, Path: path + ".price",
			Message: "bid currency differs from imp.bidfloorcur; skip floor compare",
		}}
	}
	if bid.Price < floor {
		return []FitIssue{{
			Code: CodePriceBelowFloor, Severity: SeverityWarn, Path: path + ".price",
			Message: "price below imp.bidfloor",
		}}
	}
	return nil
}

func checkAttr(imp *inspect.ImpView, bid *openrtb.Bid, eff inspect.MarkupMask, path string) []FitIssue {
	if len(bid.Attr) == 0 || eff == inspect.MarkupNone {
		return nil
	}
	battr := battrFor(imp, eff)
	if len(battr) == 0 {
		return nil
	}
	blocked := map[int32]struct{}{}
	for _, v := range battr {
		blocked[v] = struct{}{}
	}
	for _, a := range bid.Attr {
		if _, ok := blocked[a]; ok {
			return []FitIssue{{
				Code: CodeAttrBlocked, Severity: SeverityWarn, Path: path + ".attr",
				Message: "bid.attr intersects format battr",
			}}
		}
	}
	return nil
}

func battrFor(imp *inspect.ImpView, eff inspect.MarkupMask) []int32 {
	if eff.HasBanner() && imp.Banner != nil {
		return imp.Banner.Battr
	}
	if eff.HasVideo() && imp.Video != nil {
		return imp.Video.Battr
	}
	if eff.HasAudio() && imp.Audio != nil {
		return imp.Audio.Battr
	}
	if eff.HasNative() && imp.Native != nil {
		return imp.Native.Battr
	}
	return nil
}

func checkBlocks(req *inspect.RequestSnapshot, bid *openrtb.Bid, path string) []FitIssue {
	var issues []FitIssue
	if len(req.Shared.Badv) > 0 {
		block := lowerSet(req.Shared.Badv)
		for _, d := range bid.Adomain {
			if _, ok := block[strings.ToLower(d)]; ok {
				issues = append(issues, FitIssue{
					Code: CodeAdomainBlocked, Severity: SeverityWarn, Path: path + ".adomain",
					Message: "adomain hit BidRequest.badv",
				})
				break
			}
		}
	}
	if len(req.Shared.Bapp) > 0 && bid.Bundle != "" {
		block := lowerSet(req.Shared.Bapp)
		if _, ok := block[strings.ToLower(bid.Bundle)]; ok {
			issues = append(issues, FitIssue{
				Code: CodeBundleBlocked, Severity: SeverityWarn, Path: path + ".bundle",
				Message: "bundle hit BidRequest.bapp",
			})
		}
	}
	if len(req.Shared.Bcat) > 0 {
		block := lowerSet(req.Shared.Bcat)
		for _, c := range bid.Cat {
			if _, ok := block[strings.ToLower(c)]; ok {
				issues = append(issues, FitIssue{
					Code: CodeCatBlocked, Severity: SeverityWarn, Path: path + ".cat",
					Message: "cat hit BidRequest.bcat",
				})
				break
			}
		}
	}
	return issues
}

func lowerSet(in []string) map[string]struct{} {
	out := make(map[string]struct{}, len(in))
	for _, s := range in {
		out[strings.ToLower(s)] = struct{}{}
	}
	return out
}

func itoa(i int) string {
	if i == 0 {
		return "0"
	}
	var b [20]byte
	pos := len(b)
	n := i
	for n > 0 {
		pos--
		b[pos] = byte('0' + n%10)
		n /= 10
	}
	return string(b[pos:])
}

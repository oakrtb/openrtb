package fit

// Severity 表示 FitIssue 的严重级别。
type Severity string

const (
	// SeverityError 表示应拒绝出价的错误级发现。
	SeverityError Severity = "ERROR"
	// SeverityWarning 表示可继续但需留意的警告级发现。
	SeverityWarn Severity = "WARN"
)

// 稳定的问题代码（与 Java / Rust 对齐）。
const (
	CodeFormatNotOnImp       = "FORMAT_NOT_ON_IMP"
	CodeMultiNeedsChoice     = "MULTI_NEEDS_CHOICE"
	CodeBannerSizeMissing    = "BANNER_SIZE_MISSING"
	CodeVideoMimesMissing    = "VIDEO_MIMES_MISSING"
	CodeAudioMimesMissing    = "AUDIO_MIMES_MISSING"
	CodeNativeRequestMissing = "NATIVE_REQUEST_MISSING"
	CodeVideoDurationUnset   = "VIDEO_DURATION_UNSET"
	CodeVideoProtocolsUnset  = "VIDEO_PROTOCOLS_UNSET"

	CodeImpNotFound     = "IMP_NOT_FOUND"
	CodeMtypeRequired   = "MTYPE_REQUIRED"
	CodeMtypeMismatch   = "MTYPE_MISMATCH"
	CodeMtypeUnknown    = "MTYPE_UNKNOWN"
	CodeMtypeVendor     = "MTYPE_VENDOR"
	CodePriceBelowFloor = "PRICE_BELOW_FLOOR"
	CodeFloorCurDiff    = "FLOOR_CUR_DIFF"
	CodeAttrBlocked     = "ATTR_BLOCKED"
	CodeAdomainBlocked  = "ADOMAIN_BLOCKED"
	CodeBundleBlocked   = "BUNDLE_BLOCKED"
	CodeCatBlocked      = "CAT_BLOCKED"
	CodeCurNotAllowed   = "CUR_NOT_ALLOWED"
	CodePastDeadline    = "PAST_DEADLINE"
)

// FitIssue 是 Fit 产生的一条发现项。
type FitIssue struct {
	Code     string
	Severity Severity
	Path     string
	Message  string
}

// FitResult 聚合多条发现项；无 ERROR 时 OK 为 true。
type FitResult struct {
	Issues []FitIssue
}

// OK 在无 ERROR 级发现时返回 true。
func (r FitResult) OK() bool {
	for _, i := range r.Issues {
		if i.Severity == SeverityError {
			return false
		}
	}
	return true
}

// Has 报告是否存在指定代码的发现项。
func (r FitResult) Has(code string) bool {
	for _, i := range r.Issues {
		if i.Code == code {
			return true
		}
	}
	return false
}

// Errors 返回所有 ERROR 级发现项。
func (r FitResult) Errors() []FitIssue {
	var out []FitIssue
	for _, i := range r.Issues {
		if i.Severity == SeverityError {
			out = append(out, i)
		}
	}
	return out
}

// Warnings 返回所有 WARN 级发现项。
func (r FitResult) Warnings() []FitIssue {
	var out []FitIssue
	for _, i := range r.Issues {
		if i.Severity == SeverityWarn {
			out = append(out, i)
		}
	}
	return out
}

package schema

import "encoding/json"

// Report 是 JSON Schema 校验结果（Ok 为 false 时可作 HTTP 400 响应体）。
type Report struct {
	Ok     bool    `json:"ok"`
	Errors []Issue `json:"errors"`
}

// Issue 表示一条 Schema 校验失败项。
type Issue struct {
	Code    string `json:"code"`
	Path    string `json:"path"`
	Message string `json:"message"`
}

// OK 返回成功结果。
func OK() Report {
	return Report{Ok: true, Errors: []Issue{}}
}

// Fail 返回失败结果。
func Fail(errs ...Issue) Report {
	if len(errs) == 0 {
		errs = []Issue{}
	}
	return Report{Ok: false, Errors: errs}
}

// MarshalJSON 确保 errors 在 JSON 中始终为数组（不为 null）。
func (r Report) MarshalJSON() ([]byte, error) {
	type alias Report
	out := alias(r)
	if out.Errors == nil {
		out.Errors = []Issue{}
	}
	return json.Marshal(out)
}

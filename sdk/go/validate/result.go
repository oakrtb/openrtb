package validate

import "encoding/json"

// ValidationResult 是统一的校验结果（Ok 为 false 时可作为 HTTP 400 响应体）。
type ValidationResult struct {
	Ok     bool              `json:"ok"`
	Errors []ValidationError `json:"errors"`
}

// ValidationError 表示一条未通过的校验项。
type ValidationError struct {
	Code    string `json:"code"`
	Path    string `json:"path"`
	Message string `json:"message"`
}

// OK 返回成功的校验结果。
func OK() ValidationResult {
	return ValidationResult{Ok: true, Errors: []ValidationError{}}
}

// Fail 返回失败的校验结果，并附带给定错误（强制 ok 为 false）。
func Fail(errs ...ValidationError) ValidationResult {
	if len(errs) == 0 {
		errs = []ValidationError{}
	}
	return ValidationResult{Ok: false, Errors: errs}
}

// MarshalJSON 确保 errors 在 JSON 中始终为数组（不为 null）。
func (r ValidationResult) MarshalJSON() ([]byte, error) {
	type alias ValidationResult
	out := alias(r)
	if out.Errors == nil {
		out.Errors = []ValidationError{}
	}
	return json.Marshal(out)
}

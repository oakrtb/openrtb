package validate

import "encoding/json"

// ValidationResult is the unified validation outcome (HTTP 400 body when Ok is false).
type ValidationResult struct {
	Ok     bool              `json:"ok"`
	Errors []ValidationError `json:"errors"`
}

// ValidationError is one failing check.
type ValidationError struct {
	Code    string `json:"code"`
	Path    string `json:"path"`
	Message string `json:"message"`
}

// OK returns a successful result.
func OK() ValidationResult {
	return ValidationResult{Ok: true, Errors: []ValidationError{}}
}

// Fail returns a failed result with the given errors (ok forced false).
func Fail(errs ...ValidationError) ValidationResult {
	if len(errs) == 0 {
		errs = []ValidationError{}
	}
	return ValidationResult{Ok: false, Errors: errs}
}

// MarshalJSON ensures errors is always a JSON array (never null).
func (r ValidationResult) MarshalJSON() ([]byte, error) {
	type alias ValidationResult
	out := alias(r)
	if out.Errors == nil {
		out.Errors = []ValidationError{}
	}
	return json.Marshal(out)
}

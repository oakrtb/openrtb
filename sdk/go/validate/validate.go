package validate

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strings"
	"sync"

	_ "embed"

	"github.com/santhosh-tekuri/jsonschema/v6"
)

//go:embed schemas/openrtb.schema.json
var schemaOpenRTB []byte

//go:embed schemas/bid-request.schema.json
var schemaBidRequest []byte

//go:embed schemas/bid-response.schema.json
var schemaBidResponse []byte

//go:embed schemas/native.schema.json
var schemaNative []byte

var (
	compilerOnce sync.Once
	compilerErr  error
	requestSch   *jsonschema.Schema
	responseSch  *jsonschema.Schema
	nativeSch    *jsonschema.Schema
)

func initSchemas() {
	compilerOnce.Do(func() {
		c := jsonschema.NewCompiler()
		resources := map[string][]byte{
			"openrtb.schema.json":      schemaOpenRTB,
			"bid-request.schema.json":  schemaBidRequest,
			"bid-response.schema.json": schemaBidResponse,
			"native.schema.json":       schemaNative,
		}
		for name, raw := range resources {
			doc, err := jsonschema.UnmarshalJSON(bytes.NewReader(raw))
			if err != nil {
				compilerErr = fmt.Errorf("%s: %w", name, err)
				return
			}
			if err := c.AddResource(name, doc); err != nil {
				compilerErr = fmt.Errorf("%s: %w", name, err)
				return
			}
			if m, ok := doc.(map[string]any); ok {
				if id, ok := m["$id"].(string); ok && id != "" {
					if err := c.AddResource(id, doc); err != nil {
						compilerErr = fmt.Errorf("%s ($id): %w", name, err)
						return
					}
				}
			}
		}
		var err error
		requestSch, err = c.Compile("bid-request.schema.json")
		if err != nil {
			compilerErr = err
			return
		}
		responseSch, err = c.Compile("bid-response.schema.json")
		if err != nil {
			compilerErr = err
			return
		}
		nativeSch, err = c.Compile("native.schema.json")
		if err != nil {
			compilerErr = err
			return
		}
	})
}

// ValidateBidRequest 校验 BidRequest JSON 字节。
func ValidateBidRequest(data []byte) ValidationResult {
	initSchemas()
	return validateKind(data, requestSch, true)
}

// ValidateBidResponse 校验 BidResponse JSON 字节。
func ValidateBidResponse(data []byte) ValidationResult {
	initSchemas()
	return validateKind(data, responseSch, false)
}

func validateKind(data []byte, schema *jsonschema.Schema, checkNative bool) ValidationResult {
	if compilerErr != nil {
		return Fail(ValidationError{Code: "constraint", Path: "", Message: compilerErr.Error()})
	}
	if schema == nil {
		return Fail(ValidationError{Code: "constraint", Path: "", Message: "schema not loaded"})
	}
	var doc any
	if err := json.Unmarshal(data, &doc); err != nil {
		return Fail(ValidationError{Code: "parse", Path: "", Message: err.Error()})
	}
	if err := schema.Validate(doc); err != nil {
		return Fail(mapSchemaError(err)...)
	}
	if checkNative {
		if errs := validateNativeEmbedded(doc); len(errs) > 0 {
			return Fail(errs...)
		}
	}
	return OK()
}

func mapSchemaError(err error) []ValidationError {
	var out []ValidationError
	switch e := err.(type) {
	case *jsonschema.ValidationError:
		collectVE(e, &out)
	default:
		out = append(out, ValidationError{Code: "constraint", Path: "", Message: err.Error()})
	}
	if len(out) == 0 {
		out = append(out, ValidationError{Code: "constraint", Path: "", Message: err.Error()})
	}
	return out
}

func collectVE(e *jsonschema.ValidationError, out *[]ValidationError) {
	if len(e.Causes) == 0 {
		*out = append(*out, ValidationError{
			Code:    classify(e.Error()),
			Path:    instancePath(e),
			Message: e.Error(),
		})
		return
	}
	for _, c := range e.Causes {
		collectVE(c, out)
	}
}

func instancePath(e *jsonschema.ValidationError) string {
	// jsonschema v6 exposes InstanceLocation as []string
	if loc := e.InstanceLocation; len(loc) > 0 {
		return "/" + strings.Join(loc, "/")
	}
	return ""
}

func classify(msg string) string {
	lower := strings.ToLower(msg)
	switch {
	case strings.Contains(lower, "required"):
		return "required"
	case strings.Contains(lower, "expected") && strings.Contains(lower, "type"):
		return "type"
	case strings.Contains(lower, "type"):
		return "type"
	case strings.Contains(lower, "format") || strings.Contains(lower, "pattern"):
		return "format"
	default:
		return "constraint"
	}
}

func validateNativeEmbedded(doc any) []ValidationError {
	root, ok := doc.(map[string]any)
	if !ok {
		return nil
	}
	imps, _ := root["imp"].([]any)
	var errs []ValidationError
	for i, raw := range imps {
		imp, _ := raw.(map[string]any)
		if imp == nil {
			continue
		}
		native, _ := imp["native"].(map[string]any)
		if native == nil {
			continue
		}
		req, _ := native["request"].(string)
		if req == "" {
			continue
		}
		var inner any
		if err := json.Unmarshal([]byte(req), &inner); err != nil {
			errs = append(errs, ValidationError{
				Code:    "native",
				Path:    fmt.Sprintf("/imp/%d/native/request", i),
				Message: "native.request is not JSON: " + err.Error(),
			})
			continue
		}
		if err := nativeSch.Validate(inner); err != nil {
			for _, ve := range mapSchemaError(err) {
				ve.Code = "native"
				ve.Path = fmt.Sprintf("/imp/%d/native/request%s", i, ve.Path)
				ve.Message = "native.request: " + ve.Message
				errs = append(errs, ve)
			}
		}
	}
	return errs
}

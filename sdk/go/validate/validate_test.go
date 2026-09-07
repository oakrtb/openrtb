package validate_test

import (
	"os"
	"path/filepath"
	"runtime"
	"testing"

	"github.com/oakrtb/openrtb/sdk/go/validate"
)

func repoRoot(t *testing.T) string {
	t.Helper()
	_, file, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("runtime.Caller failed")
	}
	// sdk/go/validate -> repo root
	return filepath.Clean(filepath.Join(filepath.Dir(file), "..", "..", ".."))
}

func TestExamplesPass(t *testing.T) {
	root := repoRoot(t)
	err := filepath.Walk(filepath.Join(root, "examples"), func(path string, info os.FileInfo, err error) error {
		if err != nil || info.IsDir() || filepath.Ext(path) != ".json" {
			return err
		}
		data, err := os.ReadFile(path)
		if err != nil {
			return err
		}
		rel, _ := filepath.Rel(root, path)
		var result validate.ValidationResult
		if filepath.Base(filepath.Dir(path)) == "bid-request" {
			result = validate.ValidateBidRequest(data)
		} else {
			result = validate.ValidateBidResponse(data)
		}
		if !result.Ok {
			t.Fatalf("%s: expected ok, got %#v", rel, result.Errors)
		}
		return nil
	})
	if err != nil {
		t.Fatal(err)
	}
}

func TestInvalidRejected(t *testing.T) {
	root := repoRoot(t)
	err := filepath.Walk(filepath.Join(root, "testdata", "invalid"), func(path string, info os.FileInfo, err error) error {
		if err != nil || info.IsDir() || filepath.Ext(path) != ".json" {
			return err
		}
		data, err := os.ReadFile(path)
		if err != nil {
			return err
		}
		rel, _ := filepath.Rel(root, path)
		var result validate.ValidationResult
		if filepath.Base(filepath.Dir(path)) == "bid-request" {
			result = validate.ValidateBidRequest(data)
		} else {
			result = validate.ValidateBidResponse(data)
		}
		if result.Ok {
			t.Fatalf("%s: expected failure", rel)
		}
		if len(result.Errors) == 0 {
			t.Fatalf("%s: expected errors", rel)
		}
		return nil
	})
	if err != nil {
		t.Fatal(err)
	}
}

func TestParseError(t *testing.T) {
	r := validate.ValidateBidRequest([]byte(`{`))
	if r.Ok || len(r.Errors) == 0 || r.Errors[0].Code != "parse" {
		t.Fatalf("expected parse error, got %#v", r)
	}
}

package build

import (
	"fmt"

	"google.golang.org/protobuf/encoding/protojson"
	"google.golang.org/protobuf/proto"

	openrtb "github.com/oakrtb/openrtb/sdk/go/oakrtb/v2"
	"github.com/oakrtb/openrtb/sdk/go/validate"
)

var marshalOpts = protojson.MarshalOptions{
	UseProtoNames:   true, // OpenRTB JSON uses proto field names (bidfloor, not bidFloor)
	UseEnumNumbers:  true, // OpenRTB enums are integers
	EmitUnpopulated: false,
}

var unmarshalOpts = protojson.UnmarshalOptions{
	DiscardUnknown: true,
}

// MarshalJSON encodes a protobuf message as OpenRTB JSON.
func MarshalJSON(m proto.Message) ([]byte, error) {
	if m == nil {
		return nil, fmt.Errorf("build: nil message")
	}
	return marshalOpts.Marshal(m)
}

// UnmarshalBidRequest parses OpenRTB BidRequest JSON into a protobuf message.
func UnmarshalBidRequest(data []byte) (*openrtb.BidRequest, error) {
	out := &openrtb.BidRequest{}
	if err := unmarshalOpts.Unmarshal(data, out); err != nil {
		return nil, err
	}
	return out, nil
}

// UnmarshalBidResponse parses OpenRTB BidResponse JSON into a protobuf message.
func UnmarshalBidResponse(data []byte) (*openrtb.BidResponse, error) {
	out := &openrtb.BidResponse{}
	if err := unmarshalOpts.Unmarshal(data, out); err != nil {
		return nil, err
	}
	return out, nil
}

// ValidateJSON runs schema validation on already-encoded OpenRTB JSON.
func ValidateJSON(kind string, data []byte) validate.ValidationResult {
	switch kind {
	case "request", "bid-request":
		return validate.ValidateBidRequest(data)
	case "response", "bid-response":
		return validate.ValidateBidResponse(data)
	default:
		return validate.Fail(validate.ValidationError{
			Code:    "constraint",
			Path:    "",
			Message: "unknown kind: " + kind,
		})
	}
}

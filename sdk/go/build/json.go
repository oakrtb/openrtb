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

// MarshalJSON 将 protobuf 消息编码为 OpenRTB JSON。
func MarshalJSON(m proto.Message) ([]byte, error) {
	if m == nil {
		return nil, fmt.Errorf("build: nil message")
	}
	return marshalOpts.Marshal(m)
}

// UnmarshalBidRequest 将 OpenRTB BidRequest JSON 解析为 protobuf 消息。
func UnmarshalBidRequest(data []byte) (*openrtb.BidRequest, error) {
	out := &openrtb.BidRequest{}
	if err := unmarshalOpts.Unmarshal(data, out); err != nil {
		return nil, err
	}
	return out, nil
}

// UnmarshalBidResponse 将 OpenRTB BidResponse JSON 解析为 protobuf 消息。
func UnmarshalBidResponse(data []byte) (*openrtb.BidResponse, error) {
	out := &openrtb.BidResponse{}
	if err := unmarshalOpts.Unmarshal(data, out); err != nil {
		return nil, err
	}
	return out, nil
}

// ValidateJSON 对已编码的 OpenRTB JSON 执行 Schema 校验。
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

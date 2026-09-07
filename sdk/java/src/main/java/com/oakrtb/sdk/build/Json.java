package com.oakrtb.sdk.build;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.oakrtb.openrtb.v2.BidRequest;
import com.oakrtb.openrtb.v2.BidResponse;
import com.oakrtb.sdk.validate.ValidationResult;
import com.oakrtb.sdk.validate.Validator;

import java.nio.charset.StandardCharsets;

/** OpenRTB JSON encode/decode for protobuf models (enum as numbers, proto field names). */
public final class Json {
  private static final JsonFormat.Printer PRINTER =
      JsonFormat.printer()
          .omittingInsignificantWhitespace()
          .printingEnumsAsInts()
          .preservingProtoFieldNames();

  private static final JsonFormat.Parser PARSER =
      JsonFormat.parser().ignoringUnknownFields();

  private Json() {}

  public static String toJson(Message message) {
    try {
      return PRINTER.print(message);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException("protobuf json encode failed", e);
    }
  }

  public static byte[] toJsonBytes(Message message) {
    return toJson(message).getBytes(StandardCharsets.UTF_8);
  }

  public static BidRequest parseBidRequest(String json) throws InvalidProtocolBufferException {
    BidRequest.Builder b = BidRequest.newBuilder();
    PARSER.merge(json, b);
    return b.build();
  }

  public static BidResponse parseBidResponse(String json) throws InvalidProtocolBufferException {
    BidResponse.Builder b = BidResponse.newBuilder();
    PARSER.merge(json, b);
    return b.build();
  }

  public static ValidationResult validateBidRequestJson(byte[] json) {
    return Validator.validateBidRequest(json);
  }

  public static ValidationResult validateBidResponseJson(byte[] json) {
    return Validator.validateBidResponse(json);
  }
}

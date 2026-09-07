package com.oakrtb.sdk.build;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.oakrtb.openrtb.v2.BidRequest;
import com.oakrtb.openrtb.v2.BidResponse;
import com.oakrtb.sdk.validate.ValidationResult;
import com.oakrtb.sdk.validate.Validator;

import java.nio.charset.StandardCharsets;

/**
 * OpenRTB protobuf 模型与 JSON 的互转工具。
 *
 * <p>枚举输出为数字，字段名保留 proto 原名；适用于与 OpenRTB JSON 规范对齐的序列化/反序列化。
 */
public final class Json {
  private static final JsonFormat.Printer PRINTER =
      JsonFormat.printer()
          .omittingInsignificantWhitespace()
          .printingEnumsAsInts()
          .preservingProtoFieldNames();

  private static final JsonFormat.Parser PARSER =
      JsonFormat.parser().ignoringUnknownFields();

  private Json() {}

  /**
   * 将 protobuf 消息序列化为 OpenRTB JSON 字符串。
   *
   * @param message 待序列化的消息
   * @return JSON 字符串
   * @throws IllegalArgumentException 序列化失败时
   */
  public static String toJson(Message message) {
    try {
      return PRINTER.print(message);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException("protobuf json encode failed", e);
    }
  }

  /**
   * 将 protobuf 消息序列化为 UTF-8 JSON 字节数组。
   *
   * @param message 待序列化的消息
   * @return JSON 字节
   */
  public static byte[] toJsonBytes(Message message) {
    return toJson(message).getBytes(StandardCharsets.UTF_8);
  }

  /**
   * 从 JSON 字符串解析 {@link BidRequest}。
   *
   * @param json OpenRTB BidRequest JSON
   * @return 解析后的请求
   * @throws InvalidProtocolBufferException JSON 无法合并到 proto 时
   */
  public static BidRequest parseBidRequest(String json) throws InvalidProtocolBufferException {
    BidRequest.Builder b = BidRequest.newBuilder();
    PARSER.merge(json, b);
    return b.build();
  }

  /**
   * 从 JSON 字符串解析 {@link BidResponse}。
   *
   * @param json OpenRTB BidResponse JSON
   * @return 解析后的响应
   * @throws InvalidProtocolBufferException JSON 无法合并到 proto 时
   */
  public static BidResponse parseBidResponse(String json) throws InvalidProtocolBufferException {
    BidResponse.Builder b = BidResponse.newBuilder();
    PARSER.merge(json, b);
    return b.build();
  }

  /**
   * 对 BidRequest JSON 字节执行 Schema 校验。
   *
   * @param json UTF-8 JSON 字节
   * @return 校验结果
   */
  public static ValidationResult validateBidRequestJson(byte[] json) {
    return Validator.validateBidRequest(json);
  }

  /**
   * 对 BidResponse JSON 字节执行 Schema 校验。
   *
   * @param json UTF-8 JSON 字节
   * @return 校验结果
   */
  public static ValidationResult validateBidResponseJson(byte[] json) {
    return Validator.validateBidResponse(json);
  }
}

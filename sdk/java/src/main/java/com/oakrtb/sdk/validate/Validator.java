package com.oakrtb.sdk.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 针对 OakRTB JSON Schema 校验 BidRequest / BidResponse JSON。
 *
 * <p>离线使用 classpath 上的 schema 资源，不依赖外网。
 */
public final class Validator {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Schema REQUEST;
  private static final Schema RESPONSE;
  private static final Schema NATIVE;

  static {
    // 将 $id URL 映射到 classpath，保证离线校验。
    SchemaRegistry registry =
        SchemaRegistry.withDefaultDialect(
            SpecificationVersion.DRAFT_2020_12,
            builder ->
                builder.schemaIdResolvers(
                    resolvers ->
                        resolvers.mapPrefix(
                            "https://github.com/oakrtb/openrtb/schema/jsonschema",
                            "classpath:/schema/jsonschema")));
    REQUEST =
        registry.getSchema(
            SchemaLocation.of(
                "https://github.com/oakrtb/openrtb/schema/jsonschema/bid-request.schema.json"));
    RESPONSE =
        registry.getSchema(
            SchemaLocation.of(
                "https://github.com/oakrtb/openrtb/schema/jsonschema/bid-response.schema.json"));
    NATIVE =
        registry.getSchema(
            SchemaLocation.of(
                "https://github.com/oakrtb/openrtb/schema/jsonschema/native.schema.json"));
  }

  private Validator() {}

  /**
   * 校验 BidRequest JSON 字节。
   *
   * @param json UTF-8 JSON
   * @return 校验结果（含 embedded native.request 校验）
   */
  public static ValidationResult validateBidRequest(byte[] json) {
    return validate(json, REQUEST, true);
  }

  /**
   * 校验 BidResponse JSON 字节。
   *
   * @param json UTF-8 JSON
   * @return 校验结果
   */
  public static ValidationResult validateBidResponse(byte[] json) {
    return validate(json, RESPONSE, false);
  }

  /**
   * 校验 BidRequest JSON 字符串。
   *
   * @param json JSON 字符串
   * @return 校验结果
   */
  public static ValidationResult validateBidRequest(String json) {
    return validateBidRequest(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  /**
   * 校验 BidResponse JSON 字符串。
   *
   * @param json JSON 字符串
   * @return 校验结果
   */
  public static ValidationResult validateBidResponse(String json) {
    return validateBidResponse(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  private static ValidationResult validate(byte[] json, Schema schema, boolean checkNative) {
    final JsonNode node;
    try {
      node = MAPPER.readTree(json);
    } catch (IOException e) {
      return ValidationResult.fail(new ValidationError("parse", "", e.getMessage()));
    }
    List<ValidationError> errors = new ArrayList<>();
    for (Error err : schema.validate(node)) {
      errors.add(toError(err));
    }
    if (checkNative) {
      errors.addAll(validateNativeEmbedded(node));
    }
    if (errors.isEmpty()) {
      return ValidationResult.ok();
    }
    return ValidationResult.fail(errors);
  }

  private static ValidationError toError(Error err) {
    String path =
        err.getInstanceLocation() == null ? "" : err.getInstanceLocation().toString();
    if (path.equals("$")) {
      path = "";
    }
    return new ValidationError(classify(err.getMessage()), path, err.getMessage());
  }

  private static String classify(String message) {
    String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
    if (lower.contains("required")) {
      return "required";
    }
    if (lower.contains("type")) {
      return "type";
    }
    if (lower.contains("format") || lower.contains("pattern")) {
      return "format";
    }
    return "constraint";
  }

  private static List<ValidationError> validateNativeEmbedded(JsonNode root) {
    List<ValidationError> errors = new ArrayList<>();
    JsonNode imps = root.get("imp");
    if (imps == null || !imps.isArray()) {
      return errors;
    }
    for (int i = 0; i < imps.size(); i++) {
      JsonNode nativeNode = imps.get(i).get("native");
      if (nativeNode == null || !nativeNode.has("request")) {
        continue;
      }
      JsonNode req = nativeNode.get("request");
      if (!req.isTextual()) {
        continue;
      }
      String path = "/imp/" + i + "/native/request";
      try {
        JsonNode inner = MAPPER.readTree(req.asText());
        for (Error err : NATIVE.validate(inner)) {
          String sub =
              err.getInstanceLocation() == null ? "" : err.getInstanceLocation().toString();
          if (sub.equals("$")) {
            sub = "";
          }
          errors.add(
              new ValidationError(
                  "native", path + sub, "native.request: " + err.getMessage()));
        }
      } catch (IOException e) {
        errors.add(
            new ValidationError("native", path, "native.request is not JSON: " + e.getMessage()));
      }
    }
    return errors;
  }
}

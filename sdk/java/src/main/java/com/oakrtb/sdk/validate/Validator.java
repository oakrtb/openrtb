package com.oakrtb.sdk.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Validates BidRequest / BidResponse JSON against OakRTB JSON Schema. */
public final class Validator {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final JsonSchema REQUEST;
  private static final JsonSchema RESPONSE;
  private static final JsonSchema NATIVE;

  static {
    // Map $id URLs to classpath so validation works offline.
    JsonSchemaFactory factory =
        JsonSchemaFactory.getInstance(
            SpecVersion.VersionFlag.V202012,
            builder ->
                builder.schemaMappers(
                    mappers ->
                        mappers.mapPrefix(
                            "https://github.com/oakrtb/openrtb/schema/jsonschema",
                            "classpath:/schema/jsonschema")));
    REQUEST =
        factory.getSchema(
            SchemaLocation.of(
                "https://github.com/oakrtb/openrtb/schema/jsonschema/bid-request.schema.json"));
    RESPONSE =
        factory.getSchema(
            SchemaLocation.of(
                "https://github.com/oakrtb/openrtb/schema/jsonschema/bid-response.schema.json"));
    NATIVE =
        factory.getSchema(
            SchemaLocation.of(
                "https://github.com/oakrtb/openrtb/schema/jsonschema/native.schema.json"));
  }

  private Validator() {}

  public static ValidationResult validateBidRequest(byte[] json) {
    return validate(json, REQUEST, true);
  }

  public static ValidationResult validateBidResponse(byte[] json) {
    return validate(json, RESPONSE, false);
  }

  public static ValidationResult validateBidRequest(String json) {
    return validateBidRequest(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  public static ValidationResult validateBidResponse(String json) {
    return validateBidResponse(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  private static ValidationResult validate(byte[] json, JsonSchema schema, boolean checkNative) {
    final JsonNode node;
    try {
      node = MAPPER.readTree(json);
    } catch (IOException e) {
      return ValidationResult.fail(new ValidationError("parse", "", e.getMessage()));
    }
    List<ValidationError> errors = new ArrayList<>();
    Set<ValidationMessage> messages = schema.validate(node);
    for (ValidationMessage msg : messages) {
      errors.add(toError(msg));
    }
    if (checkNative) {
      errors.addAll(validateNativeEmbedded(node));
    }
    if (errors.isEmpty()) {
      return ValidationResult.ok();
    }
    return ValidationResult.fail(errors);
  }

  private static ValidationError toError(ValidationMessage msg) {
    String path = msg.getInstanceLocation() == null ? "" : msg.getInstanceLocation().toString();
    if (path.equals("$")) {
      path = "";
    }
    return new ValidationError(classify(msg.getMessage()), path, msg.getMessage());
  }

  private static String classify(String message) {
    String lower = message.toLowerCase(Locale.ROOT);
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
        for (ValidationMessage msg : NATIVE.validate(inner)) {
          String sub =
              msg.getInstanceLocation() == null ? "" : msg.getInstanceLocation().toString();
          if (sub.equals("$")) {
            sub = "";
          }
          errors.add(
              new ValidationError(
                  "native", path + sub, "native.request: " + msg.getMessage()));
        }
      } catch (IOException e) {
        errors.add(new ValidationError("native", path, "native.request is not JSON: " + e.getMessage()));
      }
    }
    return errors;
  }
}

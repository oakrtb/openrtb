package com.oakrtb.sdk.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OakRTB JSON Schema 校验入口。
 *
 * <p>离线使用 classpath 上的 schema 资源。热路径请用 view LightGate，勿与本类叠跑。
 */
public final class Schema {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final com.networknt.schema.Schema REQUEST;
  private static final com.networknt.schema.Schema RESPONSE;
  private static final com.networknt.schema.Schema NATIVE;

  static {
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

  private Schema() {}

  /** 校验 BidRequest JSON 字节。 */
  public static Report request(byte[] json) {
    return check(json, REQUEST, true);
  }

  /** 校验 BidResponse JSON 字节。 */
  public static Report response(byte[] json) {
    return check(json, RESPONSE, false);
  }

  /** 校验 BidRequest JSON 字符串。 */
  public static Report request(String json) {
    return request(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  /** 校验 BidResponse JSON 字符串。 */
  public static Report response(String json) {
    return response(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  private static Report check(
      byte[] json, com.networknt.schema.Schema schema, boolean checkNative) {
    final JsonNode node;
    try {
      node = MAPPER.readTree(json);
    } catch (IOException e) {
      return Report.fail(new Issue("parse", "", e.getMessage()));
    }
    List<Issue> errors = new ArrayList<>();
    for (com.networknt.schema.Error err : schema.validate(node)) {
      errors.add(toIssue(err));
    }
    if (checkNative) {
      errors.addAll(nativeEmbedded(node));
    }
    if (errors.isEmpty()) {
      return Report.ok();
    }
    return Report.fail(errors);
  }

  private static Issue toIssue(com.networknt.schema.Error err) {
    String path =
        err.getInstanceLocation() == null ? "" : err.getInstanceLocation().toString();
    if (path.equals("$")) {
      path = "";
    }
    return new Issue(classify(err.getMessage()), path, err.getMessage());
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

  private static List<Issue> nativeEmbedded(JsonNode root) {
    List<Issue> errors = new ArrayList<>();
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
        for (com.networknt.schema.Error err : NATIVE.validate(inner)) {
          String sub =
              err.getInstanceLocation() == null ? "" : err.getInstanceLocation().toString();
          if (sub.equals("$")) {
            sub = "";
          }
          errors.add(new Issue("native", path + sub, "native.request: " + err.getMessage()));
        }
      } catch (IOException e) {
        errors.add(
            new Issue("native", path, "native.request is not JSON: " + e.getMessage()));
      }
    }
    return errors;
  }
}

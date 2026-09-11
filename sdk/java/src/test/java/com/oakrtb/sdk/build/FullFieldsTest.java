package com.oakrtb.sdk.build;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.oakrtb.openrtb.v2.App;
import com.oakrtb.openrtb.v2.BidRequest;
import com.oakrtb.openrtb.v2.BidResponse;
import com.oakrtb.openrtb.v2.Dooh;
import com.oakrtb.openrtb.v2.MarkupType;
import com.oakrtb.openrtb.v2.Site;
import com.oakrtb.sdk.schema.Report;
import com.oakrtb.sdk.schema.Schema;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exhaustive set/get coverage: every proto field (except ext) is populated with a
 * schema-friendly value, then JSON round-tripped and validated.
 */
class FullFieldsTest {

  @Test
  void bidRequestWithApp_allFieldsSetAndValid() throws Exception {
    BidRequest.Builder b = BidRequest.newBuilder();
    fillMessage(b, Set.of("ext", "site", "dooh"));
    // Ensure inventory is App (fill may have skipped site/dooh).
    if (!b.hasApp()) {
      App.Builder app = App.newBuilder();
      fillMessage(app, Set.of("ext"));
      b.setApp(app);
    }
    BidRequest req = b.build();
    List<String> unset = unsetPaths(req, Set.of("ext", "site", "dooh"));
    assertTrue(unset.isEmpty(), () -> "unset: " + unset);

    byte[] json = Json.toJsonBytes(req);
    Report result = Schema.request(json);
    assertTrue(result.isOk(), result::toJson);

    BidRequest back = Json.parseBidRequest(new String(json));
    assertEquals(req.getId(), back.getId());
    assertEquals(req.getImpCount(), back.getImpCount());
    assertTrue(back.hasApp());
    assertFalse(back.hasSite());
    assertFalse(back.hasDooh());
  }

  @Test
  void bidRequestWithSite_valid() throws Exception {
    BidRequest.Builder b = BidRequest.newBuilder();
    fillMessage(b, Set.of("ext", "app", "dooh"));
    Site.Builder site = Site.newBuilder();
    fillMessage(site, Set.of("ext"));
    b.setSite(site);
    BidRequest req = b.build();
    byte[] json = Json.toJsonBytes(req);
    assertTrue(Schema.request(json).isOk(), () -> Schema.request(json).toJson());
    assertTrue(Json.parseBidRequest(new String(json)).hasSite());
  }

  @Test
  void bidRequestWithDooh_valid() throws Exception {
    BidRequest.Builder b = BidRequest.newBuilder();
    fillMessage(b, Set.of("ext", "app", "site"));
    Dooh.Builder dooh = Dooh.newBuilder();
    fillMessage(dooh, Set.of("ext"));
    b.setDooh(dooh);
    BidRequest req = b.build();
    byte[] json = Json.toJsonBytes(req);
    assertTrue(Schema.request(json).isOk(), () -> Schema.request(json).toJson());
    assertTrue(Json.parseBidRequest(new String(json)).hasDooh());
  }

  @Test
  void bidResponse_allFieldsSetAndValid() throws Exception {
    BidResponse.Builder b = BidResponse.newBuilder();
    fillMessage(b, Set.of("ext"));
    BidResponse res = b.build();
    List<String> unset = unsetPaths(res, Set.of("ext"));
    assertTrue(unset.isEmpty(), () -> "unset: " + unset);

    byte[] json = Json.toJsonBytes(res);
    Report result = Schema.response(json);
    assertTrue(result.isOk(), result::toJson);

    BidResponse back = Json.parseBidResponse(new String(json));
    assertEquals(res.getId(), back.getId());
    assertEquals(res.getSeatbidCount(), back.getSeatbidCount());
  }

  @Test
  void goldenFullFixturesValidate() throws Exception {
    Path root = repoRoot().resolve("testdata").resolve("full");
    assertTrue(
        Schema.request(Files.readAllBytes(root.resolve("bid-request/app.json")))
            .isOk());
    assertTrue(
        Schema.request(Files.readAllBytes(root.resolve("bid-request/site.json")))
            .isOk());
    assertTrue(
        Schema.request(Files.readAllBytes(root.resolve("bid-request/dooh.json")))
            .isOk());
    assertTrue(
        Schema.response(Files.readAllBytes(root.resolve("bid-response/full.json")))
            .isOk());
  }

  private static Path repoRoot() {
    Path cwd = Paths.get("").toAbsolutePath();
    if (cwd.getFileName().toString().equals("java")
        && cwd.getParent() != null
        && cwd.getParent().getFileName().toString().equals("sdk")) {
      return cwd.getParent().getParent();
    }
    return cwd;
  }

  /** Recursively fill all fields on a protobuf builder. */
  static void fillMessage(Message.Builder builder, Set<String> skip) {
    for (Descriptors.FieldDescriptor fd : builder.getDescriptorForType().getFields()) {
      if (skip.contains(fd.getName())) {
        continue;
      }
      if (fd.isRepeated()) {
        switch (fd.getJavaType()) {
          case MESSAGE -> {
            Message.Builder child = builder.newBuilderForField(fd);
            fillMessage(child, Set.of("ext"));
            builder.addRepeatedField(fd, child.build());
          }
          case STRING -> {
            if (fd.getName().equals("cur")) {
              builder.addRepeatedField(fd, "USD");
              builder.addRepeatedField(fd, "EUR");
            } else if (fd.getName().equals("mimes")) {
              String parent = builder.getDescriptorForType().getName();
              if (parent.equals("Video")) {
                builder.addRepeatedField(fd, "video/mp4");
              } else if (parent.equals("Audio")) {
                builder.addRepeatedField(fd, "audio/mpeg");
              } else {
                builder.addRepeatedField(fd, "image/jpeg");
              }
            } else {
              builder.addRepeatedField(fd, fd.getFullName() + ".a");
              builder.addRepeatedField(fd, fd.getFullName() + ".b");
            }
          }
          case INT -> {
            int v =
                switch (fd.getName()) {
                  case "protocols" -> 3;
                  case "playbackmethod" -> 1;
                  case "delivery" -> 1;
                  case "api", "apis" -> 5;
                  case "battr", "attr" -> 1;
                  case "poddedupe" -> 1;
                  case "gpp_sid" -> 2;
                  default -> 1;
                };
            builder.addRepeatedField(fd, v);
            builder.addRepeatedField(fd, v + 1);
          }
          case LONG -> builder.addRepeatedField(fd, 1L);
          case FLOAT -> builder.addRepeatedField(fd, 1.5f);
          case DOUBLE -> builder.addRepeatedField(fd, 1.5d);
          case BOOLEAN -> builder.addRepeatedField(fd, true);
          case ENUM -> builder.addRepeatedField(fd, nonZeroEnum(fd));
          default -> throw new IllegalStateException("unsupported repeated " + fd);
        }
        continue;
      }
      switch (fd.getJavaType()) {
        case MESSAGE -> {
          Message.Builder child = builder.newBuilderForField(fd);
          fillMessage(child, Set.of("ext"));
          builder.setField(fd, child.build());
        }
        case STRING -> builder.setField(fd, stringValue(builder, fd));
        case INT -> builder.setField(fd, intValue(builder, fd));
        case LONG -> builder.setField(fd, 7L);
        case FLOAT -> builder.setField(fd, 1.25f);
        case DOUBLE -> builder.setField(fd, doubleValue(fd));
        case BOOLEAN -> builder.setField(fd, true);
        case ENUM -> builder.setField(fd, nonZeroEnum(fd));
        case BYTE_STRING -> throw new IllegalStateException("bytes field " + fd);
      }
    }
  }

  private static Object nonZeroEnum(Descriptors.FieldDescriptor fd) {
    if (fd.getEnumType().getName().equals("MarkupType")) {
      return MarkupType.MARKUP_TYPE_BANNER.getValueDescriptor();
    }
    for (Descriptors.EnumValueDescriptor v : fd.getEnumType().getValues()) {
      if (v.getNumber() != 0) {
        return v;
      }
    }
    return fd.getEnumType().getValues().get(0);
  }

  private static String stringValue(Message.Builder parent, Descriptors.FieldDescriptor fd) {
    String name = fd.getName();
    String msg = parent.getDescriptorForType().getName();
    return switch (name) {
      case "request" ->
          "{\"ver\":\"1.2\",\"assets\":[{\"id\":1,\"required\":1,\"title\":{\"len\":90}}]}";
      case "bidfloorcur", "cur" -> "USD";
      case "language" -> "en";
      case "langb" -> "en-US";
      case "country" -> "USA";
      case "ip" -> "192.0.2.10";
      case "ipv6" -> "2001:db8::1";
      case "ver" -> msg.equals("Native") ? "1.2" : "1.0";
      case "page", "storeurl", "url", "nurl", "burl", "lurl", "iurl", "ref" ->
          "https://example.com/" + msg + "/" + name;
      default -> msg + "." + name + ".v";
    };
  }

  private static int intValue(Message.Builder parent, Descriptors.FieldDescriptor fd) {
    String name = fd.getName();
    // Flag / constrained enums from OakRTB schema
    return switch (name) {
      case "test",
          "allimps",
          "instl",
          "clickbrowser",
          "secure",
          "rwdd",
          "skip",
          "boxingallowed",
          "stitched",
          "guar",
          "private_auction",
          "livestream",
          "realtime",
          "firstbroadcast",
          "embeddable",
          "mobile",
          "privacypolicy",
          "paid",
          "dnt",
          "lmt",
          "js",
          "geofetch",
          "hp",
          "complete",
          "fd",
          "coppa",
          "gdpr",
          "group",
          "topframe",
          "vcm",
          "fixed" ->
          1;
      case "ssai" -> 2;
      case "at" -> 1;
      case "tmax" -> 120;
      case "cattax", "gtax" -> 6;
      case "startdelay" -> -1;
      case "plcmt", "linearity", "playbackend", "pos", "placement", "slotinpod", "podseq" -> 1;
      case "minduration" -> 5;
      case "maxduration" -> 30;
      case "w" -> parent.getDescriptorForType().getName().equals("Video") ? 1920 : 300;
      case "h" -> parent.getDescriptorForType().getName().equals("Video") ? 1080 : 250;
      case "devicetype" -> 4;
      case "connectiontype" -> 2;
      case "feed" -> 3;
      case "nvol", "prodq", "context", "qagmediarating", "sourcerelationship", "atype", "mm", "reftype" ->
          1;
      case "sourcetype" -> 1;
      case "nbr" -> 2;
      default -> 7;
    };
  }

  private static double doubleValue(Descriptors.FieldDescriptor fd) {
    return switch (fd.getName()) {
      case "price" -> 1.23;
      case "bidfloor" -> 0.5;
      case "multiplier" -> 2.5;
      case "pxratio" -> 2.0;
      case "lat" -> 37.77;
      case "lon" -> -122.42;
      default -> 3.14;
    };
  }

  static List<String> unsetPaths(Message msg, Set<String> skip) {
    List<String> out = new ArrayList<>();
    walkUnset(msg, "", skip, out);
    return out;
  }

  private static void walkUnset(Message msg, String prefix, Set<String> skip, List<String> out) {
    for (Descriptors.FieldDescriptor fd : msg.getDescriptorForType().getFields()) {
      String name = fd.getName();
      String path = prefix.isEmpty() ? name : prefix + "." + name;
      if (skip.contains(name)) {
        continue;
      }
      if (fd.isRepeated()) {
        int n = msg.getRepeatedFieldCount(fd);
        if (n == 0) {
          out.add(path);
          continue;
        }
        if (fd.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
          for (int i = 0; i < n; i++) {
            walkUnset((Message) msg.getRepeatedField(fd, i), path + "[]", Set.of("ext"), out);
          }
        }
        continue;
      }
      if (!msg.hasField(fd)) {
        out.add(path);
        continue;
      }
      if (fd.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
        walkUnset((Message) msg.getField(fd), path, Set.of("ext"), out);
      }
    }
  }
}

package com.oakrtb.sdk.schema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Schema / Report 单元测试。 */
class SchemaTest {

  private static Path repoRoot() {
    Path p = Path.of("").toAbsolutePath();
    if (p.getFileName().toString().equals("java")
        && p.getParent().getFileName().toString().equals("sdk")) {
      return p.getParent().getParent();
    }
    return p;
  }

  @Test
  void exampleBidRequestsPass() throws IOException {
    Path dir = repoRoot().resolve("examples/bid-request");
    try (Stream<Path> files = Files.walk(dir)) {
      files
          .filter(f -> f.toString().endsWith(".json"))
          .forEach(
              f -> {
                try {
                  byte[] json = Files.readAllBytes(f);
                  Report r = Schema.request(json);
                  assertTrue(r.isOk(), () -> f + ": " + r.toJson());
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }
  }

  @Test
  void exampleBidResponsesPass() throws IOException {
    Path dir = repoRoot().resolve("examples/bid-response");
    try (Stream<Path> files = Files.walk(dir)) {
      files
          .filter(f -> f.toString().endsWith(".json"))
          .forEach(
              f -> {
                try {
                  byte[] json = Files.readAllBytes(f);
                  Report r = Schema.response(json);
                  assertTrue(r.isOk(), () -> f + ": " + r.toJson());
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }
  }

  @Test
  void invalidFixturesRejected() throws IOException {
    Path root = repoRoot().resolve("testdata/invalid");
    try (Stream<Path> files = Files.walk(root)) {
      files
          .filter(f -> f.toString().endsWith(".json"))
          .forEach(
              f -> {
                try {
                  byte[] json = Files.readAllBytes(f);
                  boolean isReq = f.toString().contains("bid-request");
                  Report r = isReq ? Schema.request(json) : Schema.response(json);
                  assertFalse(r.isOk(), () -> f + " should fail");
                  assertFalse(r.getErrors().isEmpty(), () -> f + " needs errors");
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }
  }

  @Test
  void parseErrorOnBrokenJson() {
    Report r = Schema.request("{".getBytes(StandardCharsets.UTF_8));
    assertFalse(r.isOk());
    assertFalse(r.getErrors().isEmpty());
    assertTrue(
        r.getErrors().stream().anyMatch(e -> "parse".equals(e.getCode())), () -> r.toJson());
  }

  @Test
  void stringOverloadWorks() {
    Report bad = Schema.request("{");
    assertFalse(bad.isOk());
  }

  @Test
  void reportOkAndFail() {
    assertTrue(Report.ok().isOk());
    assertTrue(Report.ok().getErrors().isEmpty());
    Report fail = Report.fail(new Issue("x", "/id", "missing"));
    assertFalse(fail.isOk());
    assertTrue(fail.toJson().contains("\"ok\":false"));
    assertTrue(fail.toJsonBytes().length > 0);
  }
}

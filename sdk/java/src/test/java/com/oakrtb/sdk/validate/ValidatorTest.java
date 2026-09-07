package com.oakrtb.sdk.validate;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Validator / ValidationResult 单元测试。 */
class ValidatorTest {

  private static Path repoRoot() {
    Path p = Path.of("").toAbsolutePath();
    // surefire cwd 通常是 sdk/java
    if (p.getFileName().toString().equals("java") && p.getParent().getFileName().toString().equals("sdk")) {
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
                  ValidationResult r = Validator.validateBidRequest(json);
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
                  ValidationResult r = Validator.validateBidResponse(json);
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
                  ValidationResult r =
                      isReq
                          ? Validator.validateBidRequest(json)
                          : Validator.validateBidResponse(json);
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
    ValidationResult r = Validator.validateBidRequest("{".getBytes(StandardCharsets.UTF_8));
    assertFalse(r.isOk());
    assertFalse(r.getErrors().isEmpty());
    assertTrue(
        r.getErrors().stream().anyMatch(e -> "parse".equals(e.getCode())),
        () -> r.toJson());
  }

  @Test
  void stringOverloadWorks() {
    ValidationResult bad = Validator.validateBidRequest("{");
    assertFalse(bad.isOk());
  }

  @Test
  void validationResultOkAndFail() {
    assertTrue(ValidationResult.ok().isOk());
    assertTrue(ValidationResult.ok().getErrors().isEmpty());
    ValidationResult fail =
        ValidationResult.fail(new ValidationError("x", "/id", "missing"));
    assertFalse(fail.isOk());
    assertTrue(fail.toJson().contains("\"ok\":false"));
    assertTrue(fail.toJsonBytes().length > 0);
  }
}

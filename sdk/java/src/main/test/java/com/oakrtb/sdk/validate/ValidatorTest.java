package com.oakrtb.sdk.validate;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorTest {
  private static Path repoRoot() {
    Path cwd = Paths.get("").toAbsolutePath();
    // sdk/java when running Maven
    if (cwd.getFileName().toString().equals("java")
        && cwd.getParent() != null
        && cwd.getParent().getFileName().toString().equals("sdk")) {
      return cwd.getParent().getParent();
    }
    return cwd;
  }

  @Test
  void examplesPass() throws IOException {
    Path examples = repoRoot().resolve("examples");
    try (Stream<Path> walk = Files.walk(examples)) {
      walk.filter(p -> p.toString().endsWith(".json"))
          .forEach(
              path -> {
                try {
                  byte[] data = Files.readAllBytes(path);
                  boolean request = path.getParent().getFileName().toString().equals("bid-request");
                  ValidationResult result =
                      request
                          ? Validator.validateBidRequest(data)
                          : Validator.validateBidResponse(data);
                  assertTrue(result.isOk(), path + " -> " + result.toJson());
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }
  }

  @Test
  void invalidRejected() throws IOException {
    Path invalid = repoRoot().resolve("testdata").resolve("invalid");
    try (Stream<Path> walk = Files.walk(invalid)) {
      walk.filter(p -> p.toString().endsWith(".json"))
          .forEach(
              path -> {
                try {
                  byte[] data = Files.readAllBytes(path);
                  boolean request = path.getParent().getFileName().toString().equals("bid-request");
                  ValidationResult result =
                      request
                          ? Validator.validateBidRequest(data)
                          : Validator.validateBidResponse(data);
                  assertFalse(result.isOk(), path + " should fail");
                  assertFalse(result.getErrors().isEmpty());
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }
  }

  @Test
  void parseError() {
    ValidationResult result = Validator.validateBidRequest("{");
    assertFalse(result.isOk());
    assertTrue(result.getErrors().stream().anyMatch(e -> "parse".equals(e.getCode())));
  }
}

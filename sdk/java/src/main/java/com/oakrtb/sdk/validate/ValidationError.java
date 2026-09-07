package com.oakrtb.sdk.validate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/** One failing validation check. */
public final class ValidationError {
  private final String code;
  private final String path;
  private final String message;

  @JsonCreator
  public ValidationError(
      @JsonProperty("code") String code,
      @JsonProperty("path") String path,
      @JsonProperty("message") String message) {
    this.code = Objects.requireNonNull(code, "code");
    this.path = path == null ? "" : path;
    this.message = Objects.requireNonNull(message, "message");
  }

  public String getCode() {
    return code;
  }

  public String getPath() {
    return path;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ValidationError)) {
      return false;
    }
    ValidationError that = (ValidationError) o;
    return Objects.equals(code, that.code)
        && Objects.equals(path, that.path)
        && Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, path, message);
  }
}

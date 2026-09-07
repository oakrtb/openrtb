package com.oakrtb.sdk.validate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * 单条校验失败项。
 */
public final class ValidationError {
  private final String code;
  private final String path;
  private final String message;

  /**
   * 构造校验错误。
   *
   * @param code 错误分类（如 required、type、format）
   * @param path JSON 路径
   * @param message 详细消息
   */
  @JsonCreator
  public ValidationError(
      @JsonProperty("code") String code,
      @JsonProperty("path") String path,
      @JsonProperty("message") String message) {
    this.code = Objects.requireNonNull(code, "code");
    this.path = path == null ? "" : path;
    this.message = Objects.requireNonNull(message, "message");
  }

  /**
   * 错误分类码。
   *
   * @return code
   */
  public String getCode() {
    return code;
  }

  /**
   * JSON 实例路径。
   *
   * @return path
   */
  public String getPath() {
    return path;
  }

  /**
   * 人类可读错误消息。
   *
   * @return message
   */
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

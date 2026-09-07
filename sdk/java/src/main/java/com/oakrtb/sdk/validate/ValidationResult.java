package com.oakrtb.sdk.validate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 统一的 JSON Schema 校验结果（{@code ok} 为 false 时可作 HTTP 400 响应体）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ValidationResult {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final boolean ok;
  private final List<ValidationError> errors;

  /**
   * Jackson 反序列化构造。
   *
   * @param ok 是否通过
   * @param errors 错误列表
   */
  public ValidationResult(
      @JsonProperty("ok") boolean ok,
      @JsonProperty("errors") List<ValidationError> errors) {
    this.ok = ok;
    this.errors =
        errors == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(errors));
  }

  /**
   * 创建成功结果。
   *
   * @return ok=true 的实例
   */
  public static ValidationResult ok() {
    return new ValidationResult(true, Collections.emptyList());
  }

  /**
   * 由错误列表创建失败结果。
   *
   * @param errors 错误项
   * @return ok=false 的实例
   */
  public static ValidationResult fail(List<ValidationError> errors) {
    return new ValidationResult(false, errors);
  }

  /**
   * 由可变参数创建失败结果。
   *
   * @param errors 一个或多个错误
   * @return ok=false 的实例
   */
  public static ValidationResult fail(ValidationError... errors) {
    List<ValidationError> list = new ArrayList<>();
    Collections.addAll(list, errors);
    return fail(list);
  }

  /**
   * 是否通过校验。
   *
   * @return 通过为 true
   */
  @JsonProperty("ok")
  public boolean isOk() {
    return ok;
  }

  /**
   * 校验错误列表（成功时为空）。
   *
   * @return 不可变错误列表
   */
  @JsonProperty("errors")
  public List<ValidationError> getErrors() {
    return errors;
  }

  /**
   * 序列化为标准 ValidationResult JSON 字符串。
   *
   * @return JSON 字符串
   */
  public String toJson() {
    try {
      return MAPPER.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * 序列化为 UTF-8 JSON 字节。
   *
   * @return JSON 字节
   */
  public byte[] toJsonBytes() {
    try {
      return MAPPER.writeValueAsBytes(this);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ValidationResult)) {
      return false;
    }
    ValidationResult that = (ValidationResult) o;
    return ok == that.ok && Objects.equals(errors, that.errors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ok, errors);
  }
}

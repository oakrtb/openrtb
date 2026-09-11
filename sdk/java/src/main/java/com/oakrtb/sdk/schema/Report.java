package com.oakrtb.sdk.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * JSON Schema 校验结果（{@code ok} 为 false 时可作 HTTP 400 响应体）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Report {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final boolean ok;
  private final List<Issue> errors;

  public Report(
      @JsonProperty("ok") boolean ok, @JsonProperty("errors") List<Issue> errors) {
    this.ok = ok;
    this.errors =
        errors == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(errors));
  }

  /** 成功结果。 */
  public static Report ok() {
    return new Report(true, Collections.emptyList());
  }

  /** 失败结果。 */
  public static Report fail(List<Issue> errors) {
    return new Report(false, errors);
  }

  /** 失败结果。 */
  public static Report fail(Issue... errors) {
    List<Issue> list = new ArrayList<>();
    Collections.addAll(list, errors);
    return fail(list);
  }

  @JsonProperty("ok")
  public boolean isOk() {
    return ok;
  }

  @JsonProperty("errors")
  public List<Issue> getErrors() {
    return errors;
  }

  /** 序列化为 {@code {"ok", "errors"}} JSON。 */
  public String toJson() {
    try {
      return MAPPER.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

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
    if (!(o instanceof Report)) {
      return false;
    }
    Report that = (Report) o;
    return ok == that.ok && Objects.equals(errors, that.errors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ok, errors);
  }
}

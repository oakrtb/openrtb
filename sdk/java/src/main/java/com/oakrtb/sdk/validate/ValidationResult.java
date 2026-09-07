package com.oakrtb.sdk.validate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Unified validation outcome (HTTP 400 body when {@code ok} is false). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ValidationResult {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final boolean ok;
  private final List<ValidationError> errors;

  public ValidationResult(
      @JsonProperty("ok") boolean ok,
      @JsonProperty("errors") List<ValidationError> errors) {
    this.ok = ok;
    this.errors =
        errors == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(errors));
  }

  public static ValidationResult ok() {
    return new ValidationResult(true, Collections.emptyList());
  }

  public static ValidationResult fail(List<ValidationError> errors) {
    return new ValidationResult(false, errors);
  }

  public static ValidationResult fail(ValidationError... errors) {
    List<ValidationError> list = new ArrayList<>();
    Collections.addAll(list, errors);
    return fail(list);
  }

  @JsonProperty("ok")
  public boolean isOk() {
    return ok;
  }

  @JsonProperty("errors")
  public List<ValidationError> getErrors() {
    return errors;
  }

  /** Serialize to the standard ValidationResult JSON. */
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

package com.oakrtb.sdk.build;

import com.oakrtb.sdk.validate.ValidationResult;

/**
 * {@link #buildValidated()} 的返回封装：OpenRTB JSON 字节与 Schema 校验结果。
 *
 * <p>适用于构建后立即判断 payload 是否可发送或需修正。
 *
 * @param json 序列化后的 JSON 字节
 * @param result JSON Schema 校验结果
 */
public record ValidatedPayload(byte[] json, ValidationResult result) {
  /**
   * 校验是否通过（{@link ValidationResult#isOk()} 为 true）。
   *
   * @return 通过时为 {@code true}
   */
  public boolean ok() {
    return result != null && result.isOk();
  }
}

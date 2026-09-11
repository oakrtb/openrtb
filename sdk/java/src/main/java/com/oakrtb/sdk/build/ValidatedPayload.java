package com.oakrtb.sdk.build;

import com.oakrtb.sdk.schema.Report;

/**
 * {@link #buildValidated()} 的返回封装：OpenRTB JSON 字节与 Schema 校验结果。
 *
 * @param json 序列化后的 JSON 字节
 * @param result Schema 校验报告
 */
public record ValidatedPayload(byte[] json, Report result) {
  /**
   * 校验是否通过（{@link Report#isOk()} 为 true）。
   *
   * @return 通过时为 {@code true}
   */
  public boolean ok() {
    return result != null && result.isOk();
  }
}

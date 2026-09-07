package com.oakrtb.sdk.fit;

import java.util.Objects;

/**
 * {@link Fit} 产生的一条发现项。
 *
 * @param code 稳定错误码（见 {@link IssueCode}）
 * @param severity 严重级别
 * @param path JSON 风格路径
 * @param message 人类可读说明
 */
public record FitIssue(String code, Severity severity, String path, String message) {
  public FitIssue {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(severity, "severity");
    path = path == null ? "" : path;
    message = message == null ? "" : message;
  }
}

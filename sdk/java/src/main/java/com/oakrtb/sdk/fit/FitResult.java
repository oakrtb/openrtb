package com.oakrtb.sdk.fit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Fit 检查的聚合结果。{@link #ok()} 在无 {@link Severity#ERROR} 时为 true。
 */
public final class FitResult {
  private final List<FitIssue> issues;

  private FitResult(List<FitIssue> issues) {
    this.issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
  }

  /**
   * 由问题列表构造结果。
   *
   * @param issues 发现项列表
   * @return FitResult
   */
  public static FitResult of(List<FitIssue> issues) {
    return new FitResult(issues);
  }

  /**
   * 空结果（无任何问题）。
   *
   * @return 空的 FitResult
   */
  public static FitResult empty() {
    return new FitResult(List.of());
  }

  /**
   * 全部发现项。
   *
   * @return 不可变问题列表
   */
  public List<FitIssue> issues() {
    return issues;
  }

  /**
   * 无 ERROR 时为 true（仍可能有 WARN）。
   *
   * @return 通过为 true
   */
  public boolean ok() {
    for (FitIssue i : issues) {
      if (i.severity() == Severity.ERROR) {
        return false;
      }
    }
    return true;
  }

  /**
   * 仅 ERROR 级别问题。
   *
   * @return ERROR 列表
   */
  public List<FitIssue> errors() {
    List<FitIssue> out = new ArrayList<>();
    for (FitIssue i : issues) {
      if (i.severity() == Severity.ERROR) {
        out.add(i);
      }
    }
    return Collections.unmodifiableList(out);
  }

  /**
   * 仅 WARN 级别问题。
   *
   * @return WARN 列表
   */
  public List<FitIssue> warnings() {
    List<FitIssue> out = new ArrayList<>();
    for (FitIssue i : issues) {
      if (i.severity() == Severity.WARN) {
        out.add(i);
      }
    }
    return Collections.unmodifiableList(out);
  }

  /**
   * 是否包含指定错误码。
   *
   * @param code {@link IssueCode} 常量
   * @return 存在时为 true
   */
  public boolean has(String code) {
    for (FitIssue i : issues) {
      if (code.equals(i.code())) {
        return true;
      }
    }
    return false;
  }
}

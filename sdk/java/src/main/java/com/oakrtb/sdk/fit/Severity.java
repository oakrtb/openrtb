package com.oakrtb.sdk.fit;

/**
 * {@link FitIssue} 的严重级别。
 */
public enum Severity {
  /** 结构性不匹配——调用方应拒绝或重写出价。 */
  ERROR,
  /** 业务风险——载荷可能仍合法；按策略记录或过滤。 */
  WARN
}

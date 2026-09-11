package com.oakrtb.sdk.fit;

/**
 * Fit API 的稳定字符串错误码，与 Go/Rust SDK 保持一致。
 *
 * <p>供 {@link FitIssue#code()} 与 {@link FitResult#has(String)} 使用。
 */
public final class IssueCode {
  private IssueCode() {}

  /** 所选格式不在 Imp 上。 */
  public static final String FORMAT_NOT_ON_IMP = "FORMAT_NOT_ON_IMP";
  /** MarkupMask 须为单 bit。 */
  public static final String MULTI_NEEDS_CHOICE = "MULTI_NEEDS_CHOICE";
  /** Banner 缺少 w/h 或 format[]。 */
  public static final String BANNER_SIZE_MISSING = "BANNER_SIZE_MISSING";
  /** Video 缺少 mimes。 */
  public static final String VIDEO_MIMES_MISSING = "VIDEO_MIMES_MISSING";
  /** Audio 缺少 mimes。 */
  public static final String AUDIO_MIMES_MISSING = "AUDIO_MIMES_MISSING";
  /** Native 缺少 request。 */
  public static final String NATIVE_REQUEST_MISSING = "NATIVE_REQUEST_MISSING";
  /** Video 未设置时长范围（警告）。 */
  public static final String VIDEO_DURATION_UNSET = "VIDEO_DURATION_UNSET";
  /** Video 未设置 protocols（警告）。 */
  public static final String VIDEO_PROTOCOLS_UNSET = "VIDEO_PROTOCOLS_UNSET";

  /** 响应/载荷结构无法解析（如非 JSON object）。 */
  public static final String MALFORMED = "MALFORMED";
  /** bid.impid 在请求中不存在。 */
  public static final String IMP_NOT_FOUND = "IMP_NOT_FOUND";
  /** 多格式 Imp 须指定 Bid.mtype。 */
  public static final String MTYPE_REQUIRED = "MTYPE_REQUIRED";
  /** Bid.mtype 与 Imp 格式不匹配。 */
  public static final String MTYPE_MISMATCH = "MTYPE_MISMATCH";
  /** mtype 不在 1–4 且非 vendor 范围。 */
  public static final String MTYPE_UNKNOWN = "MTYPE_UNKNOWN";
  /** 厂商扩展 mtype ≥500（警告）。 */
  public static final String MTYPE_VENDOR = "MTYPE_VENDOR";
  /** 出价低于底价（警告）。 */
  public static final String PRICE_BELOW_FLOOR = "PRICE_BELOW_FLOOR";
  /** 出价货币与 imp.bidfloorcur 不同，跳过底价比较（警告）。 */
  public static final String FLOOR_CUR_DIFF = "FLOOR_CUR_DIFF";
  /** bid.attr 与格式 battr 冲突（警告）。 */
  public static final String ATTR_BLOCKED = "ATTR_BLOCKED";
  /** adomain 命中 BidRequest.badv（警告）。 */
  public static final String ADOMAIN_BLOCKED = "ADOMAIN_BLOCKED";
  /** bundle 命中 BidRequest.bapp（警告）。 */
  public static final String BUNDLE_BLOCKED = "BUNDLE_BLOCKED";
  /** cat 命中 BidRequest.bcat（警告）。 */
  public static final String CAT_BLOCKED = "CAT_BLOCKED";
  /** 响应货币不在 BidRequest.cur 中（警告）。 */
  public static final String CUR_NOT_ALLOWED = "CUR_NOT_ALLOWED";
  /** 已超过请求 deadline（警告）。 */
  public static final String PAST_DEADLINE = "PAST_DEADLINE";
}

package com.oakrtb.sdk.inspect;

/**
 * Imp/Bid 的 markup 类型位掩码（banner / video / audio / native）。
 *
 * <p>用于在 inspect 与 fit 流程中快速判断展示位或出价包含哪些广告格式。
 *
 * <p><b>注意：</b>本类型与 proto {@code Banner.Format}（尺寸格式列表）无关；此处 {@code BANNER} 等常量表示
 * OpenRTB 广告格式位，对应 {@code Bid.mtype} 1–4。
 */
public final class MarkupMask {
  /** 无任何格式位。 */
  public static final int NONE = 0;
  /** Banner 格式位，对应 Bid.mtype=1。 */
  public static final int BANNER = 1 << 0;
  /** Video 格式位，对应 Bid.mtype=2。 */
  public static final int VIDEO = 1 << 1;
  /** Audio 格式位，对应 Bid.mtype=3。 */
  public static final int AUDIO = 1 << 2;
  /** Native 格式位，对应 Bid.mtype=4。 */
  public static final int NATIVE = 1 << 3;

  private final int bits;

  private MarkupMask(int bits) {
    this.bits = bits;
  }

  /**
   * 由原始位值创建掩码。
   *
   * @param bits 位组合（通常为 {@link #BANNER} 等常量的或运算结果）
   * @return MarkupMask 实例
   */
  public static MarkupMask of(int bits) {
    return new MarkupMask(bits);
  }

  /**
   * 返回原始位值。
   *
   * @return 位掩码整数
   */
  public int bits() {
    return bits;
  }

  /**
   * 判断是否包含指定位标志。
   *
   * @param flag {@link #BANNER}、{@link #VIDEO} 等单 bit 常量
   * @return 包含时为 {@code true}
   */
  public boolean has(int flag) {
    return (bits & flag) != 0;
  }

  /**
   * 是否包含 Banner 格式。
   *
   * @return 包含 Banner 时为 {@code true}
   */
  public boolean hasBanner() {
    return has(BANNER);
  }

  /**
   * 是否包含 Video 格式。
   *
   * @return 包含 Video 时为 {@code true}
   */
  public boolean hasVideo() {
    return has(VIDEO);
  }

  /**
   * 是否包含 Audio 格式。
   *
   * @return 包含 Audio 时为 {@code true}
   */
  public boolean hasAudio() {
    return has(AUDIO);
  }

  /**
   * 是否包含 Native 格式。
   *
   * @return 包含 Native 时为 {@code true}
   */
  public boolean hasNative() {
    return has(NATIVE);
  }

  /**
   * 统计已置位的格式数量。
   *
   * @return 格式个数（0–4）
   */
  public int count() {
    return Integer.bitCount(bits);
  }

  /**
   * 当且仅当恰好一种格式时返回该格式的 bit；否则返回 {@link #NONE}。
   *
   * @return 单一格式 bit 或 NONE
   */
  public int primary() {
    return count() == 1 ? bits : NONE;
  }

  /**
   * 将单一格式映射为 OpenRTB {@code Bid.mtype}（1–4）；多格式或无格式时返回 0。
   *
   * @return mtype 数值
   */
  public int mtype() {
    return switch (primary()) {
      case BANNER -> 1;
      case VIDEO -> 2;
      case AUDIO -> 3;
      case NATIVE -> 4;
      default -> 0;
    };
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof MarkupMask f && f.bits == bits;
  }

  @Override
  public int hashCode() {
    return bits;
  }

  /**
   * 返回调试字符串 {@code MarkupMask(bits)}。
   *
   * @return 字符串表示
   */
  @Override
  public String toString() {
    return "MarkupMask(" + bits + ")";
  }
}

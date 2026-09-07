package com.oakrtb.sdk.inspect;

/**
 * 竞价请求的库存面（site / app / dooh，三者互斥）。
 *
 * <p>用于 {@link RequestInspect} 与 {@link RequestPipeline} 判断流量来源类型。
 *
 * <p><b>注意：</b>本枚举与 proto {@code Content.Channel} 无关，仅表示 BidRequest 顶层的库存对象类型。
 */
public enum Inventory {
  /** 未设置 site/app/dooh。 */
  NONE,
  /** Web 站点库存。 */
  SITE,
  /** 移动应用库存。 */
  APP,
  /** 数字户外（DOOH）库存。 */
  DOOH;

  /**
   * 返回小写 wire 风格字符串（site/app/dooh/none）。
   *
   * @return 库存类型字符串
   */
  @Override
  public String toString() {
    return switch (this) {
      case SITE -> "site";
      case APP -> "app";
      case DOOH -> "dooh";
      default -> "none";
    };
  }
}

package com.oakrtb.sdk.build;

import com.oakrtb.openrtb.v2.Bid;
import com.oakrtb.openrtb.v2.BidResponse;
import com.oakrtb.openrtb.v2.MarkupType;
import com.oakrtb.openrtb.v2.SeatBid;
import com.oakrtb.sdk.schema.Report;
import com.oakrtb.sdk.schema.Schema;

import java.util.Objects;

/**
 * OpenRTB {@link BidResponse} 的流式构建器。
 *
 * <p>适用于 DSP 侧在竞价结束后组装出价响应或结构化 no-bid；在 {@link #build()} 前做必要字段校验。
 */
public final class BidResponseBuilder {
  private final BidResponse.Builder res = BidResponse.newBuilder().setCur("USD");
  private boolean noBidSet;
  private String error;

  private BidResponseBuilder(String requestId) {
    if (requestId == null || requestId.isBlank()) {
      error = "BidResponse.id is required (echo BidRequest.id)";
    } else {
      res.setId(requestId);
    }
  }

  /**
   * 创建响应构建器，{@code id} 须回显对应 {@link BidRequest} 的 id。
   *
   * @param requestId 竞价请求 id（必填，非空）
   * @return 新的构建器实例
   */
  public static BidResponseBuilder create(String requestId) {
    return new BidResponseBuilder(requestId);
  }

  /**
   * 设置响应级 bid id（可选，用于日志/对账）。
   *
   * @param bidid 出价 id
   * @return 当前构建器，便于链式调用
   */
  public BidResponseBuilder bidId(String bidid) {
    res.setBidid(bidid);
    return this;
  }

  /**
   * 设置响应货币（ISO-4217，如 {@code USD}）。
   *
   * @param cur 货币代码
   * @return 当前构建器，便于链式调用
   */
  public BidResponseBuilder currency(String cur) {
    res.setCur(cur);
    return this;
  }

  /**
   * 声明结构化 no-bid，并清空已有 {@code seatbid}。
   *
   * @param nbr no-bid 原因码（OpenRTB 枚举值）
   * @return 当前构建器，便于链式调用
   */
  public BidResponseBuilder noBid(int nbr) {
    res.setNbr(nbr);
    res.clearSeatbid();
    noBidSet = true;
    return this;
  }

  /**
   * 追加一个 {@link SeatBid}，内含一条或多条 {@link Bid}。
   *
   * <p>调用后会清除 no-bid 状态；{@code bids} 至少需一条。
   *
   * @param seat 席位标识
   * @param bids 该席位下的出价列表
   * @return 当前构建器，便于链式调用
   * @throws IllegalArgumentException 当 {@code bids} 为空时
   */
  public BidResponseBuilder addSeatBid(String seat, Bid... bids) {
    if (bids == null || bids.length == 0) {
      throw new IllegalArgumentException("SeatBid requires at least one Bid");
    }
    // Switching to a bid clears structured no-bid.
    noBidSet = false;
    res.clearNbr();
    SeatBid.Builder sb = SeatBid.newBuilder().setSeat(seat);
    for (Bid bid : bids) {
      sb.addBid(Objects.requireNonNull(bid));
    }
    res.addSeatbid(sb);
    return this;
  }

  /**
   * 构建 protobuf {@link BidResponse}，并校验 id、cur、seatbid/no-bid 及每条 bid 的 id/impid/price。
   *
   * @return 构建完成的响应
   * @throws IllegalStateException 当必填字段缺失或 bid 不合法时
   */
  public BidResponse build() {
    if (error != null) {
      throw new IllegalStateException(error);
    }
    if (res.getCur().isEmpty()) {
      throw new IllegalStateException("BidResponse.cur is required (ISO-4217)");
    }
    if (res.getSeatbidCount() == 0 && !noBidSet) {
      throw new IllegalStateException("BidResponse needs seatbid[] or noBid(nbr)");
    }
    for (int i = 0; i < res.getSeatbidCount(); i++) {
      SeatBid sb = res.getSeatbid(i);
      for (int j = 0; j < sb.getBidCount(); j++) {
        Bid bid = sb.getBid(j);
        if (bid.getId().isEmpty() || bid.getImpid().isEmpty()) {
          throw new IllegalStateException("seatbid[" + i + "].bid[" + j + "] requires id and impid");
        }
        if (bid.getPrice() <= 0) {
          throw new IllegalStateException("seatbid[" + i + "].bid[" + j + "].price must be > 0");
        }
      }
    }
    return res.build();
  }

  /**
   * 构建响应并序列化为 OpenRTB JSON 字节数组。
   *
   * @return UTF-8 JSON 字节
   */
  public byte[] buildJson() {
    return Json.toJsonBytes(build());
  }

  /**
   * 构建 JSON 并执行 JSON Schema 校验，返回载荷与校验结果。
   *
   * @return 含 JSON 与 {@link Report} 的封装
   */
  public ValidatedPayload buildValidated() {
    byte[] json = buildJson();
    return new ValidatedPayload(json, Schema.response(json));
  }

  /**
   * 创建单条 {@link Bid} 的流式构建器。
   *
   * @param id 出价 id（必填）
   * @param impid 对应 Imp.id（必填）
   * @param price CPM 价格，须 &gt; 0
   * @return {@link BidBuilder} 实例
   */
  public static BidBuilder bid(String id, String impid, double price) {
    return new BidBuilder(id, impid, price);
  }

  /**
   * 单条 {@link Bid} 的流式构建器，用于 {@link #addSeatBid(String, Bid...)}。
   */
  public static final class BidBuilder {
    private final Bid.Builder b = Bid.newBuilder();

    BidBuilder(String id, String impid, double price) {
      b.setId(id).setImpid(impid).setPrice(price);
    }

    /**
     * 设置广告 markup（adm）。
     *
     * @param adm 广告创意内容
     * @return 当前构建器
     */
    public BidBuilder adm(String adm) {
      b.setAdm(adm);
      return this;
    }

    /**
     * 设置 win notice URL（nurl）。
     *
     * @param u 胜出通知 URL
     * @return 当前构建器
     */
    public BidBuilder nurl(String u) {
      b.setNurl(u);
      return this;
    }

    /**
     * 设置 billing notice URL（burl）。
     *
     * @param u 计费通知 URL
     * @return 当前构建器
     */
    public BidBuilder burl(String u) {
      b.setBurl(u);
      return this;
    }

    /**
     * 设置创意 id（crid）。
     *
     * @param crid 创意标识
     * @return 当前构建器
     */
    public BidBuilder crid(String crid) {
      b.setCrid(crid);
      return this;
    }

    /**
     * 设置 Campaign id（cid）。
     *
     * @param cid 活动标识
     * @return 当前构建器
     */
    public BidBuilder cid(String cid) {
      b.setCid(cid);
      return this;
    }

    /**
     * 设置广告主域名列表（adomain）。
     *
     * @param domains 一个或多个域名
     * @return 当前构建器
     */
    public BidBuilder adomain(String... domains) {
      b.clearAdomain();
      for (String d : domains) {
        b.addAdomain(d);
      }
      return this;
    }

    /**
     * 设置创意宽高（w/h）。
     *
     * @param w 宽度（像素）
     * @param h 高度（像素）
     * @return 当前构建器
     */
    public BidBuilder size(int w, int h) {
      b.setW(w).setH(h);
      return this;
    }

    /**
     * 设置 PMP deal id（dealid）。
     *
     * @param id 交易 id
     * @return 当前构建器
     */
    public BidBuilder dealId(String id) {
      b.setDealid(id);
      return this;
    }

    /**
     * 设置 markup 类型（对应 proto {@link MarkupType} / Bid.mtype）。
     *
     * @param m markup 类型枚举
     * @return 当前构建器
     */
    public BidBuilder markupType(MarkupType m) {
      b.setMtype(m);
      return this;
    }

    /**
     * 将 markup 类型设为 Banner（mtype=1）。
     *
     * @return 当前构建器
     */
    public BidBuilder banner() {
      return markupType(MarkupType.MARKUP_TYPE_BANNER);
    }

    /**
     * 将 markup 类型设为 Video（mtype=2）。
     *
     * @return 当前构建器
     */
    public BidBuilder video() {
      return markupType(MarkupType.MARKUP_TYPE_VIDEO);
    }

    /**
     * 将 markup 类型设为 Audio（mtype=3）。
     *
     * @return 当前构建器
     */
    public BidBuilder audio() {
      return markupType(MarkupType.MARKUP_TYPE_AUDIO);
    }

    /**
     * 将 markup 类型设为 Native（mtype=4）。
     *
     * @return 当前构建器
     */
    public BidBuilder nativeAd() {
      return markupType(MarkupType.MARKUP_TYPE_NATIVE);
    }

    /**
     * 设置创意时长（秒），常用于 video/audio。
     *
     * @param seconds 时长（秒）
     * @return 当前构建器
     */
    public BidBuilder dur(int seconds) {
      b.setDur(seconds);
      return this;
    }

    /**
     * 构建 protobuf {@link Bid}。
     *
     * @return 单条出价对象
     */
    public Bid build() {
      return b.build();
    }
  }
}

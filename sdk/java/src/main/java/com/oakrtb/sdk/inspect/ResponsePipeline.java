package com.oakrtb.sdk.inspect;

import com.oakrtb.openrtb.v2.BidResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 按固定顺序编排 BidResponse inspect 流程，并暴露 {@link Snapshot}。
 *
 * <ol>
 *   <li>{@link #lightGate}
 *   <li>{@link #pinShared}
 *   <li>{@link #viewBids}
 * </ol>
 */
public final class ResponsePipeline {
  private final BidResponse res;
  private ResponseInspect.SharedView shared;
  private List<ResponseInspect.SeatBidView> seatBids = List.of();
  private List<ResponseInspect.BidView> bids = List.of();
  private boolean gated;
  private boolean pinned;
  private boolean viewed;

  private ResponsePipeline(BidResponse res) {
    this.res = Objects.requireNonNull(res, "BidResponse");
  }

  /**
   * 对已解码的 BidResponse 开始编排。
   *
   * @param res BidResponse
   * @return 管道实例
   */
  public static ResponsePipeline of(BidResponse res) {
    return new ResponsePipeline(res);
  }

  /**
   * 按序执行全部步骤并返回快照。
   *
   * @param res BidResponse
   * @return Snapshot
   */
  public static Snapshot run(BidResponse res) {
    return of(res).lightGate().pinShared().viewBids().snapshot();
  }

  /**
   * 步骤 1：结构校验。
   *
   * @return 当前管道
   */
  public ResponsePipeline lightGate() {
    ResponseInspect.lightGate(res);
    gated = true;
    return this;
  }

  /**
   * 步骤 2：固定 SharedView（须先 lightGate）。
   *
   * @return 当前管道
   */
  public ResponsePipeline pinShared() {
    requireGated();
    shared = ResponseInspect.pinShared(res);
    pinned = true;
    return this;
  }

  /**
   * 步骤 3：生成 SeatBid/Bid 视图（须先 pinShared）。
   *
   * @return 当前管道
   */
  public ResponsePipeline viewBids() {
    requirePinned();
    ResponseInspect.BidLists lists = ResponseInspect.viewBidLists(res);
    seatBids = lists.seatBids();
    bids = lists.bids();
    viewed = true;
    return this;
  }

  /**
   * 完成管道；跳过步骤将自动补跑。
   *
   * @return Snapshot
   */
  public Snapshot snapshot() {
    if (!gated) {
      lightGate();
    }
    if (!pinned) {
      pinShared();
    }
    if (!viewed) {
      viewBids();
    }
    return new Snapshot(shared, seatBids, bids);
  }

  private void requireGated() {
    if (!gated) {
      throw new IllegalStateException("pipeline: call lightGate() first");
    }
  }

  private void requirePinned() {
    requireGated();
    if (!pinned) {
      throw new IllegalStateException("pipeline: call pinShared() first");
    }
  }

  /**
   * 响应 inspect 完成后的可查询快照。
   *
   * @param shared 响应级视图
   * @param seatBids SeatBid 视图列表
   * @param bids 扁平 Bid 列表
   */
  public record Snapshot(
      ResponseInspect.SharedView shared,
      List<ResponseInspect.SeatBidView> seatBids,
      List<ResponseInspect.BidView> bids) {

    public Snapshot {
      Objects.requireNonNull(shared, "shared");
      seatBids = List.copyOf(Objects.requireNonNull(seatBids, "seatBids"));
      bids = List.copyOf(Objects.requireNonNull(bids, "bids"));
    }

    /** 回显的请求 id。 */
    public String requestId() {
      return shared.id();
    }

    /** 响应级 bid id。 */
    public String bidid() {
      return shared.bidid();
    }

    /** 响应货币。 */
    public String currency() {
      return shared.cur();
    }

    /** 是否为 wire no-bid（无 seatbid）。 */
    public boolean noBid() {
      return shared.noBid();
    }

    /** no-bid 原因码。 */
    public int nbr() {
      return shared.nbr();
    }

    /** 原始 BidResponse。 */
    public BidResponse response() {
      return shared.res();
    }

    /**
     * 按 bid id 查找。
     *
     * @param id bid id
     * @return BidView，无则 empty
     */
    public Optional<ResponseInspect.BidView> findBid(String id) {
      if (id == null) {
        return Optional.empty();
      }
      return bids.stream().filter(b -> id.equals(b.id())).findFirst();
    }

    /**
     * 按 impid 筛选出价。
     *
     * @param impid Imp id
     * @return 匹配的 Bid 列表
     */
    public List<ResponseInspect.BidView> bidsForImp(String impid) {
      List<ResponseInspect.BidView> out = new ArrayList<>();
      for (ResponseInspect.BidView b : bids) {
        if (Objects.equals(impid, b.impid())) {
          out.add(b);
        }
      }
      return Collections.unmodifiableList(out);
    }

    /**
     * 筛选包含指定格式位的出价。
     *
     * @param formatFlag {@link MarkupMask} 单 bit 常量
     * @return 匹配的 Bid 列表
     */
    public List<ResponseInspect.BidView> bidsWith(int formatFlag) {
      List<ResponseInspect.BidView> out = new ArrayList<>();
      for (ResponseInspect.BidView b : bids) {
        if (b.markup().has(formatFlag)) {
          out.add(b);
        }
      }
      return Collections.unmodifiableList(out);
    }

    /**
     * 提取各 Bid 的紧凑事实。
     *
     * @return BidFact 列表
     */
    public List<BidFact> facts() {
      List<BidFact> out = new ArrayList<>(bids.size());
      for (ResponseInspect.BidView b : bids) {
        out.add(BidFact.from(b));
      }
      return Collections.unmodifiableList(out);
    }
  }

  /**
   * 单条 Bid 的扁平事实。
   *
   * @param id bid id
   * @param impid Imp id
   * @param seat 席位
   * @param price 价格
   * @param markup 格式掩码
   * @param mtype mtype 数值
   * @param crid 创意 id
   * @param dealid deal id
   * @param w 宽
   * @param h 高
   * @param dur 时长
   * @param hasAdm 是否有 adm
   * @param adomain 广告主域名
   */
  public record BidFact(
      String id,
      String impid,
      String seat,
      double price,
      MarkupMask markup,
      int mtype,
      String crid,
      String dealid,
      int w,
      int h,
      int dur,
      boolean hasAdm,
      List<String> adomain) {

    static BidFact from(ResponseInspect.BidView b) {
      return new BidFact(
          b.id(),
          b.impid(),
          b.seat(),
          b.price(),
          b.markup(),
          b.mtype(),
          b.crid(),
          b.dealid(),
          b.w(),
          b.h(),
          b.dur(),
          b.adm() != null && !b.adm().isBlank(),
          b.adomain());
    }

    /** 是否 Banner 出价。 */
    public boolean hasBanner() {
      return markup.hasBanner();
    }

    /** 是否 Video 出价。 */
    public boolean hasVideo() {
      return markup.hasVideo();
    }

    /** 是否 Audio 出价。 */
    public boolean hasAudio() {
      return markup.hasAudio();
    }

    /** 是否 Native 出价。 */
    public boolean hasNative() {
      return markup.hasNative();
    }
  }
}

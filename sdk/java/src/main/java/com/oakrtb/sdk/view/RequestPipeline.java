package com.oakrtb.sdk.view;

import com.oakrtb.openrtb.v2.BidRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 按固定顺序编排 BidRequest view 流程，并暴露可查询的 {@link Snapshot}。
 *
 * <ol>
 *   <li>{@link #lightGate} — 结构拒绝
 *   <li>{@link #shared} — 竞价级 {@link RequestViews.SharedView}
 *   <li>{@link #imps} — 各 {@link RequestViews.ImpView} 及格式位掩码
 * </ol>
 *
 * <p>一次性：{@link #run(BidRequest)}。分步：{@link #of} 后链式调用 → {@link #snapshot}。
 *
 * <p>非完整拍卖引擎，也非 JSON Schema 校验。
 */
public final class RequestPipeline {
  private final BidRequest req;
  private RequestViews.SharedView shared;
  private List<RequestViews.ImpView> imps = List.of();
  private boolean gated;
  private boolean pinned;
  private boolean viewed;

  private RequestPipeline(BidRequest req) {
    this.req = Objects.requireNonNull(req, "BidRequest");
  }

  /**
   * 对已解码的 BidRequest 开始编排。
   *
   * @param req BidRequest
   * @return 管道实例
   */
  public static RequestPipeline of(BidRequest req) {
    return new RequestPipeline(req);
  }

  /**
   * 按序执行全部步骤并返回可查询快照。
   *
   * @param req BidRequest
   * @return Snapshot
   */
  public static Snapshot run(BidRequest req) {
    return of(req).lightGate().shared().imps().snapshot();
  }

  /**
   * 步骤 1：廉价结构校验。
   *
   * @return 当前管道
   */
  public RequestPipeline lightGate() {
    RequestViews.lightGate(req);
    gated = true;
    return this;
  }

  /**
   * 步骤 2：固定竞价级 SharedView（须先 {@link #lightGate}）。
   *
   * @return 当前管道
   */
  public RequestPipeline shared() {
    requireGated();
    if (pinned) {
      return this;
    }
    shared = RequestViews.shared(req);
    pinned = true;
    return this;
  }

  /**
   * 步骤 3：生成带格式掩码的 ImpView 列表（须先 {@link #shared}）。
   *
   * @return 当前管道
   */
  public RequestPipeline imps() {
    requirePinned();
    imps = RequestViews.imps(req);
    viewed = true;
    return this;
  }

  /**
   * 完成管道；若某步被跳过，将自动按序补跑剩余步骤。
   *
   * @return Snapshot
   */
  public Snapshot snapshot() {
    if (!gated) {
      lightGate();
    }
    if (!pinned) {
      shared();
    }
    if (!viewed) {
      imps();
    }
    return new Snapshot(shared, imps);
  }

  private void requireGated() {
    if (!gated) {
      throw new IllegalStateException("pipeline: call lightGate first");
    }
  }

  private void requirePinned() {
    requireGated();
    if (!pinned) {
      throw new IllegalStateException("pipeline: call shared first");
    }
  }

  /**
   * 管道完成后的可查询输出。
   *
   * <p>含 SharedView / ImpView，并附带常用查询辅助方法。
   *
   * @param shared 竞价级视图
   * @param imps Imp 视图列表
   */
  public record Snapshot(RequestViews.SharedView shared, List<RequestViews.ImpView> imps) {
    public Snapshot {
      Objects.requireNonNull(shared, "shared");
      imps = List.copyOf(Objects.requireNonNull(imps, "imps"));
    }

    /**
     * 竞价 id。
     *
     * @return 请求 id
     */
    public String auctionId() {
      return shared.id();
    }

    /**
     * 拍卖类型（at）。
     *
     * @return at 值
     */
    public int auctionType() {
      return shared.at();
    }

    /**
     * 库存类型（{@link Inventory}，≠ Content.Channel）。
     *
     * @return Inventory 枚举
     */
    public Inventory inventory() {
      return shared.inventory();
    }

    /**
     * 允许的出价货币列表。
     *
     * @return 货币代码列表
     */
    public List<String> currencies() {
      return shared.cur();
    }

    /**
     * 是否已超过建议截止时间。
     *
     * @return 超时为 true
     */
    public boolean pastDeadline() {
      return shared.pastDeadline();
    }

    /**
     * 按 id 查找 Imp 视图。
     *
     * @param id Imp id
     * @return 匹配的 ImpView，无则 empty
     */
    public Optional<RequestViews.ImpView> findImp(String id) {
      if (id == null) {
        return Optional.empty();
      }
      return imps.stream().filter(i -> id.equals(i.id())).findFirst();
    }

    /**
     * 筛选包含指定格式位的 Imp（如 {@link MarkupMask#BANNER}）。
     *
     * @param formatFlag 单 bit 格式常量
     * @return 匹配的 Imp 列表
     */
    public List<RequestViews.ImpView> impsWith(int formatFlag) {
      List<RequestViews.ImpView> out = new ArrayList<>();
      for (RequestViews.ImpView iv : imps) {
        if (iv.markup().has(formatFlag)) {
          out.add(iv);
        }
      }
      return Collections.unmodifiableList(out);
    }

    /**
     * 提取各 Imp 的紧凑事实，供匹配/出价使用。
     *
     * @return ImpFact 列表
     */
    public List<ImpFact> facts() {
      List<ImpFact> out = new ArrayList<>(imps.size());
      for (RequestViews.ImpView iv : imps) {
        out.add(ImpFact.from(iv));
      }
      return Collections.unmodifiableList(out);
    }

    /**
     * 原始 BidRequest 引用。
     *
     * @return BidRequest
     */
    public BidRequest request() {
      return shared.req();
    }
  }

  /**
   * 从单条 ImpView 提取的扁平事实。
   *
   * @param id Imp id
   * @param markup 格式掩码
   * @param mtype 建议 mtype（单格式时有值）
   * @param tagId tagid
   * @param bidFloor 底价
   * @param bidFloorCur 底价货币
   * @param secure secure 标志
   * @param instl instl 标志
   * @param rwdd rwdd 标志
   * @param ssai ssai 标志
   * @param bannerW Banner 宽（若有）
   * @param bannerH Banner 高（若有）
   * @param nativeRequest Native request JSON（若有）
   */
  public record ImpFact(
      String id,
      MarkupMask markup,
      int mtype,
      String tagId,
      double bidFloor,
      String bidFloorCur,
      int secure,
      int instl,
      int rwdd,
      int ssai,
      Integer bannerW,
      Integer bannerH,
      String nativeRequest) {

    static ImpFact from(RequestViews.ImpView iv) {
      Integer w = null;
      Integer h = null;
      if (iv.banner() != null) {
        if (iv.banner().getW() != 0) {
          w = iv.banner().getW();
        }
        if (iv.banner().getH() != 0) {
          h = iv.banner().getH();
        }
      }
      String nativeReq = null;
      if (iv.nativeAd() != null) {
        String r = iv.nativeAd().getRequest();
        if (r != null && !r.isBlank()) {
          nativeReq = r;
        }
      }
      return new ImpFact(
          iv.id(),
          iv.markup(),
          iv.markup().mtype(),
          iv.tagId(),
          iv.bidFloor(),
          iv.bidFloorCur(),
          iv.secure(),
          iv.instl(),
          iv.rwdd(),
          iv.ssai(),
          w,
          h,
          nativeReq);
    }

    /** 是否含 Banner 格式。 */
    public boolean hasBanner() {
      return markup.hasBanner();
    }

    /** 是否含 Video 格式。 */
    public boolean hasVideo() {
      return markup.hasVideo();
    }

    /** 是否含 Audio 格式。 */
    public boolean hasAudio() {
      return markup.hasAudio();
    }

    /** 是否含 Native 格式。 */
    public boolean hasNative() {
      return markup.hasNative();
    }
  }
}

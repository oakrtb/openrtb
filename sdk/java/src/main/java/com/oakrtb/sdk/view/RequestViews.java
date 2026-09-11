package com.oakrtb.sdk.view;

import com.oakrtb.openrtb.v2.App;
import com.oakrtb.openrtb.v2.Audio;
import com.oakrtb.openrtb.v2.Banner;
import com.oakrtb.openrtb.v2.BidRequest;
import com.oakrtb.openrtb.v2.Device;
import com.oakrtb.openrtb.v2.Dooh;
import com.oakrtb.openrtb.v2.Imp;
import com.oakrtb.openrtb.v2.Native;
import com.oakrtb.openrtb.v2.Pmp;
import com.oakrtb.openrtb.v2.Regs;
import com.oakrtb.openrtb.v2.Site;
import com.oakrtb.openrtb.v2.Source;
import com.oakrtb.openrtb.v2.User;
import com.oakrtb.openrtb.v2.Video;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * BidRequest 的轻量 view 步骤，供匹配、出价、响应等业务逻辑使用。
 *
 * <p>不执行完整 JSON Schema 校验——权威校验请使用 {@code Schema}。
 * 有序编排请优先使用 {@link RequestPipeline#run}。
 */
public final class RequestViews {
  private RequestViews() {}

  /**
   * 竞价级零拷贝视图（持有原始 {@link BidRequest} 引用）。
   *
   * @param req 原始请求
   * @param id 请求 id
   * @param at 拍卖类型
   * @param cur 允许货币列表
   * @param tmax 最大响应时间（毫秒）
   * @param test 是否测试流量
   * @param inventory 库存类型（{@link Inventory}，≠ proto Content.Channel）
   * @param deadline 建议截止时间（tmax 的 85%）
   * @param site Site 对象（若有）
   * @param app App 对象（若有）
   * @param dooh Dooh 对象（若有）
   * @param device Device 对象（若有）
   * @param user User 对象（若有）
   * @param regs Regs 对象（若有）
   * @param source Source 对象（若有）
   * @param bcat 屏蔽分类
   * @param badv 屏蔽广告主域名
   * @param bapp 屏蔽应用 bundle
   */
  public record SharedView(
      BidRequest req,
      String id,
      int at,
      List<String> cur,
      int tmax,
      int test,
      Inventory inventory,
      Instant deadline,
      Site site,
      App app,
      Dooh dooh,
      Device device,
      User user,
      Regs regs,
      Source source,
      List<String> bcat,
      List<String> badv,
      List<String> bapp) {

    /**
     * 当前时间是否已超过建议截止时间。
     *
     * @return 超时为 {@code true}；无 deadline 时为 {@code false}
     */
    public boolean pastDeadline() {
      return deadline != null && Instant.now().isAfter(deadline);
    }
  }

  /**
   * 单条 Imp 的 view 视图，含格式位掩码与各格式 spec 的懒访问引用。
   *
   * @param imp 原始 Imp
   * @param id Imp id
   * @param markup 格式位掩码（{@link MarkupMask}，≠ Banner.Format）
   * @param tagId tagid
   * @param bidFloor 底价
   * @param bidFloorCur 底价货币
   * @param instl 是否插屏
   * @param secure 是否要求 HTTPS
   * @param rwdd 是否激励
   * @param ssai SSAI 标志
   * @param banner Banner spec（若有）
   * @param video Video spec（若有）
   * @param audio Audio spec（若有）
   * @param nativeAd Native spec（若有）
   * @param pmp PMP spec（若有）
   */
  public record ImpView(
      Imp imp,
      String id,
      MarkupMask markup,
      String tagId,
      double bidFloor,
      String bidFloorCur,
      int instl,
      int secure,
      int rwdd,
      int ssai,
      Banner banner,
      Video video,
      Audio audio,
      Native nativeAd,
      Pmp pmp) {}


  /**
   * 从 Imp 提取格式位掩码。
   *
   * @param imp Imp 对象，可为 null
   * @return 格式掩码；null imp 返回空掩码
   */
  public static MarkupMask markupMask(Imp imp) {
    if (imp == null) {
      return MarkupMask.of(MarkupMask.NONE);
    }
    int bits = MarkupMask.NONE;
    if (imp.hasBanner()) {
      bits |= MarkupMask.BANNER;
    }
    if (imp.hasVideo()) {
      bits |= MarkupMask.VIDEO;
    }
    if (imp.hasAudio()) {
      bits |= MarkupMask.AUDIO;
    }
    if (imp.hasNative()) {
      bits |= MarkupMask.NATIVE;
    }
    return MarkupMask.of(bits);
  }

  /**
   * 廉价结构校验（非完整 JSON Schema）。
   *
   * @param req BidRequest
   * @throws IllegalArgumentException 结构不合法时
   */
  public static void lightGate(BidRequest req) {
    Objects.requireNonNull(req, "BidRequest");
    if (req.getId().isBlank()) {
      throw new IllegalArgumentException("view: BidRequest.id is required");
    }
    if (req.getAt() == 0) {
      throw new IllegalArgumentException("view: BidRequest.at is required");
    }
    if (req.getCurCount() == 0) {
      throw new IllegalArgumentException("view: BidRequest.cur is required");
    }
    for (int i = 0; i < req.getCurCount(); i++) {
      if (req.getCur(i).isBlank()) {
        throw new IllegalArgumentException("view: BidRequest.cur[" + i + "] is blank");
      }
    }
    if (req.getImpCount() == 0) {
      throw new IllegalArgumentException("view: BidRequest.imp requires at least one Imp");
    }
    int n = 0;
    if (req.hasSite()) {
      n++;
    }
    if (req.hasApp()) {
      n++;
    }
    if (req.hasDooh()) {
      n++;
    }
    if (n > 1) {
      throw new IllegalArgumentException("view: site/app/dooh are mutually exclusive");
    }
    for (int i = 0; i < req.getImpCount(); i++) {
      Imp imp = req.getImp(i);
      if (imp.getId().isBlank()) {
        throw new IllegalArgumentException("view: imp[" + i + "].id is required");
      }
      if (markupMask(imp).bits() == MarkupMask.NONE) {
        throw new IllegalArgumentException(
            "view: imp[" + i + "] needs banner, video, audio, or native");
      }
    }
  }


  /**
   * 在 {@link #lightGate} 之后：固定竞价级 {@link SharedView}。
   *
   * @param req BidRequest
   * @return SharedView
   */
  public static SharedView shared(BidRequest req) {
    Inventory ch = Inventory.NONE;
    if (req.hasSite()) {
      ch = Inventory.SITE;
    } else if (req.hasApp()) {
      ch = Inventory.APP;
    } else if (req.hasDooh()) {
      ch = Inventory.DOOH;
    }
    Instant deadline = null;
    if (req.getTmax() > 0) {
      deadline = Instant.now().plusMillis(req.getTmax() * 85L / 100L);
    }
    return new SharedView(
        req,
        req.getId(),
        req.getAt(),
        List.copyOf(req.getCurList()),
        req.getTmax(),
        req.getTest(),
        ch,
        deadline,
        req.hasSite() ? req.getSite() : null,
        req.hasApp() ? req.getApp() : null,
        req.hasDooh() ? req.getDooh() : null,
        req.hasDevice() ? req.getDevice() : null,
        req.hasUser() ? req.getUser() : null,
        req.hasRegs() ? req.getRegs() : null,
        req.hasSource() ? req.getSource() : null,
        List.copyOf(req.getBcatList()),
        List.copyOf(req.getBadvList()),
        List.copyOf(req.getBappList()));
  }

  /**
   * 在 {@link #shared} 之后：生成带格式掩码的 ImpView 列表。
   *
   * @param req BidRequest
   * @return 不可变 ImpView 列表
   */
  public static List<ImpView> imps(BidRequest req) {
    List<ImpView> out = new ArrayList<>(req.getImpCount());
    for (Imp imp : req.getImpList()) {
      out.add(
          new ImpView(
              imp,
              imp.getId(),
              markupMask(imp),
              imp.getTagid(),
              imp.getBidfloor(),
              imp.getBidfloorcur(),
              imp.getInstl(),
              imp.getSecure(),
              imp.getRwdd(),
              imp.getSsai(),
              imp.hasBanner() ? imp.getBanner() : null,
              imp.hasVideo() ? imp.getVideo() : null,
              imp.hasAudio() ? imp.getAudio() : null,
              imp.hasNative() ? imp.getNative() : null,
              imp.hasPmp() ? imp.getPmp() : null));
    }
    return Collections.unmodifiableList(out);
  }
}

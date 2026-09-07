package com.oakrtb.sdk.fit;

import com.oakrtb.openrtb.v2.Audio;
import com.oakrtb.openrtb.v2.Banner;
import com.oakrtb.openrtb.v2.Bid;
import com.oakrtb.openrtb.v2.BidResponse;
import com.oakrtb.openrtb.v2.Native;
import com.oakrtb.openrtb.v2.SeatBid;
import com.oakrtb.openrtb.v2.Video;
import com.oakrtb.sdk.inspect.MarkupMask;
import com.oakrtb.sdk.inspect.RequestInspect;
import com.oakrtb.sdk.inspect.RequestPipeline;
import com.oakrtb.sdk.inspect.ResponseInspect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * RequestInspect 与 ResponseInspect 之间的可选 bid↔request 一致性检查。
 *
 * <p>非 JSON Schema，也非 LightGate；返回软性 {@link FitResult}（不抛异常）。
 * 调用方自行决定是否因 ERROR 拒绝或处理 WARN。
 */
public final class Fit {
  private Fit() {}

  /**
   * 检查所选 markup 类型是否在 Imp 上存在，且具备 OakRTB 构建器级 spec。
   *
   * @param imp Imp 视图
   * @param mtype OpenRTB mtype（1–4）
   * @return Fit 结果
   */
  public static FitResult impReady(RequestInspect.ImpView imp, int mtype) {
    Objects.requireNonNull(imp, "imp");
    List<FitIssue> issues = new ArrayList<>();
    String path = "imp[" + imp.id() + "]";
    MarkupMask chosen = ResponseInspect.markupFromMtypeValue(mtype);
    if (mtype < 1 || mtype > 4) {
      issues.add(
          new FitIssue(
              IssueCode.MTYPE_UNKNOWN,
              Severity.ERROR,
              path + ".mtype",
              "mtype must be 1–4 for impReady"));
      return FitResult.of(issues);
    }
    return impReadyChosen(imp, chosen, path, issues);
  }

  /**
   * 与 {@link #impReady(RequestInspect.ImpView, int)} 相同，但使用单 bit {@link MarkupMask}。
   *
   * @param imp Imp 视图
   * @param chosen 须恰好一种格式的掩码
   * @return Fit 结果
   */
  public static FitResult impReady(RequestInspect.ImpView imp, MarkupMask chosen) {
    Objects.requireNonNull(imp, "imp");
    Objects.requireNonNull(chosen, "chosen");
    List<FitIssue> issues = new ArrayList<>();
    String path = "imp[" + imp.id() + "]";
    if (chosen.count() != 1) {
      issues.add(
          new FitIssue(
              IssueCode.MULTI_NEEDS_CHOICE,
              Severity.ERROR,
              path,
              "chosen MarkupMask must be exactly one bit"));
      return FitResult.of(issues);
    }
    return impReadyChosen(imp, chosen, path, issues);
  }

  private static FitResult impReadyChosen(
      RequestInspect.ImpView imp, MarkupMask chosen, String path, List<FitIssue> issues) {
    if (!imp.markup().has(chosen.bits())) {
      issues.add(
          new FitIssue(
              IssueCode.FORMAT_NOT_ON_IMP,
              Severity.ERROR,
              path,
              "chosen format is not present on Imp"));
      return FitResult.of(issues);
    }
    if (chosen.hasBanner()) {
      Banner b = imp.banner();
      boolean sizeOk =
          b != null && ((b.getW() > 0 && b.getH() > 0) || b.getFormatCount() > 0);
      if (!sizeOk) {
        issues.add(
            new FitIssue(
                IssueCode.BANNER_SIZE_MISSING,
                Severity.ERROR,
                path + ".banner",
                "banner needs w/h or format[]"));
      }
    }
    if (chosen.hasVideo()) {
      Video v = imp.video();
      if (v == null || v.getMimesCount() == 0) {
        issues.add(
            new FitIssue(
                IssueCode.VIDEO_MIMES_MISSING,
                Severity.ERROR,
                path + ".video.mimes",
                "video.mimes is required"));
      } else {
        if (v.getMinduration() == 0 && v.getMaxduration() == 0) {
          issues.add(
              new FitIssue(
                  IssueCode.VIDEO_DURATION_UNSET,
                  Severity.WARN,
                  path + ".video",
                  "video minduration/maxduration unset"));
        }
        if (v.getProtocolsCount() == 0) {
          issues.add(
              new FitIssue(
                  IssueCode.VIDEO_PROTOCOLS_UNSET,
                  Severity.WARN,
                  path + ".video.protocols",
                  "video.protocols unset"));
        }
      }
    }
    if (chosen.hasAudio()) {
      Audio a = imp.audio();
      if (a == null || a.getMimesCount() == 0) {
        issues.add(
            new FitIssue(
                IssueCode.AUDIO_MIMES_MISSING,
                Severity.ERROR,
                path + ".audio.mimes",
                "audio.mimes is required"));
      }
    }
    if (chosen.hasNative()) {
      Native n = imp.nativeAd();
      if (n == null || n.getRequest().isBlank()) {
        issues.add(
            new FitIssue(
                IssueCode.NATIVE_REQUEST_MISSING,
                Severity.ERROR,
                path + ".native.request",
                "native.request is required"));
      }
    }
    return FitResult.of(issues);
  }

  /**
   * 将单条 Bid 与请求快照做 Fit（不含响应级货币检查）。
   *
   * @param req 请求管道快照
   * @param bid 出价
   * @return Fit 结果
   */
  public static FitResult bidFit(RequestPipeline.Snapshot req, Bid bid) {
    return bidFit(req, bid, null, "bid");
  }

  /**
   * 对整个 BidResponse 做 Fit。空 seatbid（wire no-bid）返回空 ok 结果；
   * 必要时在响应级追加 PAST_DEADLINE / CUR_NOT_ALLOWED。
   *
   * @param req 请求管道快照
   * @param res 出价响应
   * @return Fit 结果
   */
  public static FitResult responseFit(RequestPipeline.Snapshot req, BidResponse res) {
    Objects.requireNonNull(req, "req");
    Objects.requireNonNull(res, "res");
    List<FitIssue> issues = new ArrayList<>();
    if (req.pastDeadline()) {
      issues.add(
          new FitIssue(
              IssueCode.PAST_DEADLINE,
              Severity.WARN,
              "tmax",
              "past request deadline (85% of tmax)"));
    }
    if (res.getSeatbidCount() == 0) {
      return FitResult.of(issues);
    }
    String resCur = res.getCur();
    if (resCur != null && !resCur.isBlank()) {
      boolean allowed = false;
      for (String c : req.currencies()) {
        if (resCur.equalsIgnoreCase(c)) {
          allowed = true;
          break;
        }
      }
      if (!allowed) {
        issues.add(
            new FitIssue(
                IssueCode.CUR_NOT_ALLOWED,
                Severity.WARN,
                "BidResponse.cur",
                "response currency not in BidRequest.cur"));
      }
    }
    for (int i = 0; i < res.getSeatbidCount(); i++) {
      SeatBid sb = res.getSeatbid(i);
      for (int j = 0; j < sb.getBidCount(); j++) {
        String path = "seatbid[" + i + "].bid[" + j + "]";
        FitResult one = bidFit(req, sb.getBid(j), resCur, path);
        issues.addAll(one.issues());
      }
    }
    return FitResult.of(issues);
  }

  private static FitResult bidFit(
      RequestPipeline.Snapshot req, Bid bid, String responseCur, String path) {
    Objects.requireNonNull(req, "req");
    Objects.requireNonNull(bid, "bid");
    List<FitIssue> issues = new ArrayList<>();
    Optional<RequestInspect.ImpView> found = req.findImp(bid.getImpid());
    if (found.isEmpty()) {
      issues.add(
          new FitIssue(
              IssueCode.IMP_NOT_FOUND,
              Severity.ERROR,
              path + ".impid",
              "impid not found in request"));
      return FitResult.of(issues);
    }
    RequestInspect.ImpView imp = found.get();
    int mtype = bid.getMtypeValue();
    MarkupMask markup = imp.markup();

    if (mtype >= 500) {
      issues.add(
          new FitIssue(
              IssueCode.MTYPE_VENDOR,
              Severity.WARN,
              path + ".mtype",
              "vendor mtype >=500"));
    } else if (mtype != 0 && (mtype < 1 || mtype > 4)) {
      issues.add(
          new FitIssue(
              IssueCode.MTYPE_UNKNOWN,
              Severity.ERROR,
              path + ".mtype",
              "mtype must be 0–4 or >=500"));
    } else if (mtype == 0 && markup.count() > 1) {
      issues.add(
          new FitIssue(
              IssueCode.MTYPE_REQUIRED,
              Severity.ERROR,
              path + ".mtype",
              "multi-format Imp requires Bid.mtype"));
    } else if (mtype >= 1 && mtype <= 4) {
      MarkupMask chosen = ResponseInspect.markupFromMtypeValue(mtype);
      if (!markup.has(chosen.bits())) {
        issues.add(
            new FitIssue(
                IssueCode.MTYPE_MISMATCH,
                Severity.ERROR,
                path + ".mtype",
                "mtype does not match Imp formats"));
      }
    }

    // Effective format for blocklist checks
    MarkupMask eff =
        (mtype >= 1 && mtype <= 4)
            ? ResponseInspect.markupFromMtypeValue(mtype)
            : (markup.count() == 1 ? markup : MarkupMask.of(MarkupMask.NONE));

    checkFloor(issues, imp, bid, responseCur, path);
    checkAttr(issues, imp, bid, eff, path);
    checkBlocks(issues, req, bid, path);
    return FitResult.of(issues);
  }

  private static void checkFloor(
      List<FitIssue> issues,
      RequestInspect.ImpView imp,
      Bid bid,
      String responseCur,
      String path) {
    double floor = imp.bidFloor();
    if (floor <= 0) {
      return;
    }
    String floorCur = imp.bidFloorCur();
    if (responseCur != null
        && !responseCur.isBlank()
        && floorCur != null
        && !floorCur.isBlank()
        && !responseCur.equalsIgnoreCase(floorCur)) {
      issues.add(
          new FitIssue(
              IssueCode.FLOOR_CUR_DIFF,
              Severity.WARN,
              path + ".price",
              "bid currency differs from imp.bidfloorcur; skip floor compare"));
      return;
    }
    if (bid.getPrice() < floor) {
      issues.add(
          new FitIssue(
              IssueCode.PRICE_BELOW_FLOOR,
              Severity.WARN,
              path + ".price",
              "price below imp.bidfloor"));
    }
  }

  private static void checkAttr(
      List<FitIssue> issues,
      RequestInspect.ImpView imp,
      Bid bid,
      MarkupMask eff,
      String path) {
    if (bid.getAttrCount() == 0 || eff.bits() == MarkupMask.NONE) {
      return;
    }
    List<Integer> battr = battrFor(imp, eff);
    if (battr.isEmpty()) {
      return;
    }
    Set<Integer> blocked = new HashSet<>(battr);
    for (int a : bid.getAttrList()) {
      if (blocked.contains(a)) {
        issues.add(
            new FitIssue(
                IssueCode.ATTR_BLOCKED,
                Severity.WARN,
                path + ".attr",
                "bid.attr intersects format battr"));
        return;
      }
    }
  }

  private static List<Integer> battrFor(RequestInspect.ImpView imp, MarkupMask eff) {
    if (eff.hasBanner() && imp.banner() != null) {
      return imp.banner().getBattrList();
    }
    if (eff.hasVideo() && imp.video() != null) {
      return imp.video().getBattrList();
    }
    if (eff.hasAudio() && imp.audio() != null) {
      return imp.audio().getBattrList();
    }
    if (eff.hasNative() && imp.nativeAd() != null) {
      return imp.nativeAd().getBattrList();
    }
    return List.of();
  }

  private static void checkBlocks(
      List<FitIssue> issues, RequestPipeline.Snapshot req, Bid bid, String path) {
    List<String> badv = req.shared().badv();
    if (!badv.isEmpty()) {
      Set<String> block = toLowerSet(badv);
      for (String d : bid.getAdomainList()) {
        if (d != null && block.contains(d.toLowerCase(Locale.ROOT))) {
          issues.add(
              new FitIssue(
                  IssueCode.ADOMAIN_BLOCKED,
                  Severity.WARN,
                  path + ".adomain",
                  "adomain hit BidRequest.badv"));
          break;
        }
      }
    }
    List<String> bapp = req.shared().bapp();
    if (!bapp.isEmpty() && bid.getBundle() != null && !bid.getBundle().isBlank()) {
      Set<String> block = toLowerSet(bapp);
      if (block.contains(bid.getBundle().toLowerCase(Locale.ROOT))) {
        issues.add(
            new FitIssue(
                IssueCode.BUNDLE_BLOCKED,
                Severity.WARN,
                path + ".bundle",
                "bundle hit BidRequest.bapp"));
      }
    }
    List<String> bcat = req.shared().bcat();
    if (!bcat.isEmpty()) {
      Set<String> block = toLowerSet(bcat);
      for (String c : bid.getCatList()) {
        if (c != null && block.contains(c.toLowerCase(Locale.ROOT))) {
          issues.add(
              new FitIssue(
                  IssueCode.CAT_BLOCKED,
                  Severity.WARN,
                  path + ".cat",
                  "cat hit BidRequest.bcat"));
          break;
        }
      }
    }
  }

  private static Set<String> toLowerSet(List<String> in) {
    Set<String> out = new HashSet<>();
    for (String s : in) {
      if (s != null) {
        out.add(s.toLowerCase(Locale.ROOT));
      }
    }
    return out;
  }
}

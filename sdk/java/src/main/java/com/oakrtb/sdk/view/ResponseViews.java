package com.oakrtb.sdk.view;

import com.oakrtb.openrtb.v2.Bid;
import com.oakrtb.openrtb.v2.BidResponse;
import com.oakrtb.openrtb.v2.MarkupType;
import com.oakrtb.openrtb.v2.SeatBid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * BidResponse 的轻量 view 步骤，供胜出/失败处理、日志等使用。
 *
 * <p>线上空 {@code seatbid} 视为 no-bid；构建器侧仍要求显式调用 {@code noBid(nbr)}。
 *
 * <p>不执行完整 JSON Schema——权威校验请使用 {@code Schema}。
 * 有序编排请优先使用 {@link ResponsePipeline#run}。
 */
public final class ResponseViews {
  private ResponseViews() {}

  /**
   * 响应级共享视图。
   *
   * @param res 原始 BidResponse
   * @param id 回显的请求 id
   * @param bidid 响应级 bid id
   * @param cur 货币
   * @param nbr no-bid 原因码
   * @param customdata 自定义数据
   * @param noBid 是否无 seatbid（wire no-bid）
   */
  public record SharedView(
      BidResponse res,
      String id,
      String bidid,
      String cur,
      int nbr,
      String customdata,
      boolean noBid) {}

  /**
   * 单个 SeatBid 的视图。
   *
   * @param seatBid 原始 SeatBid
   * @param seat 席位标识
   * @param group group 标志
   * @param bids 该席位下的 BidView 列表
   */
  public record SeatBidView(SeatBid seatBid, String seat, int group, List<BidView> bids) {}

  /**
   * 单条 Bid 的 flatten 视图。
   *
   * @param bid 原始 Bid
   * @param id bid id
   * @param impid 对应 Imp id
   * @param seat 所属席位
   * @param price CPM 价格
   * @param markup 格式掩码（{@link MarkupMask}）
   * @param mtype proto mtype 数值
   * @param crid 创意 id
   * @param cid 活动 id
   * @param dealid deal id
   * @param w 宽度
   * @param h 高度
   * @param dur 时长（秒）
   * @param adm markup
   * @param nurl win notice
   * @param burl billing notice
   * @param lurl loss notice
   * @param adomain 广告主域名
   */
  public record BidView(
      Bid bid,
      String id,
      String impid,
      String seat,
      double price,
      MarkupMask markup,
      int mtype,
      String crid,
      String cid,
      String dealid,
      int w,
      int h,
      int dur,
      String adm,
      String nurl,
      String burl,
      String lurl,
      List<String> adomain) {}


  /**
   * 从 proto {@link MarkupType} 转为 {@link MarkupMask}。
   *
   * @param m MarkupType 枚举，可为 null
   * @return 对应格式掩码
   */
  public static MarkupMask markupFromMtype(MarkupType m) {
    if (m == null) {
      return MarkupMask.of(MarkupMask.NONE);
    }
    return switch (m) {
      case MARKUP_TYPE_BANNER -> MarkupMask.of(MarkupMask.BANNER);
      case MARKUP_TYPE_VIDEO -> MarkupMask.of(MarkupMask.VIDEO);
      case MARKUP_TYPE_AUDIO -> MarkupMask.of(MarkupMask.AUDIO);
      case MARKUP_TYPE_NATIVE -> MarkupMask.of(MarkupMask.NATIVE);
      default -> MarkupMask.of(MarkupMask.NONE);
    };
  }

  /**
   * 从 mtype 整数值转为 {@link MarkupMask}。
   *
   * @param mtype OpenRTB mtype（1–4）
   * @return 对应格式掩码；未知值返回空掩码
   */
  public static MarkupMask markupFromMtypeValue(int mtype) {
    return switch (mtype) {
      case 1 -> MarkupMask.of(MarkupMask.BANNER);
      case 2 -> MarkupMask.of(MarkupMask.VIDEO);
      case 3 -> MarkupMask.of(MarkupMask.AUDIO);
      case 4 -> MarkupMask.of(MarkupMask.NATIVE);
      default -> MarkupMask.of(MarkupMask.NONE);
    };
  }

  /**
   * 廉价结构校验（非完整 JSON Schema）。空 seatbid 视为合法 no-bid。
   *
   * @param res BidResponse
   * @throws IllegalArgumentException 结构不合法时
   */
  public static void lightGate(BidResponse res) {
    Objects.requireNonNull(res, "BidResponse");
    if (res.getId().isBlank()) {
      throw new IllegalArgumentException("view: BidResponse.id is required");
    }
    if (res.getCur().isBlank()) {
      throw new IllegalArgumentException("view: BidResponse.cur is required");
    }
    if (res.getSeatbidCount() == 0) {
      return;
    }
    for (int i = 0; i < res.getSeatbidCount(); i++) {
      SeatBid sb = res.getSeatbid(i);
      if (sb.getBidCount() == 0) {
        throw new IllegalArgumentException("view: seatbid[" + i + "] needs at least one bid");
      }
      for (int j = 0; j < sb.getBidCount(); j++) {
        Bid bid = sb.getBid(j);
        if (bid.getId().isBlank() || bid.getImpid().isBlank()) {
          throw new IllegalArgumentException(
              "view: seatbid[" + i + "].bid[" + j + "] requires id and impid");
        }
        if (bid.getPrice() <= 0) {
          throw new IllegalArgumentException(
              "view: seatbid[" + i + "].bid[" + j + "].price must be > 0");
        }
      }
    }
  }



  /** SeatBid 与扁平 Bid 列表（供 Pipeline 使用）。 */
  record BidLists(List<SeatBidView> seatBids, List<BidView> bids) {}

  static BidLists bidLists(BidResponse res) {
    Views views = viewSeatBids(res);
    return new BidLists(views.seatBids(), views.bids());
  }

  /**
   * 固定响应级 {@link SharedView}。
   *
   * @param res BidResponse
   * @return SharedView
   */
  public static SharedView shared(BidResponse res) {
    return new SharedView(
        res,
        res.getId(),
        res.getBidid(),
        res.getCur(),
        res.getNbr(),
        res.getCustomdata(),
        res.getSeatbidCount() == 0);
  }



  private record Views(List<SeatBidView> seatBids, List<BidView> bids) {}

  private static Views viewSeatBids(BidResponse res) {
    List<SeatBidView> seats = new ArrayList<>(res.getSeatbidCount());
    List<BidView> flat = new ArrayList<>();
    for (SeatBid sb : res.getSeatbidList()) {
      List<BidView> bids = new ArrayList<>(sb.getBidCount());
      for (Bid bid : sb.getBidList()) {
        BidView bv = viewBid(bid, sb.getSeat());
        bids.add(bv);
        flat.add(bv);
      }
      seats.add(new SeatBidView(sb, sb.getSeat(), sb.getGroup(), List.copyOf(bids)));
    }
    return new Views(Collections.unmodifiableList(seats), Collections.unmodifiableList(flat));
  }

  private static BidView viewBid(Bid bid, String seat) {
    MarkupType mt = bid.getMtype();
    MarkupMask markup = markupFromMtype(mt);
    return new BidView(
        bid,
        bid.getId(),
        bid.getImpid(),
        seat,
        bid.getPrice(),
        markup,
        bid.getMtypeValue(),
        bid.getCrid(),
        bid.getCid(),
        bid.getDealid(),
        bid.getW(),
        bid.getH(),
        bid.getDur(),
        bid.getAdm(),
        bid.getNurl(),
        bid.getBurl(),
        bid.getLurl(),
        List.copyOf(bid.getAdomainList()));
  }
}

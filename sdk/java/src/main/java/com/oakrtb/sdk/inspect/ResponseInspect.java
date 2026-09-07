package com.oakrtb.sdk.inspect;

import com.oakrtb.openrtb.v2.Bid;
import com.oakrtb.openrtb.v2.BidResponse;
import com.oakrtb.openrtb.v2.MarkupType;
import com.oakrtb.openrtb.v2.SeatBid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * BidResponse 的轻量 inspect 步骤，供胜出/失败处理、日志等使用。
 *
 * <p>线上空 {@code seatbid} 视为 no-bid；构建器侧仍要求显式调用 {@code noBid(nbr)}。
 *
 * <p>不执行完整 JSON Schema——权威校验请使用 {@code Validator}。
 * 有序编排请优先使用 {@link ResponsePipeline#run}。
 */
public final class ResponseInspect {
  private ResponseInspect() {}

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
   * {@link #inspect} 的完整结果。
   *
   * @param shared 响应级视图
   * @param seatBids 各 SeatBid 视图
   * @param bids 扁平化 Bid 列表
   */
  public record Result(SharedView shared, List<SeatBidView> seatBids, List<BidView> bids) {}

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
      throw new IllegalArgumentException("inspect: BidResponse.id is required");
    }
    if (res.getCur().isBlank()) {
      throw new IllegalArgumentException("inspect: BidResponse.cur is required");
    }
    if (res.getSeatbidCount() == 0) {
      return;
    }
    for (int i = 0; i < res.getSeatbidCount(); i++) {
      SeatBid sb = res.getSeatbid(i);
      if (sb.getBidCount() == 0) {
        throw new IllegalArgumentException("inspect: seatbid[" + i + "] needs at least one bid");
      }
      for (int j = 0; j < sb.getBidCount(); j++) {
        Bid bid = sb.getBid(j);
        if (bid.getId().isBlank() || bid.getImpid().isBlank()) {
          throw new IllegalArgumentException(
              "inspect: seatbid[" + i + "].bid[" + j + "] requires id and impid");
        }
        if (bid.getPrice() <= 0) {
          throw new IllegalArgumentException(
              "inspect: seatbid[" + i + "].bid[" + j + "].price must be > 0");
        }
      }
    }
  }

  /**
   * 执行 lightGate 后返回完整 inspect 结果。
   *
   * @param res BidResponse
   * @return Result
   */
  public static Result inspect(BidResponse res) {
    lightGate(res);
    Views views = viewSeatBids(res);
    return new Result(pinShared(res), views.seatBids(), views.bids());
  }

  /**
   * 假定已通过 {@link #lightGate}：固定 shared 并生成 bid 视图（不重复 gate）。
   *
   * @param res BidResponse
   * @return Result
   */
  public static Result viewsAfterGate(BidResponse res) {
    Views views = viewSeatBids(res);
    return new Result(pinShared(res), views.seatBids(), views.bids());
  }

  /**
   * 仅生成 SeatBid/Bid 列表（不重新 pin shared）。
   *
   * @param res BidResponse
   * @return BidLists
   */
  public static BidLists viewBidLists(BidResponse res) {
    Views views = viewSeatBids(res);
    return new BidLists(views.seatBids(), views.bids());
  }

  /**
   * SeatBid 与扁平 Bid 列表的配对。
   *
   * @param seatBids SeatBid 视图列表
   * @param bids 扁平 Bid 列表
   */
  public record BidLists(List<SeatBidView> seatBids, List<BidView> bids) {}

  /**
   * 固定响应级 {@link SharedView}。
   *
   * @param res BidResponse
   * @return SharedView
   */
  public static SharedView pinShared(BidResponse res) {
    return new SharedView(
        res,
        res.getId(),
        res.getBidid(),
        res.getCur(),
        res.getNbr(),
        res.getCustomdata(),
        res.getSeatbidCount() == 0);
  }

  /**
   * 仅返回 SeatBidView 列表。
   *
   * @param res BidResponse
   * @return SeatBid 视图列表
   */
  public static List<SeatBidView> viewSeatBidsOnly(BidResponse res) {
    return viewSeatBids(res).seatBids();
  }

  /**
   * 仅返回扁平 BidView 列表。
   *
   * @param res BidResponse
   * @return Bid 视图列表
   */
  public static List<BidView> viewBids(BidResponse res) {
    return viewSeatBids(res).bids();
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
        mt.getNumber(),
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

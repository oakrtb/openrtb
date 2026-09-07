package com.oakrtb.sdk.build;

import com.oakrtb.openrtb.v2.Bid;
import com.oakrtb.openrtb.v2.BidResponse;
import com.oakrtb.openrtb.v2.MarkupType;
import com.oakrtb.openrtb.v2.SeatBid;
import com.oakrtb.sdk.validate.ValidationResult;

import java.util.Objects;

/** Fluent builder for OpenRTB BidResponse (bids or structured no-bid). */
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

  public static BidResponseBuilder create(String requestId) {
    return new BidResponseBuilder(requestId);
  }

  public BidResponseBuilder bidId(String bidid) {
    res.setBidid(bidid);
    return this;
  }

  public BidResponseBuilder currency(String cur) {
    res.setCur(cur);
    return this;
  }

  public BidResponseBuilder noBid(int nbr) {
    res.setNbr(nbr);
    res.clearSeatbid();
    noBidSet = true;
    return this;
  }

  public BidResponseBuilder addSeatBid(String seat, Bid... bids) {
    if (bids == null || bids.length == 0) {
      throw new IllegalArgumentException("SeatBid requires at least one Bid");
    }
    SeatBid.Builder sb = SeatBid.newBuilder().setSeat(seat);
    for (Bid bid : bids) {
      sb.addBid(Objects.requireNonNull(bid));
    }
    res.addSeatbid(sb);
    return this;
  }

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

  public byte[] buildJson() {
    return Json.toJsonBytes(build());
  }

  public BidRequestBuilder.ValidatedPayload buildValidated() {
    byte[] json = buildJson();
    ValidationResult result = com.oakrtb.sdk.validate.Validator.validateBidResponse(json);
    return new BidRequestBuilder.ValidatedPayload(json, result);
  }

  /** Factory for a single Bid. */
  public static BidBuilder bid(String id, String impid, double price) {
    return new BidBuilder(id, impid, price);
  }

  public static final class BidBuilder {
    private final Bid.Builder b = Bid.newBuilder();

    BidBuilder(String id, String impid, double price) {
      b.setId(id).setImpid(impid).setPrice(price);
    }

    public BidBuilder adm(String adm) {
      b.setAdm(adm);
      return this;
    }

    public BidBuilder nurl(String u) {
      b.setNurl(u);
      return this;
    }

    public BidBuilder burl(String u) {
      b.setBurl(u);
      return this;
    }

    public BidBuilder crid(String crid) {
      b.setCrid(crid);
      return this;
    }

    public BidBuilder cid(String cid) {
      b.setCid(cid);
      return this;
    }

    public BidBuilder adomain(String... domains) {
      b.clearAdomain();
      for (String d : domains) {
        b.addAdomain(d);
      }
      return this;
    }

    public BidBuilder size(int w, int h) {
      b.setW(w).setH(h);
      return this;
    }

    public BidBuilder dealId(String id) {
      b.setDealid(id);
      return this;
    }

    public BidBuilder markupType(MarkupType m) {
      b.setMtype(m);
      return this;
    }

    public BidBuilder banner() {
      return markupType(MarkupType.MARKUP_TYPE_BANNER);
    }

    public BidBuilder video() {
      return markupType(MarkupType.MARKUP_TYPE_VIDEO);
    }

    public BidBuilder audio() {
      return markupType(MarkupType.MARKUP_TYPE_AUDIO);
    }

    public BidBuilder nativeAd() {
      return markupType(MarkupType.MARKUP_TYPE_NATIVE);
    }

    public BidBuilder dur(int seconds) {
      b.setDur(seconds);
      return this;
    }

    public Bid build() {
      return b.build();
    }
  }
}

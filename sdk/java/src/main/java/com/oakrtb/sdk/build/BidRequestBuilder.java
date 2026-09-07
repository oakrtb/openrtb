package com.oakrtb.sdk.build;

import com.oakrtb.openrtb.v2.App;
import com.oakrtb.openrtb.v2.BidRequest;
import com.oakrtb.openrtb.v2.Device;
import com.oakrtb.openrtb.v2.Dooh;
import com.oakrtb.openrtb.v2.Imp;
import com.oakrtb.openrtb.v2.Regs;
import com.oakrtb.openrtb.v2.Site;
import com.oakrtb.openrtb.v2.Source;
import com.oakrtb.openrtb.v2.User;
import com.oakrtb.sdk.validate.ValidationResult;

import java.util.Objects;

/** Fluent builder for OpenRTB BidRequest with format-aware Imp checks. */
public final class BidRequestBuilder {
  private final BidRequest.Builder req = BidRequest.newBuilder();
  private String error;

  private BidRequestBuilder(String id) {
    if (id == null || id.isBlank()) {
      error = "BidRequest.id is required";
    } else {
      req.setId(id);
    }
  }

  public static BidRequestBuilder create(String id) {
    return new BidRequestBuilder(id);
  }

  public BidRequestBuilder firstPrice() {
    return auctionType(1);
  }

  public BidRequestBuilder secondPricePlus() {
    return auctionType(2);
  }

  public BidRequestBuilder auctionType(int at) {
    req.setAt(at);
    return this;
  }

  public BidRequestBuilder tmax(int ms) {
    req.setTmax(ms);
    return this;
  }

  public BidRequestBuilder currency(String... codes) {
    req.clearCur();
    for (String c : codes) {
      req.addCur(c);
    }
    return this;
  }

  public BidRequestBuilder test() {
    req.setTest(1);
    return this;
  }

  public BidRequestBuilder bcat(String... cats) {
    req.clearBcat();
    for (String c : cats) {
      req.addBcat(c);
    }
    return this;
  }

  public BidRequestBuilder badv(String... domains) {
    req.clearBadv();
    for (String d : domains) {
      req.addBadv(d);
    }
    return this;
  }

  public BidRequestBuilder site(Site site) {
    req.clearApp().clearDooh().setSite(Objects.requireNonNull(site));
    return this;
  }

  public BidRequestBuilder app(App app) {
    req.clearSite().clearDooh().setApp(Objects.requireNonNull(app));
    return this;
  }

  public BidRequestBuilder dooh(Dooh dooh) {
    req.clearSite().clearApp().setDooh(Objects.requireNonNull(dooh));
    return this;
  }

  public BidRequestBuilder device(Device device) {
    req.setDevice(Objects.requireNonNull(device));
    return this;
  }

  public BidRequestBuilder user(User user) {
    req.setUser(Objects.requireNonNull(user));
    return this;
  }

  public BidRequestBuilder regs(Regs regs) {
    req.setRegs(Objects.requireNonNull(regs));
    return this;
  }

  public BidRequestBuilder source(Source source) {
    req.setSource(Objects.requireNonNull(source));
    return this;
  }

  public BidRequestBuilder addImp(Imp imp) {
    req.addImp(Objects.requireNonNull(imp));
    return this;
  }

  public BidRequest build() {
    if (error != null) {
      throw new IllegalStateException(error);
    }
    if (req.getAt() == 0) {
      throw new IllegalStateException(
          "BidRequest.at is required (use firstPrice/secondPricePlus/auctionType)");
    }
    if (req.getCurCount() == 0) {
      throw new IllegalStateException(
          "BidRequest.cur is required (at least one ISO-4217 code)");
    }
    if (req.getImpCount() == 0) {
      throw new IllegalStateException("BidRequest.imp requires at least one Imp");
    }
    for (int i = 0; i < req.getImpCount(); i++) {
      checkImp(req.getImp(i), i);
    }
    return req.build();
  }

  public byte[] buildJson() {
    return Json.toJsonBytes(build());
  }

  /** Build + JSON + schema validation. */
  public ValidatedPayload buildValidated() {
    byte[] json = buildJson();
    return new ValidatedPayload(json, ValidatorBridge.request(json));
  }

  private static void checkImp(Imp imp, int i) {
    if (imp.getId().isEmpty()) {
      throw new IllegalStateException("imp[" + i + "].id is required");
    }
    int formats = 0;
    if (imp.hasBanner()) {
      formats++;
      var b = imp.getBanner();
      if (b.getW() == 0 && b.getH() == 0 && b.getFormatCount() == 0) {
        throw new IllegalStateException("imp[" + i + "].banner needs w/h or format[]");
      }
    }
    if (imp.hasVideo()) {
      formats++;
      if (imp.getVideo().getMimesCount() == 0) {
        throw new IllegalStateException("imp[" + i + "].video.mimes is required");
      }
    }
    if (imp.hasAudio()) {
      formats++;
      if (imp.getAudio().getMimesCount() == 0) {
        throw new IllegalStateException("imp[" + i + "].audio.mimes is required");
      }
    }
    if (imp.hasNative()) {
      formats++;
      if (imp.getNative().getRequest().isEmpty()) {
        throw new IllegalStateException("imp[" + i + "].native.request is required");
      }
    }
    if (formats == 0) {
      throw new IllegalStateException("imp[" + i + "] needs banner, video, audio, or native");
    }
  }

  /** Result of {@link #buildValidated()}. */
  public record ValidatedPayload(byte[] json, ValidationResult result) {
    public boolean ok() {
      return result.isOk();
    }
  }

  /** Package-private bridge to avoid circular import naming in docs. */
  static final class ValidatorBridge {
    private ValidatorBridge() {}

    static ValidationResult request(byte[] json) {
      return com.oakrtb.sdk.validate.Validator.validateBidRequest(json);
    }
  }
}

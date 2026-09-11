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
import com.oakrtb.sdk.schema.Report;
import com.oakrtb.sdk.schema.Schema;

import java.util.Objects;

/**
 * OpenRTB {@link BidRequest} 的流式构建器。
 *
 * <p>适用于 SSP/Exchange 侧组装竞价请求；{@link #build()} 会校验 at、cur、imp 及各 Imp 的格式字段。
 */
public final class BidRequestBuilder {
  private final BidRequest.Builder req = BidRequest.newBuilder();
  private String error;

  private BidRequestBuilder(String id) {
    if (id == null || id.isBlank()) {
      error = "build: BidRequest.id is required";
    } else {
      req.setId(id);
    }
  }

  /**
   * 创建请求构建器。
   *
   * @param id 竞价请求唯一 id（必填，非空）
   * @return 新的构建器实例
   */
  public static BidRequestBuilder create(String id) {
    return new BidRequestBuilder(id);
  }

  /**
   * 设置为第一价格拍卖（at=1）。
   *
   * @return 当前构建器
   */
  public BidRequestBuilder firstPrice() {
    return auctionType(1);
  }

  /**
   * 设置为第二价格或更高机制（at=2）。
   *
   * @return 当前构建器
   */
  public BidRequestBuilder secondPricePlus() {
    return auctionType(2);
  }

  /**
   * 设置拍卖类型（at）。
   *
   * @param at OpenRTB 拍卖类型枚举值
   * @return 当前构建器
   */
  public BidRequestBuilder auctionType(int at) {
    req.setAt(at);
    return this;
  }

  /**
   * 设置最大响应时间（毫秒）。
   *
   * @param ms tmax 毫秒数
   * @return 当前构建器
   */
  public BidRequestBuilder tmax(int ms) {
    req.setTmax(ms);
    return this;
  }

  /**
   * 设置允许的出价货币列表（ISO-4217，至少一种）。
   *
   * @param codes 一个或多个货币代码
   * @return 当前构建器
   */
  public BidRequestBuilder currency(String... codes) {
    req.clearCur();
    for (String c : codes) {
      req.addCur(c);
    }
    return this;
  }

  /**
   * 标记为测试请求（test=1）。
   *
   * @return 当前构建器
   */
  public BidRequestBuilder test() {
    req.setTest(1);
    return this;
  }

  /**
   * 设置 blocked advertiser categories（bcat）。
   *
   * @param cats IAB 等内容分类
   * @return 当前构建器
   */
  public BidRequestBuilder bcat(String... cats) {
    req.clearBcat();
    for (String c : cats) {
      req.addBcat(c);
    }
    return this;
  }

  /**
   * 设置 blocked advertiser domains（badv）。
   *
   * @param domains 域名列表
   * @return 当前构建器
   */
  public BidRequestBuilder badv(String... domains) {
    req.clearBadv();
    for (String d : domains) {
      req.addBadv(d);
    }
    return this;
  }

  /**
   * 设置 Web 站点库存（site），并清除 app/dooh。
   *
   * @param site Site 对象
   * @return 当前构建器
   */
  public BidRequestBuilder site(Site site) {
    req.clearApp().clearDooh().setSite(Objects.requireNonNull(site));
    return this;
  }

  /**
   * 设置移动应用库存（app），并清除 site/dooh。
   *
   * @param app App 对象
   * @return 当前构建器
   */
  public BidRequestBuilder app(App app) {
    req.clearSite().clearDooh().setApp(Objects.requireNonNull(app));
    return this;
  }

  /**
   * 设置 DOOH 库存（dooh），并清除 site/app。
   *
   * @param dooh Dooh 对象
   * @return 当前构建器
   */
  public BidRequestBuilder dooh(Dooh dooh) {
    req.clearSite().clearApp().setDooh(Objects.requireNonNull(dooh));
    return this;
  }

  /**
   * 设置设备信息（device）。
   *
   * @param device Device 对象
   * @return 当前构建器
   */
  public BidRequestBuilder device(Device device) {
    req.setDevice(Objects.requireNonNull(device));
    return this;
  }

  /**
   * 设置用户信息（user）。
   *
   * @param user User 对象
   * @return 当前构建器
   */
  public BidRequestBuilder user(User user) {
    req.setUser(Objects.requireNonNull(user));
    return this;
  }

  /**
   * 设置法规/隐私信息（regs）。
   *
   * @param regs Regs 对象
   * @return 当前构建器
   */
  public BidRequestBuilder regs(Regs regs) {
    req.setRegs(Objects.requireNonNull(regs));
    return this;
  }

  /**
   * 设置请求来源（source）。
   *
   * @param source Source 对象
   * @return 当前构建器
   */
  public BidRequestBuilder source(Source source) {
    req.setSource(Objects.requireNonNull(source));
    return this;
  }

  /**
   * 追加一条展示位（Imp）。
   *
   * @param imp Imp 对象（通常由 {@link ImpBuilders} 构建）
   * @return 当前构建器
   */
  public BidRequestBuilder addImp(Imp imp) {
    req.addImp(Objects.requireNonNull(imp));
    return this;
  }

  /**
   * 构建 protobuf {@link BidRequest}，并校验 id、at、cur、imp 及格式字段。
   *
   * @return 构建完成的请求
   * @throws IllegalStateException 必填或格式校验失败时
   */
  public BidRequest build() {
    if (error != null) {
      throw new IllegalStateException(error);
    }
    if (req.getAt() == 0) {
      throw new IllegalStateException("build: BidRequest.at is required");
    }
    if (req.getCurCount() == 0) {
      throw new IllegalStateException(
          "build: BidRequest.cur is required (at least one ISO-4217 code)");
    }
    for (int i = 0; i < req.getCurCount(); i++) {
      if (req.getCur(i).isBlank()) {
        throw new IllegalStateException("build: BidRequest.cur[" + i + "] is blank");
      }
    }
    if (req.getImpCount() == 0) {
      throw new IllegalStateException("build: BidRequest.imp requires at least one Imp");
    }
    int inv = 0;
    if (req.hasSite()) {
      inv++;
    }
    if (req.hasApp()) {
      inv++;
    }
    if (req.hasDooh()) {
      inv++;
    }
    if (inv > 1) {
      throw new IllegalStateException("build: site/app/dooh are mutually exclusive");
    }
    for (int i = 0; i < req.getImpCount(); i++) {
      checkImp(req.getImp(i), i);
    }
    return req.build();
  }

  /**
   * 构建请求并序列化为 OpenRTB JSON 字节数组。
   *
   * @return UTF-8 JSON 字节
   */
  public byte[] buildJson() {
    return Json.toJsonBytes(build());
  }

  /**
   * 构建 JSON 并执行 JSON Schema 校验。
   *
   * @return 含 JSON 与校验结果的封装
   */
  public ValidatedPayload buildValidated() {
    byte[] json = buildJson();
    return new ValidatedPayload(json, Schema.request(json));
  }

  private static void checkImp(Imp imp, int i) {
    if (imp.getId().isBlank()) {
      throw new IllegalStateException("build: imp[" + i + "].id is required");
    }
    int formats = 0;
    if (imp.hasBanner()) {
      formats++;
      var b = imp.getBanner();
      if (b.getW() == 0 && b.getH() == 0 && b.getFormatCount() == 0) {
        throw new IllegalStateException("build: imp[" + i + "].banner needs w/h or format[]");
      }
    }
    if (imp.hasVideo()) {
      formats++;
      if (imp.getVideo().getMimesCount() == 0) {
        throw new IllegalStateException("build: imp[" + i + "].video.mimes is required");
      }
    }
    if (imp.hasAudio()) {
      formats++;
      if (imp.getAudio().getMimesCount() == 0) {
        throw new IllegalStateException("build: imp[" + i + "].audio.mimes is required");
      }
    }
    if (imp.hasNative()) {
      formats++;
      if (imp.getNative().getRequest().isBlank()) {
        throw new IllegalStateException("build: imp[" + i + "].native.request is required");
      }
    }
    if (formats == 0) {
      throw new IllegalStateException("build: imp[" + i + "] needs banner, video, audio, or native");
    }
  }
}

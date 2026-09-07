package com.oakrtb.sdk.build;

import com.oakrtb.openrtb.v2.App;
import com.oakrtb.openrtb.v2.Content;
import com.oakrtb.openrtb.v2.Device;
import com.oakrtb.openrtb.v2.Dooh;
import com.oakrtb.openrtb.v2.Geo;
import com.oakrtb.openrtb.v2.Publisher;
import com.oakrtb.openrtb.v2.Site;

/**
 * Site、App、Dooh、Device 等 OpenRTB 子对象的轻量构建器。
 *
 * <p>用于配合 {@link BidRequestBuilder} 组装库存、设备、发布商等字段。
 */
public final class Parts {
  private Parts() {}

  /** 创建 Site 构建器。 */
  public static SiteBuilder site() {
    return new SiteBuilder();
  }

  /** 创建 App 构建器。 */
  public static AppBuilder app() {
    return new AppBuilder();
  }

  /** 创建 Dooh 构建器。 */
  public static DoohBuilder dooh() {
    return new DoohBuilder();
  }

  /** 创建 Device 构建器。 */
  public static DeviceBuilder device() {
    return new DeviceBuilder();
  }

  /** 创建 Publisher 构建器。 */
  public static PublisherBuilder publisher() {
    return new PublisherBuilder();
  }

  /** 创建 Content 构建器。 */
  public static ContentBuilder content() {
    return new ContentBuilder();
  }

  /** 创建 Geo 构建器。 */
  public static GeoBuilder geo() {
    return new GeoBuilder();
  }

  /** Web 站点（Site）流式构建器。 */
  public static final class SiteBuilder {
    private final Site.Builder b = Site.newBuilder();

    /**
     * 设置站点 id。
     *
     * @param id 站点标识
     * @return 当前构建器
     */
    public SiteBuilder id(String id) {
      b.setId(id);
      return this;
    }

    /**
     * 设置站点名称。
     *
     * @param name 名称
     * @return 当前构建器
     */
    public SiteBuilder name(String name) {
      b.setName(name);
      return this;
    }

    /**
     * 设置站点域名。
     *
     * @param domain 域名
     * @return 当前构建器
     */
    public SiteBuilder domain(String domain) {
      b.setDomain(domain);
      return this;
    }

    /**
     * 设置当前页面 URL。
     *
     * @param page 页面 URL
     * @return 当前构建器
     */
    public SiteBuilder page(String page) {
      b.setPage(page);
      return this;
    }

    /**
     * 设置 IAB 等内容分类（cat）。
     *
     * @param cats 分类列表
     * @return 当前构建器
     */
    public SiteBuilder cat(String... cats) {
      b.clearCat();
      for (String c : cats) {
        b.addCat(c);
      }
      return this;
    }

    /**
     * 设置发布商（publisher）。
     *
     * @param p Publisher 对象
     * @return 当前构建器
     */
    public SiteBuilder publisher(Publisher p) {
      b.setPublisher(p);
      return this;
    }

    /**
     * 构建 {@link Site}。
     *
     * @return Site 对象
     */
    public Site build() {
      return b.build();
    }
  }

  /** 移动应用（App）流式构建器。 */
  public static final class AppBuilder {
    private final App.Builder b = App.newBuilder();

    /**
     * 设置应用 id。
     *
     * @param id 应用标识
     * @return 当前构建器
     */
    public AppBuilder id(String id) {
      b.setId(id);
      return this;
    }

    /**
     * 设置应用名称。
     *
     * @param name 名称
     * @return 当前构建器
     */
    public AppBuilder name(String name) {
      b.setName(name);
      return this;
    }

    /**
     * 设置应用包名（bundle）。
     *
     * @param bundle 包名
     * @return 当前构建器
     */
    public AppBuilder bundle(String bundle) {
      b.setBundle(bundle);
      return this;
    }

    /**
     * 设置应用域名。
     *
     * @param domain 域名
     * @return 当前构建器
     */
    public AppBuilder domain(String domain) {
      b.setDomain(domain);
      return this;
    }

    /**
     * 设置发布商（publisher）。
     *
     * @param p Publisher 对象
     * @return 当前构建器
     */
    public AppBuilder publisher(Publisher p) {
      b.setPublisher(p);
      return this;
    }

    /**
     * 设置内容上下文（content）。
     *
     * @param c Content 对象
     * @return 当前构建器
     */
    public AppBuilder content(Content c) {
      b.setContent(c);
      return this;
    }

    /**
     * 构建 {@link App}。
     *
     * @return App 对象
     */
    public App build() {
      return b.build();
    }
  }

  /** 数字户外（Dooh）流式构建器。 */
  public static final class DoohBuilder {
    private final Dooh.Builder b = Dooh.newBuilder();

    /**
     * 设置 DOOH 位 id。
     *
     * @param id 标识
     * @return 当前构建器
     */
    public DoohBuilder id(String id) {
      b.setId(id);
      return this;
    }

    /**
     * 设置 DOOH 位名称。
     *
     * @param name 名称
     * @return 当前构建器
     */
    public DoohBuilder name(String name) {
      b.setName(name);
      return this;
    }

    /**
     * 设置场馆类型（venuetype）。
     *
     * @param ids 场馆类型 id 列表
     * @return 当前构建器
     */
    public DoohBuilder venueType(String... ids) {
      b.clearVenuetype();
      for (String id : ids) {
        b.addVenuetype(id);
      }
      return this;
    }

    /**
     * 设置场馆类型分类体系（venuetypetax）。
     *
     * @param tax 分类 tax 值
     * @return 当前构建器
     */
    public DoohBuilder venueTypeTax(int tax) {
      b.setVenuetypetax(tax);
      return this;
    }

    /**
     * 设置发布商（publisher）。
     *
     * @param p Publisher 对象
     * @return 当前构建器
     */
    public DoohBuilder publisher(Publisher p) {
      b.setPublisher(p);
      return this;
    }

    /**
     * 构建 {@link Dooh}。
     *
     * @return Dooh 对象
     */
    public Dooh build() {
      return b.build();
    }
  }

  /** 设备（Device）流式构建器。 */
  public static final class DeviceBuilder {
    private final Device.Builder b = Device.newBuilder();

    /**
     * 设置 User-Agent。
     *
     * @param ua UA 字符串
     * @return 当前构建器
     */
    public DeviceBuilder ua(String ua) {
      b.setUa(ua);
      return this;
    }

    /**
     * 设置 IPv4 地址。
     *
     * @param ip IP 地址
     * @return 当前构建器
     */
    public DeviceBuilder ip(String ip) {
      b.setIp(ip);
      return this;
    }

    /**
     * 设置设备类型（devicetype）。
     *
     * @param t OpenRTB 设备类型枚举值
     * @return 当前构建器
     */
    public DeviceBuilder deviceType(int t) {
      b.setDevicetype(t);
      return this;
    }

    /**
     * 设置设备制造商（make）。
     *
     * @param make 制造商
     * @return 当前构建器
     */
    public DeviceBuilder make(String make) {
      b.setMake(make);
      return this;
    }

    /**
     * 设置设备型号（model）。
     *
     * @param model 型号
     * @return 当前构建器
     */
    public DeviceBuilder model(String model) {
      b.setModel(model);
      return this;
    }

    /**
     * 设置操作系统及版本。
     *
     * @param os 操作系统名
     * @param osv 系统版本
     * @return 当前构建器
     */
    public DeviceBuilder os(String os, String osv) {
      b.setOs(os).setOsv(osv);
      return this;
    }

    /**
     * 设置 IFA（广告标识符）。
     *
     * @param ifa IFA 字符串
     * @return 当前构建器
     */
    public DeviceBuilder ifa(String ifa) {
      b.setIfa(ifa);
      return this;
    }

    /**
     * 设置地理位置（geo）。
     *
     * @param g Geo 对象
     * @return 当前构建器
     */
    public DeviceBuilder geo(Geo g) {
      b.setGeo(g);
      return this;
    }

    /**
     * 构建 {@link Device}。
     *
     * @return Device 对象
     */
    public Device build() {
      return b.build();
    }
  }

  /** 发布商（Publisher）流式构建器。 */
  public static final class PublisherBuilder {
    private final Publisher.Builder b = Publisher.newBuilder();

    /**
     * 设置发布商 id。
     *
     * @param id 标识
     * @return 当前构建器
     */
    public PublisherBuilder id(String id) {
      b.setId(id);
      return this;
    }

    /**
     * 设置发布商名称。
     *
     * @param name 名称
     * @return 当前构建器
     */
    public PublisherBuilder name(String name) {
      b.setName(name);
      return this;
    }

    /**
     * 设置发布商域名。
     *
     * @param domain 域名
     * @return 当前构建器
     */
    public PublisherBuilder domain(String domain) {
      b.setDomain(domain);
      return this;
    }

    /**
     * 构建 {@link Publisher}。
     *
     * @return Publisher 对象
     */
    public Publisher build() {
      return b.build();
    }
  }

  /** 内容（Content）流式构建器。 */
  public static final class ContentBuilder {
    private final Content.Builder b = Content.newBuilder();

    /**
     * 设置内容标题。
     *
     * @param title 标题
     * @return 当前构建器
     */
    public ContentBuilder title(String title) {
      b.setTitle(title);
      return this;
    }

    /**
     * 设置系列名（series）。
     *
     * @param series 系列
     * @return 当前构建器
     */
    public ContentBuilder series(String series) {
      b.setSeries(series);
      return this;
    }

    /**
     * 设置季（season）。
     *
     * @param season 季标识
     * @return 当前构建器
     */
    public ContentBuilder season(String season) {
      b.setSeason(season);
      return this;
    }

    /**
     * 设置集数（episode）。
     *
     * @param n 集号
     * @return 当前构建器
     */
    public ContentBuilder episode(int n) {
      b.setEpisode(n);
      return this;
    }

    /**
     * 设置内容上下文（context）。
     *
     * @param v context 枚举值
     * @return 当前构建器
     */
    public ContentBuilder context(int v) {
      b.setContext(v);
      return this;
    }

    /**
     * 设置是否直播（livestream）。
     *
     * @param v 0/1
     * @return 当前构建器
     */
    public ContentBuilder livestream(int v) {
      b.setLivestream(v);
      return this;
    }

    /**
     * 设置是否实时（realtime）。
     *
     * @param v 0/1
     * @return 当前构建器
     */
    public ContentBuilder realtime(int v) {
      b.setRealtime(v);
      return this;
    }

    /**
     * 构建 {@link Content}。
     *
     * @return Content 对象
     */
    public Content build() {
      return b.build();
    }
  }

  /** 地理位置（Geo）流式构建器。 */
  public static final class GeoBuilder {
    private final Geo.Builder b = Geo.newBuilder();

    /**
     * 设置纬度与经度。
     *
     * @param lat 纬度
     * @param lon 经度
     * @return 当前构建器
     */
    public GeoBuilder latLon(double lat, double lon) {
      b.setLat(lat).setLon(lon);
      return this;
    }

    /**
     * 设置定位类型（type）。
     *
     * @param t type 枚举值
     * @return 当前构建器
     */
    public GeoBuilder type(int t) {
      b.setType(t);
      return this;
    }

    /**
     * 设置国家代码（ISO-3166-1 alpha-3）。
     *
     * @param c 国家代码
     * @return 当前构建器
     */
    public GeoBuilder country(String c) {
      b.setCountry(c);
      return this;
    }

    /**
     * 设置城市名。
     *
     * @param c 城市
     * @return 当前构建器
     */
    public GeoBuilder city(String c) {
      b.setCity(c);
      return this;
    }

    /**
     * 构建 {@link Geo}。
     *
     * @return Geo 对象
     */
    public Geo build() {
      return b.build();
    }
  }
}

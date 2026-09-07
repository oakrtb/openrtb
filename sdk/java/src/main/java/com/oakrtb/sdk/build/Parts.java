package com.oakrtb.sdk.build;

import com.oakrtb.openrtb.v2.App;
import com.oakrtb.openrtb.v2.Content;
import com.oakrtb.openrtb.v2.Device;
import com.oakrtb.openrtb.v2.Dooh;
import com.oakrtb.openrtb.v2.Geo;
import com.oakrtb.openrtb.v2.Publisher;
import com.oakrtb.openrtb.v2.Site;

/** Small builders for Site / App / Dooh / Device / Publisher / Content / Geo. */
public final class Parts {
  private Parts() {}

  public static SiteBuilder site() {
    return new SiteBuilder();
  }

  public static AppBuilder app() {
    return new AppBuilder();
  }

  public static DoohBuilder dooh() {
    return new DoohBuilder();
  }

  public static DeviceBuilder device() {
    return new DeviceBuilder();
  }

  public static PublisherBuilder publisher() {
    return new PublisherBuilder();
  }

  public static ContentBuilder content() {
    return new ContentBuilder();
  }

  public static GeoBuilder geo() {
    return new GeoBuilder();
  }

  public static final class SiteBuilder {
    private final Site.Builder b = Site.newBuilder();

    public SiteBuilder id(String id) {
      b.setId(id);
      return this;
    }

    public SiteBuilder name(String name) {
      b.setName(name);
      return this;
    }

    public SiteBuilder domain(String domain) {
      b.setDomain(domain);
      return this;
    }

    public SiteBuilder page(String page) {
      b.setPage(page);
      return this;
    }

    public SiteBuilder cat(String... cats) {
      b.clearCat();
      for (String c : cats) {
        b.addCat(c);
      }
      return this;
    }

    public SiteBuilder publisher(Publisher p) {
      b.setPublisher(p);
      return this;
    }

    public Site build() {
      return b.build();
    }
  }

  public static final class AppBuilder {
    private final App.Builder b = App.newBuilder();

    public AppBuilder id(String id) {
      b.setId(id);
      return this;
    }

    public AppBuilder name(String name) {
      b.setName(name);
      return this;
    }

    public AppBuilder bundle(String bundle) {
      b.setBundle(bundle);
      return this;
    }

    public AppBuilder domain(String domain) {
      b.setDomain(domain);
      return this;
    }

    public AppBuilder publisher(Publisher p) {
      b.setPublisher(p);
      return this;
    }

    public AppBuilder content(Content c) {
      b.setContent(c);
      return this;
    }

    public App build() {
      return b.build();
    }
  }

  public static final class DoohBuilder {
    private final Dooh.Builder b = Dooh.newBuilder();

    public DoohBuilder id(String id) {
      b.setId(id);
      return this;
    }

    public DoohBuilder name(String name) {
      b.setName(name);
      return this;
    }

    public DoohBuilder venueType(String... ids) {
      b.clearVenuetype();
      for (String id : ids) {
        b.addVenuetype(id);
      }
      return this;
    }

    public DoohBuilder venueTypeTax(int tax) {
      b.setVenuetypetax(tax);
      return this;
    }

    public DoohBuilder publisher(Publisher p) {
      b.setPublisher(p);
      return this;
    }

    public Dooh build() {
      return b.build();
    }
  }

  public static final class DeviceBuilder {
    private final Device.Builder b = Device.newBuilder();

    public DeviceBuilder ua(String ua) {
      b.setUa(ua);
      return this;
    }

    public DeviceBuilder ip(String ip) {
      b.setIp(ip);
      return this;
    }

    public DeviceBuilder deviceType(int t) {
      b.setDevicetype(t);
      return this;
    }

    public DeviceBuilder make(String make) {
      b.setMake(make);
      return this;
    }

    public DeviceBuilder model(String model) {
      b.setModel(model);
      return this;
    }

    public DeviceBuilder os(String os, String osv) {
      b.setOs(os).setOsv(osv);
      return this;
    }

    public DeviceBuilder ifa(String ifa) {
      b.setIfa(ifa);
      return this;
    }

    public DeviceBuilder geo(Geo g) {
      b.setGeo(g);
      return this;
    }

    public Device build() {
      return b.build();
    }
  }

  public static final class PublisherBuilder {
    private final Publisher.Builder b = Publisher.newBuilder();

    public PublisherBuilder id(String id) {
      b.setId(id);
      return this;
    }

    public PublisherBuilder name(String name) {
      b.setName(name);
      return this;
    }

    public PublisherBuilder domain(String domain) {
      b.setDomain(domain);
      return this;
    }

    public Publisher build() {
      return b.build();
    }
  }

  public static final class ContentBuilder {
    private final Content.Builder b = Content.newBuilder();

    public ContentBuilder title(String title) {
      b.setTitle(title);
      return this;
    }

    public ContentBuilder series(String series) {
      b.setSeries(series);
      return this;
    }

    public ContentBuilder season(String season) {
      b.setSeason(season);
      return this;
    }

    public ContentBuilder episode(int n) {
      b.setEpisode(n);
      return this;
    }

    public ContentBuilder context(int v) {
      b.setContext(v);
      return this;
    }

    public ContentBuilder livestream(int v) {
      b.setLivestream(v);
      return this;
    }

    public ContentBuilder realtime(int v) {
      b.setRealtime(v);
      return this;
    }

    public Content build() {
      return b.build();
    }
  }

  public static final class GeoBuilder {
    private final Geo.Builder b = Geo.newBuilder();

    public GeoBuilder latLon(double lat, double lon) {
      b.setLat(lat).setLon(lon);
      return this;
    }

    public GeoBuilder type(int t) {
      b.setType(t);
      return this;
    }

    public GeoBuilder country(String c) {
      b.setCountry(c);
      return this;
    }

    public GeoBuilder city(String c) {
      b.setCity(c);
      return this;
    }

    public Geo build() {
      return b.build();
    }
  }
}

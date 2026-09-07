package com.oakrtb.sdk.build;

import com.oakrtb.openrtb.v2.Audio;
import com.oakrtb.openrtb.v2.Banner;
import com.oakrtb.openrtb.v2.Imp;
import com.oakrtb.openrtb.v2.Native;
import com.oakrtb.openrtb.v2.Video;

/**
 * 各广告格式 {@link Imp} 的工厂与流式构建器。
 *
 * <p>提供 banner、video、audio、native 四类 Imp 的便捷组装，供 {@link BidRequestBuilder#addImp(Imp)} 使用。
 */
public final class ImpBuilders {
  private ImpBuilders() {}

  /**
   * 创建 Banner 格式 Imp 构建器。
   *
   * @param id Imp 唯一 id
   * @return Banner Imp 构建器
   */
  public static BannerImp banner(String id) {
    return new BannerImp(id);
  }

  /**
   * 创建 Video 格式 Imp 构建器。
   *
   * @param id Imp 唯一 id
   * @return Video Imp 构建器
   */
  public static VideoImp video(String id) {
    return new VideoImp(id);
  }

  /**
   * 创建 Audio 格式 Imp 构建器。
   *
   * @param id Imp 唯一 id
   * @return Audio Imp 构建器
   */
  public static AudioImp audio(String id) {
    return new AudioImp(id);
  }

  /**
   * 创建 Native 格式 Imp 构建器。
   *
   * @param id Imp 唯一 id
   * @return Native Imp 构建器
   */
  public static NativeImp nativeAd(String id) {
    return new NativeImp(id);
  }

  /**
   * Imp 构建器公共基类，封装底价、安全、标签等通用字段。
   *
   * @param <T> 具体子类类型（CRTP）
   */
  public abstract static class ImpBase<T extends ImpBase<T>> {
    protected final Imp.Builder imp = Imp.newBuilder();

    @SuppressWarnings("unchecked")
    protected T self() {
      return (T) this;
    }

    protected ImpBase(String id) {
      imp.setId(id);
    }

    /**
     * 设置底价及货币。
     *
     * @param bidfloor 底价
     * @param cur ISO-4217 货币代码
     * @return 当前构建器
     */
    public T floor(double bidfloor, String cur) {
      imp.setBidfloor(bidfloor).setBidfloorcur(cur);
      return self();
    }

    /**
     * 要求 HTTPS 创意（secure=1）。
     *
     * @return 当前构建器
     */
    public T secure() {
      imp.setSecure(1);
      return self();
    }

    /**
     * 设置广告位 tag id（tagid）。
     *
     * @param tagid 标签标识
     * @return 当前构建器
     */
    public T tagId(String tagid) {
      imp.setTagid(tagid);
      return self();
    }

    /**
     * 标记为插屏（instl=1）。
     *
     * @return 当前构建器
     */
    public T interstitial() {
      imp.setInstl(1);
      return self();
    }

    /**
     * 标记为激励广告（rwdd=1）。
     *
     * @return 当前构建器
     */
    public T rewarded() {
      imp.setRwdd(1);
      return self();
    }

    /**
     * 构建 protobuf {@link Imp}。
     *
     * @return 展示位对象
     */
    public abstract Imp build();
  }

  /**
   * Banner 格式 Imp 构建器。
   */
  public static final class BannerImp extends ImpBase<BannerImp> {
    private final Banner.Builder banner = Banner.newBuilder();

    BannerImp(String id) {
      super(id);
    }

    /**
     * 设置 Banner 宽高。
     *
     * @param w 宽度（像素）
     * @param h 高度（像素）
     * @return 当前构建器
     */
    public BannerImp size(int w, int h) {
      banner.setW(w).setH(h);
      return this;
    }

    /**
     * 设置广告位位置（pos）。
     *
     * @param pos OpenRTB 位置枚举值
     * @return 当前构建器
     */
    public BannerImp pos(int pos) {
      banner.setPos(pos);
      return this;
    }

    /**
     * 设置允许的 MIME 类型。
     *
     * @param mimes 如 image/jpeg
     * @return 当前构建器
     */
    public BannerImp mimes(String... mimes) {
      banner.clearMimes();
      for (String m : mimes) {
        banner.addMimes(m);
      }
      return this;
    }

    @Override
    public Imp build() {
      return imp.setBanner(banner).build();
    }
  }

  /**
   * Video 格式 Imp 构建器。
   */
  public static final class VideoImp extends ImpBase<VideoImp> {
    private final Video.Builder video = Video.newBuilder();

    VideoImp(String id) {
      super(id);
    }

    /**
     * 设置允许的 MIME 类型（必填）。
     *
     * @param mimes 如 video/mp4
     * @return 当前构建器
     */
    public VideoImp mimes(String... mimes) {
      video.clearMimes();
      for (String m : mimes) {
        video.addMimes(m);
      }
      return this;
    }

    /**
     * 设置最小时长与最大时长（秒）。
     *
     * @param min 最小时长
     * @param max 最大时长
     * @return 当前构建器
     */
    public VideoImp duration(int min, int max) {
      video.setMinduration(min).setMaxduration(max);
      return this;
    }

    /**
     * 设置支持的 VAST 协议列表。
     *
     * @param protocols 协议枚举值
     * @return 当前构建器
     */
    public VideoImp protocols(int... protocols) {
      video.clearProtocols();
      for (int p : protocols) {
        video.addProtocols(p);
      }
      return this;
    }

    /**
     * 设置播放器尺寸。
     *
     * @param w 宽度
     * @param h 高度
     * @return 当前构建器
     */
    public VideoImp size(int w, int h) {
      video.setW(w).setH(h);
      return this;
    }

    /**
     * 设置 startdelay。
     *
     * @param v startdelay 值
     * @return 当前构建器
     */
    public VideoImp startDelay(int v) {
      video.setStartdelay(v);
      return this;
    }

    /**
     * 设置视频投放类型（plcmt）。
     *
     * @param plcmt plcmt 枚举值
     * @return 当前构建器
     */
    public VideoImp plcmt(int plcmt) {
      video.setPlcmt(plcmt);
      return this;
    }

    /**
     * 设置线性/非线性（linearity）。
     *
     * @param v linearity 值
     * @return 当前构建器
     */
    public VideoImp linearity(int v) {
      video.setLinearity(v);
      return this;
    }

    /**
     * 启用可跳过并设置 skipafter（秒）。
     *
     * @param skipafter 可跳过前的秒数
     * @return 当前构建器
     */
    public VideoImp skip(int skipafter) {
      video.setSkip(1).setSkipafter(skipafter);
      return this;
    }

    /**
     * 设置 pod 信息。
     *
     * @param podid pod id
     * @param slotinpod pod 内槽位
     * @return 当前构建器
     */
    public VideoImp pod(String podid, int slotinpod) {
      video.setPodid(podid).setSlotinpod(slotinpod);
      return this;
    }

    /**
     * 设置播放方式（playbackmethod）。
     *
     * @param methods 播放方式枚举值
     * @return 当前构建器
     */
    public VideoImp playbackMethod(int... methods) {
      video.clearPlaybackmethod();
      for (int m : methods) {
        video.addPlaybackmethod(m);
      }
      return this;
    }

    @Override
    public Imp build() {
      return imp.setVideo(video).build();
    }
  }

  /**
   * Audio 格式 Imp 构建器。
   */
  public static final class AudioImp extends ImpBase<AudioImp> {
    private final Audio.Builder audio = Audio.newBuilder();

    AudioImp(String id) {
      super(id);
    }

    /**
     * 设置允许的 MIME 类型（必填）。
     *
     * @param mimes 如 audio/mpeg
     * @return 当前构建器
     */
    public AudioImp mimes(String... mimes) {
      audio.clearMimes();
      for (String m : mimes) {
        audio.addMimes(m);
      }
      return this;
    }

    /**
     * 设置最小时长与最大时长（秒）。
     *
     * @param min 最小时长
     * @param max 最大时长
     * @return 当前构建器
     */
    public AudioImp duration(int min, int max) {
      audio.setMinduration(min).setMaxduration(max);
      return this;
    }

    /**
     * 设置支持的协议列表。
     *
     * @param protocols 协议枚举值
     * @return 当前构建器
     */
    public AudioImp protocols(int... protocols) {
      audio.clearProtocols();
      for (int p : protocols) {
        audio.addProtocols(p);
      }
      return this;
    }

    /**
     * 设置 feed 类型。
     *
     * @param feed feed 枚举值
     * @return 当前构建器
     */
    public AudioImp feed(int feed) {
      audio.setFeed(feed);
      return this;
    }

    @Override
    public Imp build() {
      return imp.setAudio(audio).build();
    }
  }

  /**
   * Native 格式 Imp 构建器。
   */
  public static final class NativeImp extends ImpBase<NativeImp> {
    private final Native.Builder nativeAd = Native.newBuilder().setVer("1.2");

    NativeImp(String id) {
      super(id);
    }

    /**
     * 设置 Native Request JSON 字符串（必填）。
     *
     * @param nativeRequestJson Native 1.x request 对象 JSON
     * @return 当前构建器
     */
    public NativeImp request(String nativeRequestJson) {
      nativeAd.setRequest(nativeRequestJson);
      return this;
    }

    /**
     * 设置 Native 规范版本（ver）。
     *
     * @param ver 版本号，默认 1.2
     * @return 当前构建器
     */
    public NativeImp ver(String ver) {
      nativeAd.setVer(ver);
      return this;
    }

    @Override
    public Imp build() {
      return imp.setNative(nativeAd).build();
    }
  }
}

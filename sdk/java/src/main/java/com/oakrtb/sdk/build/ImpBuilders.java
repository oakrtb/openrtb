package com.oakrtb.sdk.build;

import com.oakrtb.openrtb.v2.Audio;
import com.oakrtb.openrtb.v2.Banner;
import com.oakrtb.openrtb.v2.Imp;
import com.oakrtb.openrtb.v2.Native;
import com.oakrtb.openrtb.v2.Video;

/** Factories for format-specific Imp objects. */
public final class ImpBuilders {
  private ImpBuilders() {}

  public static BannerImp banner(String id) {
    return new BannerImp(id);
  }

  public static VideoImp video(String id) {
    return new VideoImp(id);
  }

  public static AudioImp audio(String id) {
    return new AudioImp(id);
  }

  public static NativeImp nativeAd(String id) {
    return new NativeImp(id);
  }

  public abstract static class ImpBase<T extends ImpBase<T>> {
    protected final Imp.Builder imp = Imp.newBuilder();

    @SuppressWarnings("unchecked")
    protected T self() {
      return (T) this;
    }

    protected ImpBase(String id) {
      imp.setId(id);
    }

    public T floor(double bidfloor, String cur) {
      imp.setBidfloor(bidfloor).setBidfloorcur(cur);
      return self();
    }

    public T secure() {
      imp.setSecure(1);
      return self();
    }

    public T tagId(String tagid) {
      imp.setTagid(tagid);
      return self();
    }

    public T interstitial() {
      imp.setInstl(1);
      return self();
    }

    public T rewarded() {
      imp.setRwdd(1);
      return self();
    }

    public abstract Imp build();
  }

  public static final class BannerImp extends ImpBase<BannerImp> {
    private final Banner.Builder banner = Banner.newBuilder();

    BannerImp(String id) {
      super(id);
    }

    public BannerImp size(int w, int h) {
      banner.setW(w).setH(h);
      return this;
    }

    public BannerImp pos(int pos) {
      banner.setPos(pos);
      return this;
    }

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

  public static final class VideoImp extends ImpBase<VideoImp> {
    private final Video.Builder video = Video.newBuilder();

    VideoImp(String id) {
      super(id);
    }

    public VideoImp mimes(String... mimes) {
      video.clearMimes();
      for (String m : mimes) {
        video.addMimes(m);
      }
      return this;
    }

    public VideoImp duration(int min, int max) {
      video.setMinduration(min).setMaxduration(max);
      return this;
    }

    public VideoImp protocols(int... protocols) {
      video.clearProtocols();
      for (int p : protocols) {
        video.addProtocols(p);
      }
      return this;
    }

    public VideoImp size(int w, int h) {
      video.setW(w).setH(h);
      return this;
    }

    public VideoImp startDelay(int v) {
      video.setStartdelay(v);
      return this;
    }

    public VideoImp plcmt(int plcmt) {
      video.setPlcmt(plcmt);
      return this;
    }

    public VideoImp linearity(int v) {
      video.setLinearity(v);
      return this;
    }

    public VideoImp skip(int skipafter) {
      video.setSkip(1).setSkipafter(skipafter);
      return this;
    }

    public VideoImp pod(String podid, int slotinpod) {
      video.setPodid(podid).setSlotinpod(slotinpod);
      return this;
    }

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

  public static final class AudioImp extends ImpBase<AudioImp> {
    private final Audio.Builder audio = Audio.newBuilder();

    AudioImp(String id) {
      super(id);
    }

    public AudioImp mimes(String... mimes) {
      audio.clearMimes();
      for (String m : mimes) {
        audio.addMimes(m);
      }
      return this;
    }

    public AudioImp duration(int min, int max) {
      audio.setMinduration(min).setMaxduration(max);
      return this;
    }

    public AudioImp protocols(int... protocols) {
      audio.clearProtocols();
      for (int p : protocols) {
        audio.addProtocols(p);
      }
      return this;
    }

    public AudioImp feed(int feed) {
      audio.setFeed(feed);
      return this;
    }

    @Override
    public Imp build() {
      return imp.setAudio(audio).build();
    }
  }

  public static final class NativeImp extends ImpBase<NativeImp> {
    private final Native.Builder nativeAd = Native.newBuilder().setVer("1.2");

    NativeImp(String id) {
      super(id);
    }

    public NativeImp request(String nativeRequestJson) {
      nativeAd.setRequest(nativeRequestJson);
      return this;
    }

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

package me.conji.cauldron;

public class TargetDescriptor {
  private String platform;
  private String version;

  public TargetDescriptor(String platform, String version) {
    this.platform = platform;
    this.version = version;
  }

  public String getPlatform() {
    return this.platform;
  }

  public String getVersion() {
    return this.version;
  }
}
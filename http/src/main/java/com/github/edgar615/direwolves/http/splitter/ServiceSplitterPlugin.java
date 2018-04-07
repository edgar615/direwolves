package com.github.edgar615.direwolves.http.splitter;

import com.github.edgar615.direwolves.core.definition.ApiPlugin;

import java.util.HashMap;
import java.util.Map;

public class ServiceSplitterPlugin implements ApiPlugin {

  private Map<String, ServiceTraffic> traffics = new HashMap<>();

  public ServiceSplitterPlugin() {
  }

  public ServiceTraffic traffic(String service) {
    return traffics.get(service);
  }

  public ServiceTraffic addTraffic(String service, ServiceTraffic traffic) {
    return traffics.put(service, traffic);
  }

  public Map<String, ServiceTraffic> traffics() {
    return traffics;
  }

  @Override
  public String name() {
    return ServiceSplitterPlugin.class.getSimpleName();
  }
}

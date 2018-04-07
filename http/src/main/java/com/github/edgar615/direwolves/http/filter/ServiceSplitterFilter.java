package com.github.edgar615.direwolves.http.filter;

import com.github.edgar615.direwolves.core.apidiscovery.ApiDiscovery;
import com.github.edgar615.direwolves.core.apidiscovery.ApiDiscoveryOptions;
import com.github.edgar615.direwolves.core.apidiscovery.ApiFinder;
import com.github.edgar615.direwolves.core.definition.ApiDefinition;
import com.github.edgar615.direwolves.core.dispatch.ApiContext;
import com.github.edgar615.direwolves.core.dispatch.Filter;
import com.github.edgar615.direwolves.http.SdHttpEndpoint;
import com.github.edgar615.direwolves.http.splitter.ServiceSplitterPlugin;
import com.github.edgar615.direwolves.http.splitter.ServiceSplitterPluginFactory;
import com.github.edgar615.direwolves.http.splitter.ServiceTraffic;
import com.github.edgar615.util.base.VersionUtils;
import com.github.edgar615.util.exception.DefaultErrorCode;
import com.github.edgar615.util.exception.SystemException;
import com.google.common.base.Strings;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.stream.Collectors;

public class ServiceSplitterFilter implements Filter {

  private final Vertx vertx;

  private final ServiceSplitterPlugin splitterPlugin;

  public ServiceSplitterFilter(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    if (config.getValue("service.splitter") instanceof JsonObject) {
      JsonObject jsonObject = new JsonObject()
              .put("service.splitter", config.getValue("service.splitter"));
      this.splitterPlugin = (ServiceSplitterPlugin) new ServiceSplitterPluginFactory().decode(jsonObject);
    } else {
      this.splitterPlugin = new ServiceSplitterPlugin();
    }

  }

  @Override
  public String type() {
    return Filter.PRE;
  }

  @Override
  public int order() {
    return 12500;
  }

  @Override
  public boolean shouldFilter(ApiContext apiContext) {
    return splitterPlugin != null
            && apiContext.apiDefinition().endpoints().stream()
            .anyMatch(e -> e instanceof SdHttpEndpoint);
  }

  @Override
  public void doFilter(ApiContext apiContext, Future<ApiContext> completeFuture) {
    Map<String, ServiceTraffic> traffics
            = apiContext.apiDefinition().endpoints().stream()
            .filter(e -> e instanceof SdHttpEndpoint)
            .map(e -> (SdHttpEndpoint) e)
            .map(e -> e.service())
            .filter(s -> splitterPlugin.traffic(s) != null)
            .collect(Collectors.toMap(s -> s, s -> splitterPlugin.traffic(s)));
    if (traffics.isEmpty()) {
      completeFuture.complete(apiContext);
      return;
    }
    ServiceSplitterPlugin serviceSplitterPlugin = new ServiceSplitterPlugin();
    traffics.forEach((k, v) -> serviceSplitterPlugin.addTraffic(k, v));
    apiContext.apiDefinition().addPlugin(serviceSplitterPlugin);
    completeFuture.complete(apiContext);
  }

}

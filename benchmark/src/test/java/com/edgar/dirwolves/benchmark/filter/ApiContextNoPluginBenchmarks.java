package com.edgar.dirwolves.benchmark.filter;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import com.edgar.direwolves.core.definition.ApiDefinition;
import com.edgar.direwolves.core.definition.HttpEndpoint;
import com.edgar.direwolves.core.dispatch.ApiContext;
import com.edgar.direwolves.plugin.acl.AclRestrictionFactory;
import com.edgar.direwolves.plugin.acl.AclRestrictionPlugin;
import com.edgar.direwolves.plugin.appkey.AppKeyPlugin;
import com.edgar.direwolves.plugin.appkey.AppKeyPluginFactory;
import com.edgar.direwolves.plugin.arg.BodyArgPlugin;
import com.edgar.direwolves.plugin.arg.BodyArgPluginFactory;
import com.edgar.direwolves.plugin.arg.Parameter;
import com.edgar.direwolves.plugin.arg.UrlArgPlugin;
import com.edgar.direwolves.plugin.arg.UrlArgPluginFactory;
import com.edgar.direwolves.plugin.ip.IpRestriction;
import com.edgar.direwolves.plugin.ip.IpRestrictionFactory;
import com.edgar.util.validation.Rule;
import io.vertx.core.http.HttpMethod;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

/**
 * Created by Edgar on 2017/7/17.
 *
 * @author Edgar  Date 2017/7/17
 */
@State(Scope.Benchmark)
public class ApiContextNoPluginBenchmarks {

  private ApiContext apiContext;

  @Setup
  public void setup() {
    Multimap<String, String> params = ArrayListMultimap.create();
    params.put("encryptKey", "0000000000000000");
    params.put("barcode", "LH10312ACCF23C4F3A5");

    apiContext = ApiContext.create(HttpMethod.POST, "/devices", params, null, null);

    HttpEndpoint httpEndpoint = HttpEndpoint.http("device.add", HttpMethod.POST, "/devices", "device");
    ApiDefinition apiDefinition = ApiDefinition.create("device.add", HttpMethod.POST, "/devices",
                                                       Lists.newArrayList(httpEndpoint));
    apiContext.setApiDefinition(apiDefinition);
  }


  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Fork(1)
  @OperationsPerInvocation(100)
  public ApiContext testApi() {
    return  apiContext.copy();
//    blackhole.consume(copy);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @Fork(1)
  @OperationsPerInvocation(100)
  public ApiContext testAverage() {
    return apiContext.copy();
  }


}

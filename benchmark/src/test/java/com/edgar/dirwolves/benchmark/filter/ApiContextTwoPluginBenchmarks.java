package com.edgar.dirwolves.benchmark.filter;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import com.edgar.direwolves.core.definition.ApiDefinition;
import com.edgar.direwolves.core.definition.HttpEndpoint;
import com.edgar.direwolves.core.definition.SimpleHttpEndpoint;
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
import com.edgar.util.base.Randoms;
import com.edgar.util.validation.Rule;
import io.vertx.core.http.HttpMethod;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Created by Edgar on 2017/7/17.
 *
 * @author Edgar  Date 2017/7/17
 */
@State(Scope.Benchmark)
@Warmup(iterations = 20)
public class ApiContextTwoPluginBenchmarks {

  private ApiContext apiContext;

  @Setup(Level.Invocation)
  public void setup() {
    Multimap<String, String> params = ArrayListMultimap.create();
    params.put("encryptKey", Randoms.randomAlphabetAndNum(16));
    params.put("barcode", Randoms.randomAlphabetAndNum(19));

    apiContext = ApiContext.create(HttpMethod.POST, "/devices", params, null, null);

    HttpEndpoint httpEndpoint = SimpleHttpEndpoint.http("device.add", HttpMethod.POST,
                                                        "/devices", 80, "localhost");
    ApiDefinition apiDefinition = ApiDefinition.create("device.add", HttpMethod.POST, "/devices",
                                                       Lists.newArrayList(httpEndpoint));
    BodyArgPlugin plugin = (BodyArgPlugin) new BodyArgPluginFactory().create();
    Parameter parameter = Parameter.create("barcode", null);
    parameter.addRule(Rule.required());
    parameter.addRule(Rule.regex("LH[0-7][0-9a-fA-F]{2}[0-5][0-4][0-9a-fA-F]{12}"));
    plugin.add(parameter);
    parameter = Parameter.create("encryptKey", null);
    parameter.addRule(Rule.required());
    parameter.addRule(Rule.regex("[0-9A-F]{16}"));
    plugin.add(parameter);
    apiDefinition.addPlugin(plugin);

    IpRestriction ipRestriction = (IpRestriction) new IpRestrictionFactory().create();
    apiDefinition.addPlugin(ipRestriction);


    apiContext.setApiDefinition(apiDefinition);
  }


  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Fork(1)
  @OperationsPerInvocation(10000)
  public ApiContext testApi() {
    return  apiContext.copy();
//    blackhole.consume(copy);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @Fork(1)
  @OperationsPerInvocation(10000)
  public ApiContext testAverage() {
    return apiContext.copy();
  }

}

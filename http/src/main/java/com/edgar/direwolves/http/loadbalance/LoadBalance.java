package com.edgar.direwolves.http.loadbalance;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.Record;

/**
 * Created by Edgar on 2017/7/31.
 *
 * @author Edgar  Date 2017/7/31
 */
public interface LoadBalance {

  void chooseServer(String service, Handler<AsyncResult<Record>> resultHandler);

  static LoadBalance create(Vertx vertx, JsonObject config) {
    ServiceFinder serviceFinder = ServiceFinder.create(vertx, config);
    return new LoadBalanceImpl(serviceFinder, config);
  }
}

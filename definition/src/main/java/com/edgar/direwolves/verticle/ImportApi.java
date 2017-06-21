package com.edgar.direwolves.verticle;

import com.edgar.direwolves.cmd.ImportApiCmd;
import com.edgar.direwolves.core.cmd.ApiCmd;
import com.edgar.util.vertx.spi.Initializable;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动时加载API.
 *
 * @author Edgar  Date 2017/3/30
 */
public class ImportApi implements Initializable {
  @Override
  public void initialize(Vertx vertx, JsonObject config, Future<Void> complete) {
    JsonObject apiConfig = config.getJsonObject("router.dir", new JsonObject());

    List<Future> futures = new ArrayList<>();
    for (String namespace : apiConfig.fieldNames()) {
      JsonObject _config = apiConfig.getJsonObject(namespace);
      String path = _config.getString("path");
      ApiCmd cmd = new ImportApiCmd(vertx);
      Future<JsonObject> imported = cmd.handle(new JsonObject().put("path", path)
                                                       .put("namespace", namespace));
      futures.add(imported);
    }
    if (futures.isEmpty()) {
      complete.complete();
      return;
    }

    CompositeFuture.all(futures)
            .setHandler(ar -> {
              if (ar.succeeded()) {
                complete.complete();
              } else {
                complete.fail(ar.cause());
              }
            });

  }
}

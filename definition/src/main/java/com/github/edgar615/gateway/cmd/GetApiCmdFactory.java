package com.github.edgar615.gateway.cmd;

import com.github.edgar615.gateway.core.cmd.ApiCmd;
import com.github.edgar615.gateway.core.cmd.ApiCmdFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * GetApiCmd的工厂类.
 *
 * @author Edgar  Date 2017/1/19
 */
public class GetApiCmdFactory implements ApiCmdFactory {
    @Override
    public ApiCmd create(Vertx vertx, JsonObject config) {
        return new GetApiCmd(vertx, config);
    }
}
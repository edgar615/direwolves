package com.github.edgar615.gateway.test.arg;

import com.github.edgar615.util.exception.DefaultErrorCode;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Edgar on 2017/2/7.
 *
 * @author Edgar  Date 2017/2/7
 */
@RunWith(VertxUnitRunner.class)
public class StrictArgTest {

    @Test
    public void undefinedArgShouldThrowInvalidArg(TestContext testContext) {
        AtomicBoolean check = new AtomicBoolean();
        Vertx.vertx().createHttpClient().post(9000, "localhost", "/arg/strict")
                .handler(resp -> {
                    testContext.assertEquals(400, resp.statusCode());
                    testContext.assertTrue(resp.headers().contains("x-request-id"));
                    resp.bodyHandler(body -> {
                        System.out.println(body.toString());
                        testContext.assertEquals(DefaultErrorCode.INVALID_ARGS.getNumber(),
                                                 body.toJsonObject().getInteger("code"));
                        JsonObject details =
                                body.toJsonObject().getJsonObject("details", new JsonObject());
                        testContext.assertEquals(1, details.size());
                        testContext.assertTrue(details.containsKey("start"));
                        check.set(true);
                    });
                })
                .setChunked(true)
                .end(new JsonObject().put("deviceType", 1).put("start", 5).encode());
        Awaitility.await().until(() -> check.get());
    }

    @Test
    public void testSuccess(TestContext testContext) {
        AtomicBoolean check = new AtomicBoolean();
        String url = "/arg/strict";
        Vertx.vertx().createHttpClient().post(9000, "localhost", url)
                .handler(resp -> {
                    testContext.assertEquals(200, resp.statusCode());
                    testContext.assertTrue(resp.headers().contains("x-request-id"));
                    resp.bodyHandler(body -> {
                        System.out.println(body.toString());
                        JsonObject jsonObject = body.toJsonObject();
                        check.set(true);
                    });
                })
                .setChunked(true)
                .end(new JsonObject().put("deviceType", 1).encode());
        Awaitility.await().until(() -> check.get());
    }

}

package com.edgar.direwolves.filter;

import com.edgar.direwolves.dispatch.ApiContext;
import com.edgar.util.exception.DefaultErrorCode;
import com.edgar.util.exception.SystemException;
import com.edgar.util.validation.ValidationException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;

/**
 * Created by Edgar on 2016/9/20.
 *
 * @author Edgar  Date 2016/9/20
 */
@RunWith(VertxUnitRunner.class)
public class TImeoutFilterTest {

    Vertx vertx;

    @Before
    public void setUp(TestContext testContext) {
        vertx = Vertx.vertx();
    }


    @Test
    public void testTimeoutParam(TestContext testContext) {

        ApiContext apiContext = ApiContext.builder()
                .setMethod(HttpMethod.GET)
                .setPath("/devices")
                .build();

        TimeoutFilter filter = new TimeoutFilter();
        filter.config(vertx, new JsonObject());

        Future<ApiContext> future = Future.future();
        try {
            filter.doFilter(apiContext, future);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ValidationException);
        }
    }

    @Test
    public void testTimeout(TestContext testContext) {

        Multimap<String, String> params = ArrayListMultimap.create();
        params.put("timestamp", Instant.now().getEpochSecond() + (10 * 60) + "");

        ApiContext apiContext = ApiContext.builder()
                .setMethod(HttpMethod.GET)
                .setPath("/devices")
                .setParams(params)
                .build();

        TimeoutFilter filter = new TimeoutFilter();
        filter.config(vertx, new JsonObject());

        Future<ApiContext> future = Future.future();
        filter.doFilter(apiContext, future);

        Async async = testContext.async();
        future.setHandler(ar -> {
            if (ar.succeeded()) {
                testContext.fail();
            } else {
                Throwable e = ar.cause();
                testContext.assertTrue(e instanceof SystemException);
                SystemException ex = (SystemException) e;
                testContext.assertEquals(DefaultErrorCode.EXPIRE.getNumber(), ex.getErrorCode().getNumber());

                async.complete();
            }
        });
    }

    @Test
    public void testTimeoutOK(TestContext testContext) {

        Multimap<String, String> params = ArrayListMultimap.create();
        params.put("timestamp", Instant.now().getEpochSecond() + "");

        ApiContext apiContext = ApiContext.builder()
                .setMethod(HttpMethod.GET)
                .setPath("/devices")
                .setParams(params)
                .build();

        TimeoutFilter filter = new TimeoutFilter();
        filter.config(vertx, new JsonObject());

        Future<ApiContext> future = Future.future();
        filter.doFilter(apiContext, future);
        future.setHandler(ar -> {
            if (ar.succeeded()) {
                ApiContext apiContext1 = ar.result();
                testContext.assertTrue(apiContext1.params().containsKey("timestamp"));
            } else {
                testContext.fail();
            }
        });
    }


}
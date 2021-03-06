package com.github.edgar615.gateway.plugin.arg;

import com.google.common.collect.ArrayListMultimap;

import com.github.edgar615.gateway.core.dispatch.ApiContext;
import com.github.edgar615.gateway.core.dispatch.Filter;
import com.github.edgar615.util.validation.ValidationException;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * 严格校验参数.
 * 如果请求体或者请求参数中包括了未定义的参数，直接抛出ValidationException
 * 该filter可以接受下列的配置参数
 * <pre>
 *   strict.arg.enable bool值是否启用，默认值false
 *   strict.arg.query.excludes 数组，请求参数中允许的例外
 *   strict.arg.body.excludes 数组，请求体中允许的例外
 * </pre>
 * 该filter的order=99
 * Created by edgar on 16-10-28.
 */
public class StrictArgFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(StrictArgFilter.class);

    private final Set<String> excludeQuery = new HashSet<>();

    private final Set<String> excludeBody = new HashSet<>();

    private volatile boolean enabled = false;

    StrictArgFilter(JsonObject config) {
        updateConfig(config);
    }

    @Override
    public String type() {
        return PRE;
    }

    @Override
    public int order() {
        return 8900;
    }

    @Override
    public boolean shouldFilter(ApiContext apiContext) {
        StrictArgPlugin plugin
                = (StrictArgPlugin) apiContext.apiDefinition()
                .plugin(StrictArgPlugin.class.getSimpleName());
        if (plugin == null) {
            return enabled;
        } else {
            return plugin.strict();
        }
    }

    @Override
    public void doFilter(ApiContext apiContext, Future<ApiContext> completeFuture) {
        UrlArgPlugin urlArgPlugin =
                (UrlArgPlugin) apiContext.apiDefinition()
                        .plugin(UrlArgPlugin.class.getSimpleName());

        ArrayListMultimap error = ArrayListMultimap.create();
        if (!apiContext.params().isEmpty()) {
            apiContext.params().keySet().stream()
                    .filter(k -> testUrlArg(k, urlArgPlugin))
                    .forEach(k -> error.put(k, "prohibited"));
        }
        if (!error.isEmpty()) {
            LOGGER.warn("[{}] [StrictArgFilter] [{}]", apiContext.id(), "ArgProhibited");
            throw new ValidationException(error);
        }

        BodyArgPlugin bodyArgPlugin =
                (BodyArgPlugin) apiContext.apiDefinition()
                        .plugin(BodyArgPlugin.class.getSimpleName());
        ArrayListMultimap bodyError = ArrayListMultimap.create();
        if (apiContext.body() != null) {
            apiContext.body().fieldNames().stream()
                    .filter(k -> testBodyArg(k, bodyArgPlugin))
                    .forEach(k -> bodyError.put(k, "prohibited"));
        }
        if (!bodyError.isEmpty()) {
            throw new ValidationException(bodyError);
        }
        completeFuture.complete(apiContext);
    }

    @Override
    public void updateConfig(JsonObject config) {
        if (config.getValue("strict.arg") instanceof JsonObject) {
            JsonObject jsonObject = config.getJsonObject("strict.arg", new JsonObject());
            JsonArray queryArray = jsonObject.getJsonArray("query.excludes", new JsonArray());
            excludeQuery.clear();
            excludeBody.clear();
            for (int i = 0; i < queryArray.size(); i++) {
                excludeQuery.add(queryArray.getString(i));
            }
            JsonArray bodyArray = jsonObject.getJsonArray("body.excludes", new JsonArray());
            for (int i = 0; i < bodyArray.size(); i++) {
                excludeBody.add(bodyArray.getString(i));
            }
            this.enabled = jsonObject.getBoolean("enable", false);
        }

    }

    private boolean testBodyArg(String argName, BodyArgPlugin plugin) {
        if ("".equalsIgnoreCase(argName)) {
            return false;
        }
        if (excludeBody.contains(argName)) {
            return false;
        }
        return plugin == null || plugin.parameter(argName) == null;
    }

    private boolean testUrlArg(String argName, UrlArgPlugin plugin) {
        if (excludeQuery.contains(argName)) {
            return false;
        }
        return plugin == null || plugin.parameter(argName) == null;
    }
}

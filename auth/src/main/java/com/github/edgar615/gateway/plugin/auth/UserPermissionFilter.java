package com.github.edgar615.gateway.plugin.auth;

import com.google.common.base.Splitter;

import com.github.edgar615.gateway.core.dispatch.ApiContext;
import com.github.edgar615.gateway.core.dispatch.Filter;
import com.github.edgar615.gateway.core.plugin.scope.ScopePlugin;
import com.github.edgar615.util.exception.DefaultErrorCode;
import com.github.edgar615.util.exception.SystemException;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * 权限校验的filter.
 * <p>
 * 如果接口包括Authentication插件，那么在AuthenticationFilter调用之后会在用户属性中存入<b>permissions</b>变量
 * 如果调用方或者用户没有对应的权限，直接返回1004的错误.
 * <p>
 * <p>
 * 该filter的order=1100
 */
public class UserPermissionFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserPermissionFilter.class);

    UserPermissionFilter(Vertx vertx, JsonObject config) {
    }

    @Override
    public String type() {
        return PRE;
    }

    @Override
    public int order() {
        return 11000;
    }

    @Override
    public boolean shouldFilter(ApiContext apiContext) {
        return apiContext.apiDefinition().plugin(ScopePlugin.class.getSimpleName()) != null
               && apiContext.principal() != null;
    }

    @Override
    public void doFilter(ApiContext apiContext, Future<ApiContext> completeFuture) {
        ScopePlugin plugin = (ScopePlugin) apiContext.apiDefinition()
                .plugin(ScopePlugin.class.getSimpleName());
        String appScope = plugin.scope();

        Set<String> permissions = new HashSet<>();
        Object userPermissions = apiContext.principal()
                .getString("permissions", "all");
        if (userPermissions instanceof String) {
            permissions.addAll(Splitter.on(",")
                                       .omitEmptyStrings().trimResults()
                                       .splitToList((String) userPermissions));
        }

        if (permissions.contains("all") || permissions.contains(appScope)) {
            log(apiContext.id(), "UserPermissionAdmitted");
            completeFuture.complete(apiContext);
        } else {
            SystemException ex = SystemException.create(DefaultErrorCode.PERMISSION_DENIED)
                    .set("details", "User does not have permission");
            failed(completeFuture, apiContext.id(), "UserPermissionDenied", ex);
        }

    }

}
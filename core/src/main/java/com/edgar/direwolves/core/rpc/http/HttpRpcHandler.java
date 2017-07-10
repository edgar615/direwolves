package com.edgar.direwolves.core.rpc.http;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import com.edgar.direwolves.core.definition.HttpEndpoint;
import com.edgar.direwolves.core.rpc.RpcHandler;
import com.edgar.direwolves.core.rpc.RpcMetric;
import com.edgar.direwolves.core.rpc.RpcRequest;
import com.edgar.direwolves.core.rpc.RpcResponse;
import com.edgar.direwolves.core.utils.Log;
import com.edgar.direwolves.core.utils.LogType;
import com.edgar.direwolves.core.utils.MultimapUtils;
import com.edgar.util.exception.DefaultErrorCode;
import com.edgar.util.exception.SystemException;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Edgar on 2016/12/30.
 *
 * @author Edgar  Date 2016/12/30
 */
public class HttpRpcHandler implements RpcHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpRpcHandler.class);

  private final HttpClient httpClient;

  private final RpcMetric metric;

  protected HttpRpcHandler(Vertx vertx, JsonObject config, RpcMetric metric) {
    this.metric = metric;
    this.httpClient = vertx.createHttpClient();
  }

  @Override
  public String type() {
    return HttpEndpoint.TYPE;
  }

  @Override
  public Future<RpcResponse> handle(RpcRequest rpcRequest) {
    HttpRpcRequest httpRpcRequest = (HttpRpcRequest) rpcRequest;
    if (checkMethod(httpRpcRequest)) {
      return Future.failedFuture(
              SystemException.create(DefaultErrorCode.INVALID_ARGS)
                      .set("details", "Method must be GET | POST | PUT | DELETE")
      );
    }
    if (checkBody(httpRpcRequest)) {
      return Future.failedFuture(
              SystemException.create(DefaultErrorCode.MISSING_ARGS)
                      .set("details", "POST or PUT method must contains request body")
      );
    }

    if (metric != null) {
      metric.request(httpRpcRequest.serverId());
    }

    Log.create(LOGGER)
            .setTraceId(httpRpcRequest.id())
            .setLogType(LogType.CS)
            .setEvent(type().toUpperCase())
            .addData("server",  httpRpcRequest.host() + ":" + httpRpcRequest.port())
            .setMessage("[{}] [{}] [{}] [{}]")
            .addArg( httpRpcRequest.method().name() + " " + httpRpcRequest.path())
            .addArg( MultimapUtils.convertToString(httpRpcRequest.headers(), "no header"))
            .addArg(    MultimapUtils.convertToString(httpRpcRequest.params(), "no param"))
            .addArg( httpRpcRequest.body() == null ? "no body" : httpRpcRequest.body().encode())
            .info();
    Future<RpcResponse> future = Future.future();
    String path = requestPath(httpRpcRequest);
    final long startTime = System.currentTimeMillis();
    HttpClientRequest request =
            httpClient
                    .request(httpRpcRequest.method(), httpRpcRequest.port(), httpRpcRequest.host(),
                             path)
                    .putHeader("content-type", "application/json");
    request.handler(response -> {
      response.bodyHandler(body -> {
        RpcResponse rpcResponse =
                RpcResponse.create(httpRpcRequest.id(),
                                   response.statusCode(),
                                   body,
                                   System.currentTimeMillis() - startTime);
        Log.create(LOGGER)
                .setTraceId(rpcRequest.id())
                .setLogType(LogType.CR)
                .setEvent("HTTP")
                .setMessage(" [{}] [{}ms] [{} bytes]")
                .addArg(rpcResponse.statusCode())
                .addArg(rpcResponse.elapsedTime())
                .addArg(body.getBytes().length)
                .info();
        if (metric != null) {
          metric.response(httpRpcRequest.serverId(), rpcResponse.statusCode(),
                          rpcResponse.elapsedTime());
        }
        future.complete(rpcResponse);
      }).exceptionHandler(throwable -> {
        if (!future.isComplete()) {
          Log.create(LOGGER)
                  .setTraceId(rpcRequest.id())
                  .setLogType(LogType.CR)
                  .setEvent("HTTP")
                 .setThrowable(throwable)
                  .error();
          future.fail(throwable);
        }
      });
    });
    header(httpRpcRequest, request);
    exceptionHandler(future, request, httpRpcRequest.serverId());
    timeout(httpRpcRequest, request);

    endRequest(httpRpcRequest, request);
    return future;
  }

  private boolean checkBody(HttpRpcRequest request) {
    return (request.method() == HttpMethod.POST
            || request.method() == HttpMethod.PUT)
           && request.body() == null;
  }

  private void header(HttpRpcRequest rpcRequest, HttpClientRequest request) {
    rpcRequest.headers().asMap().forEach((headerName, headerValues) -> {
      request.putHeader(headerName, headerValues);
    });
  }

  private void exceptionHandler(Future<RpcResponse> future, HttpClientRequest request,
                                String serverId) {
    request.exceptionHandler(throwable -> {
      if (!future.isComplete()) {
        if (metric != null) {
          metric.failed(serverId);
        }
        future.fail(throwable);
      }
    });
  }

  private void timeout(HttpRpcRequest rpcRequest, HttpClientRequest request) {
    if (rpcRequest.timeout() > 100) {
      request.setTimeout(rpcRequest.timeout());
    }
  }

  private void endRequest(HttpRpcRequest rpcRequest, HttpClientRequest request) {
    if (rpcRequest.method() == HttpMethod.GET) {
      request.end();
    } else if (rpcRequest.method() == HttpMethod.DELETE) {
      request.end();
    } else if (rpcRequest.method() == HttpMethod.POST) {
      request.setChunked(true)
              .end(rpcRequest.body().encode());
    } else if (rpcRequest.method() == HttpMethod.PUT) {
      request.setChunked(true)
              .end(rpcRequest.body().encode());
    }
  }

  private boolean checkMethod(HttpRpcRequest rpcRequest) {
    return rpcRequest.method() != HttpMethod.GET
           && rpcRequest.method() != HttpMethod.DELETE
           && rpcRequest.method() != HttpMethod.POST
           && rpcRequest.method() != HttpMethod.PUT;
  }

  private String requestPath(HttpRpcRequest rpcRequest) {
    List<String> query = new ArrayList<>(rpcRequest.params().size());
    for (String key : rpcRequest.params().keySet()) {
      String value = rpcRequest.params().get(key).iterator().next();
      if (value != null) {
        query.add(key + "=" + urlEncode(value));
      }
    }
    String queryString = Joiner.on("&").join(query);
    String path = rpcRequest.path();
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    if (!Strings.isNullOrEmpty(queryString)) {
      if (path.indexOf("?") > 0) {
        path += "&" + queryString;
      } else {
        path += "?" + queryString;
      }
    }
    return path;
  }

  public String urlEncode(String path) {
    try {
      return URLEncoder.encode(path, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return path;
    }
  }
}

package com.edgar.direwolves.plugin.authentication;

import com.edgar.direwolves.core.dispatch.ApiContext;
import com.edgar.direwolves.core.dispatch.Filter;
import com.edgar.util.exception.DefaultErrorCode;
import com.edgar.util.exception.SystemException;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTOptions;

import java.util.UUID;

/**
 * Created by edgar on 16-11-26.
 */
public class JwtCreateFilter implements Filter {
  private Vertx vertx;

  private String userAddAddress;

  private int expires = 1800;

  private String userKey = "userId";

  private JsonObject config = new JsonObject()
      .put("path", "keystore.jceks")
      .put("type", "jceks")//JKS, JCEKS, PKCS12, BKS，UBER
      .put("password", "secret")
      .put("algorithm", "HS512")
      .put("expiresInSeconds", 1800);

  @Override
  public String type() {
    return POST;
  }

  @Override
  public int order() {
    return 1000;
  }

  @Override
  public boolean shouldFilter(ApiContext apiContext) {
    if (apiContext.apiDefinition() == null) {
      return false;
    }
    return apiContext.apiDefinition()
        .plugin(JwtCreatePlugin.class.getSimpleName()) != null;
  }

  @Override
  public void doFilter(ApiContext apiContext, Future<ApiContext> completeFuture) {
    JsonObject jwtConfig = new JsonObject().put("keyStore", config);
    JWTAuth provider = JWTAuth.create(vertx, jwtConfig);
    JsonObject response = apiContext.response();
    if (response.getBoolean("isArray", true)) {
      JsonObject body = response.getJsonObject("body");
      String jti = UUID.randomUUID().toString();
      JsonObject claims = new JsonObject()
          .put("jti", jti)
          .put(userKey, body.getInteger(userKey));
      JsonObject user = body.copy().put("jti", jti);
      vertx.eventBus().send(userAddAddress, user, ar -> {
        if (ar.succeeded()) {
          String token = provider.generateToken(claims, new JWTOptions(config));
          body.put("token", token);
          apiContext.setResponse(response.put("body", body));
          completeFuture.complete(apiContext);
        } else {
          completeFuture.fail(ar.cause());
        }
      });
    } else {
      completeFuture.fail(SystemException.create(DefaultErrorCode.INVALID_JSON));
    }
  }

  /**
   * 配置.
   * <pre>
   *     - keystore.path string 证书文件路径 默认值keystore.jceks
   *     - keystore.type string 证书类型，可选值 jceks, jks,默认值jceks
   *     - keystore.password string 证书密钥，默认值secret
   *     - jwt.alg string jwt的加密算法,默认值HS512
   *     - jwt.audience string token的客户aud
   *     - jwt.issuer string token的发行者iss
   *     - jwt.subject string token的主题sub
   *     - jwt.expires int 过期时间exp，单位秒，默认值1800
   * </pre>
   *
   * @param vertx  Vertx
   * @param config 配置
   */
  @Override
  public void config(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    if (config.containsKey("keystore.path")) {
      this.config.put("path", config.getString("keystore.path"));
    }
    if (config.containsKey("keystore.type")) {
      this.config.put("type", config.getString("keystore.type"));
    }
    if (config.containsKey("keystore.password")) {
      this.config.put("password", config.getString("keystore.password"));
    }
    if (config.containsKey("jwt.alg")) {
      this.config.put("algorithm", config.getString("jwt.alg"));
    }
    if (config.containsKey("jwt.audience")) {
      this.config.put("audience", config.getString("jwt.audience"));
    }
    if (config.containsKey("jwt.issuer")) {
      this.config.put("issuer", config.getString("jwt.issuer"));
    }
    if (config.containsKey("jwt.subject")) {
      this.config.put("subject", config.getString("jwt.subject"));
    }
    if (config.containsKey("jwt.expires")) {
      this.config.put("expiresInSeconds", config.getInteger("jwt.expires"));
    }
    this.userAddAddress = config.getString("jwt.user.add.address", "eventbus.jwt.user.add");
    this.expires = config.getInteger("jwt.expires", 1800);
    this.userKey = config.getString("jwt.user.key", "userId");

  }
}
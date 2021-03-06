package com.github.edgar615.servicediscovery.verticle;

import com.github.edgar615.servicediscovery.zookeeper.ZookeeperServiceImporter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.spi.ServiceImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 从zookeeper中读取服务节点.
 *
 * @author Edgar  Date 2017/6/9
 */
public class ZookeeperServiceDiscoveryVerticle extends AbstractVerticle {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ZookeeperServiceDiscoveryVerticle.class);

    @Override
    public void start() throws Exception {
        LOGGER.info("[Verticle] [start] start {}",
                    ZookeeperServiceDiscoveryVerticle.class.getSimpleName());
        ServiceDiscoveryOptions options;
        if (config().getValue("service.discovery") instanceof JsonObject) {
            options = new ServiceDiscoveryOptions(config().getJsonObject("service.discovery"));
        } else {
            options = new ServiceDiscoveryOptions();
        }
        ServiceDiscovery discovery = ServiceDiscovery.create(vertx, options);

        if (config().getValue("zookeeper") instanceof JsonObject) {
            registerZookeeper(discovery, config().getJsonObject("zookeeper"));
        }

    }

    private void registerZookeeper(ServiceDiscovery discovery, JsonObject config) {
        try {
            ServiceImporter serviceImporter = new ZookeeperServiceImporter();
            discovery
                    .registerServiceImporter(serviceImporter, config,
                                             Future.<Void>future().completer());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
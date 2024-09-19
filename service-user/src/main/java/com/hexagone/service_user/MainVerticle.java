package com.hexagone.service_user;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Promise<Void> startPromise) {
        logger.info("Starting MainVerticle");
        vertx.deployVerticle(new UserVerticle(), ar -> {
            if (ar.succeeded()) {
                logger.info("UserVerticle deployed successfully");
                startPromise.complete();
            } else {
                logger.error("Failed to deploy UserVerticle", ar.cause());
                startPromise.fail(ar.cause());
            }
        });
    }
}
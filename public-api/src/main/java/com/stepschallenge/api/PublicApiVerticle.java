package com.stepschallenge.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.Router;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PublicApiVerticle extends AbstractVerticle {
    private static final Logger logger = LogManager.getLogger(PublicApiVerticle.class);

    @Override
    public void start(Promise<Void> startPromise) {
        Router router = Router.router(vertx);

        BodyHandler bodyHandler = BodyHandler.create();
        router.post().handler(bodyHandler);
        router.put().handler(bodyHandler);

        String prefix = "/api/v1";

        router.post(prefix + "/register").handler(this::register);
        router.post(prefix + "/token").handler(this::register);

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080, http -> {
                if (http.succeeded()) {
                    logger.info("HTTP server started on port 8080");
                    startPromise.complete();
                } else {
                    logger.error("Failed to launch HTTP server", http.cause());
                    startPromise.fail(http.cause());
                }
            });
    }

    private void register(RoutingContext routingContext) {

    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        
        vertx.deployVerticle(new PublicApiVerticle(), res -> {
            if (res.succeeded()) {
                System.out.println("PublicApiVerticle deployed successfully");
            } else {
                System.err.println("Failed to deploy PublicApiVerticle: " + res.cause());
            }
        });
    }
} 
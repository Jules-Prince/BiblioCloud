package com.hexagone.service_user;

import java.util.List;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(UserVerticle.class);
    private UserService userService;

    @Override
    public void start(Promise<Void> startPromise) {
        logger.info("Starting UserVerticle");

        ConfigStoreOptions fileStore = new ConfigStoreOptions()
            .setType("file")
            .setFormat("properties")
            .setConfig(new JsonObject().put("path", "application.conf"));

        ConfigStoreOptions envStore = new ConfigStoreOptions()
            .setType("env");

        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
            .addStore(fileStore)
            .addStore(envStore);

        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

        retriever.getConfig(ar -> {
            if (ar.succeeded()) {
                JsonObject config = ar.result();
                logger.info("Configuration loaded successfully");
                JsonObject substitutedConfig = substituteEnvVariables(config);
                setupService(substitutedConfig, startPromise);
            } else {
                logger.error("Failed to load configuration", ar.cause());
                startPromise.fail("Failed to load configuration");
            }
        });
    }

    private JsonObject substituteEnvVariables(JsonObject config) {
        JsonObject result = new JsonObject();
        for (String key : config.fieldNames()) {
            String value = config.getString(key);
            if (value.startsWith("${") && value.endsWith("}")) {
                String envVar = value.substring(2, value.length() - 1);
                String envValue = System.getenv(envVar);
                result.put(key, envValue != null ? envValue : value);
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    private void setupService(JsonObject config, Promise<Void> startPromise) {
       logger.info(config.toString());
        userService = new UserService(vertx, config);

        int httpPort = Integer.parseInt(config.getString("http.port", "8888"));
        setupHttpServer(httpPort, startPromise);
    }

    private void setupHttpServer(int port, Promise<Void> startPromise) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.post("/users").handler(this::createUser);
        router.get("/users/:id").handler(this::getUser);
        router.get("/users").handler(this::getAllUsers);
        router.put("/users/:id").handler(this::updateUser);

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(port, http -> {
                if (http.succeeded()) {
                    logger.info("HTTP server started on port " + port);
                    startPromise.complete();
                } else {
                    logger.error("Failed to start HTTP server", http.cause());
                    startPromise.fail(http.cause());
                }
            });
    }

    private void createUser(RoutingContext routingContext) {
        JsonObject json = routingContext.body().asJsonObject();
        if (json == null) {
            logger.warn("Invalid JSON body received");
            routingContext.response().setStatusCode(400).end("Invalid JSON body");
            return;
        }

        String name = json.getString("name");
        String email = json.getString("email");

        if (name == null || email == null) {
            logger.warn("Missing required fields: name or email");
            routingContext.response().setStatusCode(400).end("Name and email are required");
            return;
        }

        logger.info("Creating user: name={}, email={}", name, email);
        userService.createUser(name, email).onComplete(ar -> {
            if (ar.succeeded()) {
                User createdUser = ar.result();
                logger.info("User created successfully: id={}", createdUser.getId());
                routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(createdUser.toJson().encode());
            } else {
                logger.error("Failed to create user", ar.cause());
                routingContext.response().setStatusCode(500).end(ar.cause().getMessage());
            }
        });
    }

    private void getUser(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        logger.info("Received request to get user with id: {}", id);

        userService.getUser(id).onComplete(ar -> {
            if (ar.succeeded()) {
                logger.info("Successfully retrieved user with id: {}", id);
                routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodePrettily(ar.result()));
            } else {
                logger.error("Failed to retrieve user with id: {}. Error: {}", id, ar.cause().getMessage());
                routingContext.response().setStatusCode(404).end(ar.cause().getMessage());
            }
        });
    }

    private void getAllUsers(RoutingContext routingContext) {
        logger.info("Received request to get all users");

        userService.getAllUsers().onComplete(ar -> {
            if (ar.succeeded()) {
                List<User> users = ar.result();
                logger.info("Successfully retrieved {} users", users.size());
                routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodePrettily(users));
            } else {
                logger.error("Failed to retrieve all users. Error: {}", ar.cause().getMessage());
                routingContext.response().setStatusCode(500).end(ar.cause().getMessage());
            }
        });
    }

    private void updateUser(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        logger.info("Received request to update user with id: {}", id);

        JsonObject json = routingContext.body().asJsonObject();
        if (json == null) {
            logger.warn("Invalid JSON body received for updating user with id: {}", id);
            routingContext.response().setStatusCode(400).end("Invalid JSON body");
            return;
        }

        String name = json.getString("name");
        String email = json.getString("email");
        if (name == null || email == null) {
            logger.warn("Missing required fields (name or email) for updating user with id: {}", id);
            routingContext.response().setStatusCode(400).end("Name and email are required");
            return;
        }

        logger.info("Updating user with id: {}, new name: {}, new email: {}", id, name, email);
        userService.updateUser(id, name, email).onComplete(ar -> {
            if (ar.succeeded()) {
                logger.info("Successfully updated user with id: {}", id);
                routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodePrettily(ar.result()));
            } else {
                logger.error("Failed to update user with id: {}. Error: {}", id, ar.cause().getMessage());
                routingContext.response().setStatusCode(404).end(ar.cause().getMessage());
            }
        });
    }
}
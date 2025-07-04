package com.stepschallenge.userprofile;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.mongo.MongoAuthentication;
import io.vertx.ext.auth.mongo.MongoAuthenticationOptions;
import io.vertx.ext.auth.mongo.MongoUserUtil;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Pattern;

public class UserProfileApiVerticle extends AbstractVerticle {
    private static final int HTTP_PORT = 3000;
    private static final Logger logger = LogManager.getLogger(UserProfileApiVerticle.class);
    private MongoClient mongoClient;
    private MongoAuthentication mongoAuth;
    private final Pattern validUsername = Pattern.compile("\\w[\\w+|-]*");
    private final Pattern validDeviceId = Pattern.compile("\\w[\\w+|-]*");
    // Email regexp from https://www.owasp.org/index.php/OWASP_Validation_Regex_Repository
    private final Pattern validEmail = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    private JsonObject mongoConfig() {
        return new JsonObject()
                .put("host", "localhost")
                .put("port", 27017)
                .put("db_name", "profiles");
    }

    @Override
    public void start(Promise<Void> startPromise) {
        logger.info("Starting UserProfileApiVerticle on port {}", HTTP_PORT);
        mongoClient = MongoClient.createShared(vertx, mongoConfig());
        mongoAuth = MongoAuthentication.create(mongoClient, new MongoAuthenticationOptions());

        Router router = Router.router(vertx);
        BodyHandler bodyHandler = BodyHandler.create();
        router.post().handler(bodyHandler);
        router.put().handler(bodyHandler);

        router.post("/register")
                .handler(this::validateRegistration)
                .handler(this::register);
        router.get("/:username").handler(this::fetchUser);
        router.put("/:username").handler(this::updateUser);
        router.post("/authenticate").handler(this::authenticate);
        router.get("/owns/:deviceId").handler(this::whoOwns);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(HTTP_PORT, http -> {
                    if (http.succeeded()) {
                        logger.info("HTTP server started on port {}", HTTP_PORT);
                        startPromise.complete();
                    } else {
                        logger.error("Failed to start HTTP server", http.cause());
                        startPromise.fail(http.cause());
                    }
                });
    }

    private boolean anyRegistrationFieldIsMissing(JsonObject body) {
        return !(body.containsKey("username") &&
                body.containsKey("password") &&
                body.containsKey("email") &&
                body.containsKey("city") &&
                body.containsKey("deviceId") &&
                body.containsKey("makePublic"));
    }

    private boolean anyRegistrationFieldIsWrong(JsonObject body) {
        return !validUsername.matcher(body.getString("username")).matches() ||
                !validEmail.matcher(body.getString("email")).matches() ||
                body.getString("password").trim().isEmpty() ||
                !validDeviceId.matcher(body.getString("deviceId")).matches();
    }

    private void validateRegistration(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null || anyRegistrationFieldIsMissing(body)) {
            ctx.response().setStatusCode(400).end("Missing required registration fields");
            return;
        }
        if (anyRegistrationFieldIsWrong(body)) {
            ctx.response().setStatusCode(400).end("Invalid registration field values");
            return;
        }
        ctx.next();
    }

    private void register(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String username = body.getString("username");
        logger.info("Registering user: {}", username);
        String password = body.getString("password");
        String email = body.getString("email");
        String city = body.getString("city");
        String deviceId = body.getString("deviceId");
        boolean makePublic = body.getBoolean("makePublic");

        MongoUserUtil userUtil = MongoUserUtil.create(mongoClient);
        userUtil.createUser(username, password)
                .onSuccess(id -> {
                    // Add extra fields to the user document
                    JsonObject update = new JsonObject()
                            .put("$set", new JsonObject()
                                    .put("email", email)
                                    .put("city", city)
                                    .put("deviceId", deviceId)
                                    .put("makePublic", makePublic)
                            );
                    JsonObject query = new JsonObject().put("_id", id);
                    mongoClient.updateCollection("user", query, update, updateRes -> {
                        if (updateRes.succeeded()) {
                            logger.info("User {} registered successfully", username);
                            ctx.response().setStatusCode(201).end("User registered");
                        } else {
                            logger.error("Failed to update user {}: {}", username, updateRes.cause());
                            ctx.response().setStatusCode(500).end("Failed to update user");
                        }
                    });
                })
                .onFailure(err -> {
                    logger.error("Failed to register user {}: {}", username, err);
                    ctx.response().setStatusCode(500).end("Failed to register user");
                });
    }

    private void fetchUser(RoutingContext ctx) {
        String username = ctx.pathParam("username");
        logger.info("Fetching user: {}", username);
        if (username == null || !validUsername.matcher(username).matches()) {
            logger.warn("Invalid username provided: {}", username);
            ctx.response().setStatusCode(400).end("Invalid username");
            return;
        }
        JsonObject query = new JsonObject().put("username", username);
        mongoClient.findOne("user", query, null, res -> {
            if (res.succeeded()) {
                JsonObject user = res.result();
                if (user == null) {
                    logger.info("User not found: {}", username);
                    ctx.response().setStatusCode(404).end("User not found");
                } else {
                    user.remove("passwordHash");
                    user.remove("_id");
                    logger.info("User {} fetched successfully", username);
                    ctx.response().putHeader("Content-Type", "application/json").end(user.encode());
                }
            } else {
                logger.error("Failed to fetch user {}: {}", username, res.cause());
                ctx.response().setStatusCode(500).end("Failed to fetch user");
            }
        });
    }

    private void updateUser(RoutingContext ctx) {
        String username = ctx.pathParam("username");
        logger.info("Updating user: {}", username);
        if (username == null || !validUsername.matcher(username).matches()) {
            logger.warn("Invalid username provided for update: {}", username);
            ctx.response().setStatusCode(400).end("Invalid username");
            return;
        }
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            logger.warn("Missing request body for update of user: {}", username);
            ctx.response().setStatusCode(400).end("Missing request body");
            return;
        }
        JsonObject updateFields = new JsonObject();
        if (body.containsKey("email") && validEmail.matcher(body.getString("email")).matches()) {
            updateFields.put("email", body.getString("email"));
        }
        if (body.containsKey("city")) {
            updateFields.put("city", body.getString("city"));
        }
        if (body.containsKey("deviceId") && validDeviceId.matcher(body.getString("deviceId")).matches()) {
            updateFields.put("deviceId", body.getString("deviceId"));
        }
        if (body.containsKey("makePublic")) {
            updateFields.put("makePublic", body.getBoolean("makePublic"));
        }
        if (updateFields.isEmpty()) {
            logger.warn("No valid fields to update for user: {}", username);
            ctx.response().setStatusCode(400).end("No valid fields to update");
            return;
        }
        JsonObject query = new JsonObject().put("username", username);
        JsonObject update = new JsonObject().put("$set", updateFields);
        mongoClient.updateCollection("user", query, update, res -> {
            if (res.succeeded()) {
                if (res.result().getDocMatched() == 0) {
                    logger.info("User not found for update: {}", username);
                    ctx.response().setStatusCode(404).end("User not found");
                } else {
                    logger.info("User {} updated successfully", username);
                    ctx.response().setStatusCode(200).end("User updated");
                }
            } else {
                logger.error("Failed to update user {}: {}", username, res.cause());
                ctx.response().setStatusCode(500).end("Failed to update user");
            }
        });
    }

    private void authenticate(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String username = body != null ? body.getString("username") : null;
        logger.info("Authenticating user: {}", username);
        if (body == null || !body.containsKey("username") || !body.containsKey("password")) {
            logger.warn("Missing username or password for authentication");
            ctx.response().setStatusCode(400).end("Missing username or password");
            return;
        }
        String password = body.getString("password");
        JsonObject authInfo = new JsonObject().put("username", username).put("password", password);
        mongoAuth.authenticate(authInfo)
            .onSuccess(user -> {
                logger.info("User {} authenticated successfully", username);
                ctx.response().setStatusCode(200).end("Authenticated");
            })
            .onFailure(err -> {
                logger.warn("Authentication failed for user {}: {}", username, err.getMessage());
                ctx.response().setStatusCode(401).end("Invalid credentials");
            });
    }

    private void whoOwns(RoutingContext ctx) {
        String deviceId = ctx.pathParam("deviceId");
        logger.info("Looking up owner for deviceId: {}", deviceId);
        if (deviceId == null || !validDeviceId.matcher(deviceId).matches()) {
            logger.warn("Invalid deviceId provided: {}", deviceId);
            ctx.response().setStatusCode(400).end("Invalid deviceId");
            return;
        }
        JsonObject query = new JsonObject().put("deviceId", deviceId);
        mongoClient.findOne("user", query, null, res -> {
            if (res.succeeded()) {
                JsonObject user = res.result();
                if (user == null) {
                    logger.info("No owner found for deviceId: {}", deviceId);
                    ctx.response().setStatusCode(404).end("Device not owned");
                } else {
                    logger.info("DeviceId {} is owned by user {}", deviceId, user.getString("username"));
                    ctx.response().putHeader("Content-Type", "application/json").end(user.getString("username"));
                }
            } else {
                logger.error("Failed to lookup device owner for deviceId {}: {}", deviceId, res.cause());
                ctx.response().setStatusCode(500).end("Failed to lookup device owner");
            }
        });
    }

    public static void main(String[] args) {
        io.vertx.core.Vertx vertx = io.vertx.core.Vertx.vertx();
        vertx.deployVerticle(new UserProfileApiVerticle(), res -> {
            if (res.succeeded()) {
                logger.info("UserProfileApiVerticle deployed successfully");
            } else {
                logger.error("Failed to deploy UserProfileApiVerticle", res.cause());
            }
        });
    }
} 
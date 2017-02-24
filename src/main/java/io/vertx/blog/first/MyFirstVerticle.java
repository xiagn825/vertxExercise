package io.vertx.blog.first;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by xiaguannan on 2017/2/22.
 */
public class MyFirstVerticle extends AbstractVerticle {
  @Override
  public void start(Future<Void> fut) {
    System.out.println("app start");
    createSomeData();

    Router router = Router.router(vertx);

    router.route("/").handler(routingContext ->{
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/html")
          .end("<h1>Hello from my first Vert.x 3 application<h1>");
    });

    router.get("/api/whiskies").handler(this::getAll);
    router.get("/api/whiskies/:id").handler(this::getOne);
    router.route("/api/whiskies*").handler(BodyHandler.create());
    router.post("/api/whiskies").handler(this::addOne);
    router.put("/api/whiskies/:id").handler(this::updateOne);
    router.delete("/api/whiskies/:id").handler(this::deleteOne);

    // Serve static resources from the /assets directory
    router.route("/assets/*").handler(StaticHandler.create("assets"));

    vertx.createHttpServer()
        .requestHandler(router::accept)
        .listen(config().getInteger("http.port", 8080),result->{
          if (result.succeeded()) {
            fut.complete();
          } else {
            fut.fail(result.cause());
          }
        });
  }

  // Store our product
  private Map<Integer, Whisky> products = new LinkedHashMap<>();
  // Create some product
  private void createSomeData() {
    Whisky bowmore = new Whisky("Bowmore 15 Years Laimrig", "Scotland, Islay");
    products.put(bowmore.getId(), bowmore);
    Whisky talisker = new Whisky("Talisker 57° North", "Scotland, Island");
    products.put(talisker.getId(), talisker);
  }

  private void getAll(RoutingContext routingContext) {
    System.out.printf("getALL whisky:%s", Json.encodePrettily(products.values()));
    routingContext.response()
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(products.values()));
  }

  private void addOne(RoutingContext routingContext) {
    final Whisky whisky = Json.decodeValue(routingContext.getBodyAsString(),
      Whisky.class);
    System.out.printf("add whisky:%s", whisky.getId());
    products.put(whisky.getId(), whisky);
    routingContext.response()
      .setStatusCode(201)
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(whisky));
  }

  private void deleteOne(RoutingContext routingContext) {
    String id = routingContext.request().getParam("id");
    if (id == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      products.remove(Integer.valueOf(id));
      routingContext.response().setStatusCode(204).end();
    }
  }

  private void getOne(RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    if (id == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      final Integer idAsInteger = Integer.valueOf(id);
      Whisky whisky = products.get(idAsInteger);
      if (whisky == null) {
        routingContext.response().setStatusCode(404).end();
      } else {
        routingContext.response()
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(whisky));
      }
    }
  }

  private void updateOne(RoutingContext routingContext) {
    String id = routingContext.request().getParam("id");
    JsonObject json = routingContext.getBodyAsJson();
    if (id == null || !products.containsKey(Integer.valueOf(id)) || json == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      final Whisky whisky = products.get(Integer.valueOf(id));
      whisky.setName(json.getString("name"));
      whisky.setOrigin(json.getString("origin"));
      routingContext.response()
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(whisky));
    }
  }
}



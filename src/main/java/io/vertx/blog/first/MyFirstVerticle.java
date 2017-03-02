package io.vertx.blog.first;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by xiaguannan on 2017/2/22.
 */
public class MyFirstVerticle extends AbstractVerticle {
  private JDBCClient jdbc;

  private static final String CREATE_TABLE_WHISKY = "CREATE TABLE IF NOT EXISTS Whisky (id INTEGER IDENTITY, name varchar(100), origin varchar(100))";

  // Store our product
  private Map<Integer, Whisky> products = new LinkedHashMap<>();

  public static void main(String[] args) {
    Runner.runExample(MyFirstVerticle.class);
  }

  @Override
  public void start(Future<Void> fut) {
    System.out.println("app start");
    JsonObject config = new JsonObject();
    config.put("port", "8080");
    config.put("url", "jdbc:hsqldb:mem:test?shutdown=true");
    config.put("driver_class", "org.hsqldb.jdbcDriver");
    jdbc = JDBCClient.createShared(vertx, config, "My-Whisky-Collection");
    startBackend(
      (connection) -> createSomeData(connection,
        (nothing) -> startWebApp(
          (http) -> completeStartup(http, fut)
        ), fut
      ), fut);
  }

  private void startWebApp(Handler<AsyncResult<HttpServer>> listenHandler) {
    Router router = Router.router(vertx);

    router.route("/").handler(routingContext ->{
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/html")
          .end("<h1>Hello from my first Vert.x 3 application<h1>");
    });

    router.get("/api/whiskies").handler(this::getAllInDB);
    router.get("/api/whiskies/:id").handler(this::getOne);
    router.route("/api/whiskies*").handler(BodyHandler.create());
    router.post("/api/whiskies").handler(this::addOne);
    router.put("/api/whiskies/:id").handler(this::updateOne);
    router.delete("/api/whiskies/:id").handler(this::deleteOne);

    // Serve static resources from the /assets directory
    router.route("/assets/*").handler(StaticHandler.create("assets"));

    vertx.createHttpServer()
        .requestHandler(router::accept)
        .listen(config().getInteger("http.port", 8085), listenHandler);
  }

  private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {

      if (http.succeeded()) {
        fut.complete();
      } else {
        System.out.println("completeStartup"+http.cause());
        fut.fail(http.cause());
      }
  }

  private void startBackend(Handler<AsyncResult<SQLConnection>> next, Future<Void> fut) {
    System.out.println("startBackend");
    jdbc.getConnection(ar->{
      if (ar.failed()) {
        System.out.println("startBackend" + ar.cause());
        fut.fail(ar.cause());
      } else {
        next.handle(Future.succeededFuture(ar.result()));
      }
    });
  }

  private void createSomeData(AsyncResult<SQLConnection> result, Handler<Future<Void>> next, Future<Void> fut) {
    System.out.println("createSomeData");
    if (result.failed()) {
      System.out.println("createSomeData" + result.cause());
      fut.fail(result.cause());
    } else {
      SQLConnection connection = result.result();
      connection.execute(CREATE_TABLE_WHISKY, ar-> {
        if (ar.failed()) {
          fut.fail(ar.cause());
          connection.close();
          return;
        }
        connection.query("SELECT * FROM Whisky", select -> {
          if (select.failed()) {
            fut.fail(ar.cause());
            connection.close();
            return;
          }
          System.out.printf("select.result(): %s\n", select.result().toJson()) ;
          if (select.result().getNumRows() == 0) {
            insert(
              new Whisky("Bowmore 15 Years Laimrig", "Scotland, Islay"),
              connection,
              (v) -> insert(new Whisky("Talisker 57° North", "Scotland, Island"),
                connection,
                (r) -> {
                  next.handle(Future.<Void>succeededFuture());
                  connection.close();
                }));
          } else {
            next.handle(Future.<Void>succeededFuture());
            connection.close();
          }
        });
      });
    }
  }

  private void insert(Whisky whisky, SQLConnection connection, Handler<AsyncResult<Whisky>> next) {
    System.out.println("insert");
    String sql = "INSERT INTO Whisky (name, origin) VALUES ?, ?";
    connection.updateWithParams(sql,
      new JsonArray().add(whisky.getName()).add(whisky.getOrigin()),
      (ar) -> {
        if (ar.failed()) {
          next.handle(Future.failedFuture(ar.cause()));
          return;
        }
        UpdateResult result = ar.result();
        // Build a new whisky instance with the generated id.
        Whisky w = new Whisky(result.getKeys().getInteger(0), whisky.getName(), whisky.getOrigin());
        next.handle(Future.succeededFuture(w));
      });
  }

  private void getAllInDB(RoutingContext routingContext) {
    jdbc.getConnection(ar -> {
      SQLConnection connection = ar.result();
      connection.query("SELECT * FROM Whisky", result -> {
        List<Whisky> whiskies = result.result().getRows().stream().map(Whisky::new).collect(Collectors.toList());
        routingContext.response()
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(whiskies));
        connection.close(); // Close the connection
      });
    });
  }



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



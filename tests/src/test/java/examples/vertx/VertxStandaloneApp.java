/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

public class VertxStandaloneApp {
  public static void main(String[] args) {
    // Create a Vert.x instance
    Vertx vertx = Vertx.vertx();

    // Create the HTTP server
    HttpServer server = vertx.createHttpServer();

    // Attach a single request handler for all incoming requests
    server.requestHandler(
        request -> {
          var response = request.response();
          response.putHeader("content-type", "text/plain");

          // Check the request path to determine the response
          if (request.path().equals("/hello")) {
            response.end("Hello from Vert.x core!");
          } else if (request.path().equals("/about")) {
            response.end("This is a simple server using only vertx-core.");
          } else {
            response.setStatusCode(404);
            response.end("Not Found");
          }
        });

    // Start the server and listen on port 8080
    server
        .listen(8080)
        .onSuccess(s -> System.out.println("HTTP server started on port " + s.actualPort()))
        .onFailure(t -> t.printStackTrace());
  }
}

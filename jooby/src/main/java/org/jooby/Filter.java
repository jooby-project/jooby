package org.jooby;

public interface Filter {

  void handle(Request req, Response res, Route.Chain chain) throws Exception;

}

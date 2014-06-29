package jooby;

public interface Route {

  void handle(Request request, Response response) throws Exception;

}

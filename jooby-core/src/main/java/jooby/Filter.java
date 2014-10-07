package jooby;

public interface Filter {

  void handle(Request request, Response response, Route.Chain chain) throws Exception;

}

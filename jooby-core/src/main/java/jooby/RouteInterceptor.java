package jooby;

public interface RouteInterceptor {

  void before(Request request, Response response) throws Exception;

  void beforeSend(Request request, Response response) throws Exception;

  void after(Request request, Response response) throws Exception;

  void after(Request request, Response response, Exception ex) throws Exception;
}

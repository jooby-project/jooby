package jooby;

import java.util.Set;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public interface RouteInterceptor {

  Key<Set<RouteInterceptor>> KEY = Key.get(new TypeLiteral<Set<RouteInterceptor>>() {});

  void before(Request request, Response response) throws Exception;

  void beforeSend(Request request, Response response) throws Exception;

  void after(Request request, Response response) throws Exception;

  void after(Request request, Response response, Exception ex) throws Exception;
}

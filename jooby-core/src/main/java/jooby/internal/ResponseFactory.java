package jooby.internal;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

import jooby.MediaType;
import jooby.Request;
import jooby.Response;
import jooby.RouteInterceptor;

import com.google.common.collect.ListMultimap;

public interface ResponseFactory {

  Response newResponse(Request request,
      BodyConverterSelector selector,
      Set<RouteInterceptor> interceptors,
      Charset charset,
      List<MediaType> produces,
      ListMultimap<String, String> headers);

}

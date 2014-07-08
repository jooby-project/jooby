package jooby;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

public interface ResponseFactory {

  Response newResponse(Request request, BodyConverterSelector selector,
      Set<RouteInterceptor> interceptors, Charset charset, List<MediaType> produces);

}

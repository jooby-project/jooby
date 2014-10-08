package jooby.internal;

import java.nio.charset.Charset;
import java.util.List;

import jooby.MediaTypeProvider;
import jooby.MediaType;
import jooby.Response;

import com.google.inject.Injector;

public interface ResponseFactory {

  Response newResponse(Injector injector,
      BodyConverterSelector selector,
      MediaTypeProvider typeProvider,
      Charset charset,
      List<MediaType> produces);

}

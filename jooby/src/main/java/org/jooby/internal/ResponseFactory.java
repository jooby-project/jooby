package org.jooby.internal;

import java.nio.charset.Charset;
import java.util.List;

import org.jooby.MediaType;
import org.jooby.MediaTypeProvider;
import org.jooby.Response;

import com.google.inject.Injector;

public interface ResponseFactory {

  Response newResponse(Injector injector,
      BodyConverterSelector selector,
      MediaTypeProvider typeProvider,
      Charset charset,
      List<MediaType> produces);

}

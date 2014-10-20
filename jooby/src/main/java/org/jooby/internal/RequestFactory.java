package org.jooby.internal;

import java.nio.charset.Charset;
import java.util.List;

import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Route;

import com.google.inject.Injector;

public interface RequestFactory {

  Request newRequest(Injector injector,
      Route route,
      BodyConverterSelector selector,
      Charset charset,
      MediaType contentType,
      List<MediaType> accept);

}

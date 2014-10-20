package jooby.internal;

import java.nio.charset.Charset;
import java.util.List;

import jooby.MediaType;
import jooby.Request;
import jooby.Route;

import com.google.inject.Injector;

public interface RequestFactory {

  Request newRequest(Injector injector,
      Route route,
      BodyConverterSelector selector,
      Charset charset,
      MediaType contentType,
      List<MediaType> accept);

}

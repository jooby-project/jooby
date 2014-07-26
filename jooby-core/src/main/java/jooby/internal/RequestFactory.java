package jooby.internal;

import java.nio.charset.Charset;
import java.util.List;

import jooby.MediaType;
import jooby.Request;
import jooby.RouteMatcher;

import com.google.inject.Injector;

public interface RequestFactory {

  Request newRequest(Injector injector,
      RouteMatcher routeMatcher,
      BodyConverterSelector selector,
      MediaType contentType,
      List<MediaType> accept,
      Charset defaultCharset);

}

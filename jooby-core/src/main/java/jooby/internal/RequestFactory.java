package jooby.internal;

import java.nio.charset.Charset;
import java.util.List;

import jooby.MediaType;
import jooby.Request;

import com.google.common.collect.ListMultimap;
import com.google.inject.Injector;

public interface RequestFactory {

  Request newRequest(Injector injector,
      String path,
      BodyConverterSelector selector,
      MediaType contentType,
      List<MediaType> accept,
      ListMultimap<String, String> params,
      Charset defaultCharset);

}

package jooby;

import java.nio.charset.Charset;
import java.util.List;

import com.google.common.collect.ListMultimap;
import com.google.inject.Injector;

public interface RequestFactory {

  Request newRequest(Injector injector, BodyConverterSelector selector,
      List<MediaType> accept, MediaType contentType, ListMultimap<String, String> params,
      ListMultimap<String, String> headers,
      Charset defaultCharset);

}

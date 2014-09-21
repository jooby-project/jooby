package jooby.internal;

import java.nio.charset.Charset;
import java.util.List;

import jooby.MediaType;
import jooby.Response;

import com.google.common.collect.ListMultimap;

public interface ResponseFactory {

  Response newResponse(BodyConverterSelector selector,
      Charset charset,
      List<MediaType> produces,
      ListMultimap<String, String> headers);

}

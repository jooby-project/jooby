package jooby;

import java.nio.charset.Charset;
import java.util.List;

public interface ResponseFactory {

  Response newResponse(BodyConverterSelector selector, Charset charset,
      List<MediaType> produces);

}

package jooby;

import java.nio.charset.Charset;
import java.util.List;

public interface ResponseFactory {

  Response newResponse(BodyMapperSelector selector, Charset charset,
      List<MediaType> produces);

}

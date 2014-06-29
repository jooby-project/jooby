package jooby;

import java.util.List;

public interface ResponseFactory {

  Response newResponse(MessageConverterSelector selector, List<MediaType> accept,
      List<MediaType> produces);

}

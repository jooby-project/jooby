package jooby;

import java.nio.charset.Charset;

public interface Response {

  interface ContentNegotiation {

    interface Fn {
      Object apply() throws Exception;
    }

    default ContentNegotiation when(final String mediaType, final Fn fn) {
      return when(MediaType.valueOf(mediaType), fn);
    }

    ContentNegotiation when(MediaType mediaType, Fn fn);

    void send() throws Exception;
  }

  HttpMutableField header(String name);

  Charset charset();

  Response charset(Charset charset);

  MediaType type();

  Response type(MediaType type);

  void send(Object message) throws Exception;

  void send(Object message, BodyConverter converter) throws Exception;

  ContentNegotiation when(String type, ContentNegotiation.Fn fn);

  HttpStatus status();

  Response status(HttpStatus status);

  void reset();

}

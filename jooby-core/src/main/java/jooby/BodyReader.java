package jooby;

import java.io.InputStream;
import java.io.Reader;

public interface BodyReader {

  interface Binary {
    Object read(InputStream in) throws Exception;
  }

  interface Text {
    Object read(Reader reader) throws Exception;
  }

  <T> T text(Text text) throws Exception;

  <T> T bytes(Binary bin) throws Exception;

}

package jooby;

import java.io.OutputStream;
import java.io.Writer;

public interface BodyWriter {

  public interface Binary {
    void write(OutputStream out) throws Exception;
  }

  public interface Text {
    void write(Writer writer) throws Exception;
  }

  void text(Text text) throws Exception;

  void bytes(Binary bin) throws Exception;

}

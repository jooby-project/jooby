package jooby;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public interface Upload extends Closeable {

  String name();

  MediaType type();

  HttpField header(String name);

  File file() throws IOException;

}

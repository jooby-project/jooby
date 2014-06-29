package jooby;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Multimap;

public interface MessageConverter {

  List<MediaType> types();

  <T> T read(Class<T> type, MessageReader reader) throws IOException;

  void write(Object message, MessageWriter writer, Multimap<String, String> headers)
      throws IOException;
}

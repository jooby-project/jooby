package jooby;

import java.util.List;

import com.google.common.collect.Multimap;

public interface BodyConverter {

  List<MediaType> types();

  <T> T read(Class<T> type, BodyReader reader) throws Exception;

  void write(Object message, BodyWriter writer, Multimap<String, String> headers) throws Exception;
}

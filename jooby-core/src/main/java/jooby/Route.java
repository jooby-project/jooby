package jooby;

import java.util.List;
import java.util.Map;

import com.google.common.annotations.Beta;

@Beta
public interface Route {

  String path();

  String verb();

  String pattern();

  String name();

  int index();

  Map<String, String> vars();

  List<MediaType> consume();

  List<MediaType> produces();

}

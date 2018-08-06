package io.jooby;

import java.util.Map;

public interface Route {

  Map<String, String> params();

  String pattern();

  String method();

  Handler handler();
}

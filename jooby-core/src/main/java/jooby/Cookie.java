package jooby;

public interface Cookie {

  String name();

  String value();

  String comment();

  String domain();

  int maxAge();

  String path();

  boolean secure();

  int version();

  boolean httpOnly();

}

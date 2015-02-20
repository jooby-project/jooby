package org.jooby.servlet;

public interface ServletUpgrade {

  <T> T upgrade(Class<T> type) throws Exception;

}

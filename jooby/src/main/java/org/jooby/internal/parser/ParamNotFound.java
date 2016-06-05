package org.jooby.internal.parser;

import java.util.NoSuchElementException;

@SuppressWarnings("serial")
public class ParamNotFound extends NoSuchElementException {

  public ParamNotFound(final String name) {
    super(name);
  }
}

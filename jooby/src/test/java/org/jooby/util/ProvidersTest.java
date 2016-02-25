package org.jooby.util;

import org.jooby.scope.Providers;
import org.junit.Test;

import com.google.inject.OutOfScopeException;

public class ProvidersTest {

  @Test
  public void defaults() {
    new Providers();
  }

  @Test(expected = OutOfScopeException.class)
  public void outOfScope() {
    Providers.outOfScope(ProvidersTest.class).get();
  }

}

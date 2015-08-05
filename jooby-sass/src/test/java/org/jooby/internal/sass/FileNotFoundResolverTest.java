package org.jooby.internal.sass;

import org.jooby.Err;
import org.junit.Test;

import com.vaadin.sass.internal.ScssStylesheet;

public class FileNotFoundResolverTest {

  @Test
  public void byDefShouldNotFail() {
    new FileNotFoundResolver().validate(new ScssStylesheet());
  }

  @Test(expected = Err.class)
  public void shouldFailOnNullScss() {
    new FileNotFoundResolver().validate(null);
  }

  @Test(expected = Err.class)
  public void shouldFailOnFileNotFound() {
    FileNotFoundResolver resolver = new FileNotFoundResolver();
    resolver.resolveNormalized("some.file");
    resolver.resolveNormalized("some.file");
    resolver.validate(new ScssStylesheet());
  }

}

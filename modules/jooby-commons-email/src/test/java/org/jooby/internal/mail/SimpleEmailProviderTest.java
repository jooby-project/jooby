package org.jooby.internal.mail;

import static org.junit.Assert.assertTrue;

import org.apache.commons.mail.SimpleEmail;
import org.junit.Test;

public class SimpleEmailProviderTest extends EmailFactoryTest {

  @Test
  public void newEmail() throws Exception {
    SimpleEmailProvider provider = new SimpleEmailProvider(config());
    assertTrue(provider.get() instanceof SimpleEmail);
  }

}

package org.jooby.internal.mail;

import static org.junit.Assert.assertTrue;

import org.apache.commons.mail.MultiPartEmail;
import org.junit.Test;

public class MultiPartEmailProviderTest extends EmailFactoryTest {

  @Test
  public void newEmail() throws Exception {
    MultiPartEmailProvider provider = new MultiPartEmailProvider(config());
    assertTrue(provider.get() instanceof MultiPartEmail);
  }

}

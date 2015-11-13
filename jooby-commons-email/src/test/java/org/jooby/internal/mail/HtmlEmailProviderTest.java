package org.jooby.internal.mail;

import static org.junit.Assert.assertTrue;

import org.apache.commons.mail.HtmlEmail;
import org.junit.Test;

public class HtmlEmailProviderTest extends EmailFactoryTest {

  @Test
  public void newEmail() throws Exception {
    HtmlEmailProvider provider = new HtmlEmailProvider(config());
    assertTrue(provider.get() instanceof HtmlEmail);
  }

}

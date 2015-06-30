package org.jooby.internal.mail;

import static org.junit.Assert.assertTrue;

import org.apache.commons.mail.ImageHtmlEmail;
import org.junit.Test;

public class ImageHtmlEmailProviderTest extends EmailFactoryTest {

  @Test
  public void newEmail() throws Exception {
    ImageHtmlEmailProvider provider = new ImageHtmlEmailProvider(config());
    assertTrue(provider.get() instanceof ImageHtmlEmail);
  }

}

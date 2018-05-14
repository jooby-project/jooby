package org.jooby.internal.mail;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.util.Arrays;
import java.util.Collection;

import javax.mail.internet.InternetAddress;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.jooby.mail.CommonsEmail;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class EmailFactoryTest {

  protected Block defprops = unit -> {
    SimpleEmail email = unit.get(SimpleEmail.class);
    email.setCharset("UTF-8");
    email.setDebug(false);
    expect(email.setSendPartial(false)).andReturn(email);
    email.setSmtpPort(25);
    email.setSocketConnectionTimeout(60000);
    email.setSocketTimeout(60000);
    expect(email.setSSLOnConnect(false)).andReturn(email);
    email.setSslSmtpPort("465");
  };

  private Block fullprops = unit -> {
    SimpleEmail email = unit.get(SimpleEmail.class);
    email.setCharset("UTF-8");
    email.setDebug(false);
    expect(email.setSendPartial(false)).andReturn(email);
    email.setSmtpPort(25);
    email.setSocketConnectionTimeout(60000);
    email.setSocketTimeout(60000);
    expect(email.setSSLOnConnect(false)).andReturn(email);
    email.setSslSmtpPort("465");

    email.setAuthentication("uname", "pwd");
    expect(email.setBcc(Arrays.asList(new InternetAddress("Bcc Name <bcc@bcc.com>")))).andReturn(
        email);
    expect(email.setBounceAddress("bounceAddress@mail.com")).andReturn(email);
    expect(email.setCc(Arrays.asList(new InternetAddress("Cc Name <cc@cc.com>")))).andReturn(email);
    expect(email.setFrom("from@mail.com")).andReturn(email);
    email.setHostName("hostname.com");
    expect(email.setMsg("msg")).andReturn(email);
    expect(email.setReplyTo(Arrays.asList(new InternetAddress("Reply To <reply@to.com>"))))
        .andReturn(email);
    expect(email.setSSLCheckServerIdentity(false)).andReturn(email);
    expect(email.setStartTLSEnabled(false)).andReturn(email);
    expect(email.setStartTLSRequired(false)).andReturn(email);
    expect(email.setSubject("subject")).andReturn(email);
    expect(email.setTo(Arrays.asList(new InternetAddress("To <to@to.com>")))).andReturn(email);
  };

  private Block htmlfullprops = unit -> {
    HtmlEmail email = unit.get(HtmlEmail.class);
    email.setCharset("UTF-8");
    email.setDebug(false);
    expect(email.setSendPartial(false)).andReturn(email);
    email.setSmtpPort(25);
    email.setSocketConnectionTimeout(60000);
    email.setSocketTimeout(60000);
    expect(email.setSSLOnConnect(false)).andReturn(email);
    email.setSslSmtpPort("465");

    email.setAuthentication("uname", "pwd");
    expect(email.setBcc(Arrays.asList(new InternetAddress("Bcc Name <bcc@bcc.com>")))).andReturn(
        email);
    expect(email.setBounceAddress("bounceAddress@mail.com")).andReturn(email);
    expect(email.setCc(Arrays.asList(new InternetAddress("Cc Name <cc@cc.com>")))).andReturn(email);
    expect(email.setFrom("from@mail.com")).andReturn(email);
    email.setHostName("hostname.com");
    expect(email.setHtmlMsg("msg")).andReturn(email);
    expect(email.setReplyTo(Arrays.asList(new InternetAddress("Reply To <reply@to.com>"))))
        .andReturn(email);
    expect(email.setSSLCheckServerIdentity(false)).andReturn(email);
    expect(email.setStartTLSEnabled(false)).andReturn(email);
    expect(email.setStartTLSRequired(false)).andReturn(email);
    expect(email.setSubject("subject")).andReturn(email);
    expect(email.setTo(Arrays.asList(new InternetAddress("To <to@to.com>")))).andReturn(email);
  };

  @SuppressWarnings("unchecked")
  private Block badprops = unit -> {
    SimpleEmail email = unit.get(SimpleEmail.class);
    email.setCharset("UTF-8");
    email.setDebug(false);
    expect(email.setSendPartial(false)).andReturn(email);
    email.setSmtpPort(25);
    email.setSocketConnectionTimeout(60000);
    email.setSocketTimeout(60000);
    expect(email.setSSLOnConnect(false)).andReturn(email);
    email.setSslSmtpPort("465");

    expect(email.setTo(isA(Collection.class))).andThrow(new EmailException());
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit()
        .run(unit -> {
          new EmailFactory(config());
        });
  }

  @Test
  public void set() throws Exception {
    new MockUnit(SimpleEmail.class)
        .expect(defprops)
        .run(unit -> {
          new EmailFactory(config())
              .newEmail(unit.get(SimpleEmail.class));
        });
  }

  @Test
  public void setfull() throws Exception {
    new MockUnit(SimpleEmail.class)
        .expect(fullprops)
        .run(unit -> {
          new EmailFactory(fconfig())
              .newEmail(unit.get(SimpleEmail.class));
        });
  }

  @Test
  public void sethtmlfull() throws Exception {
    new MockUnit(HtmlEmail.class)
        .expect(htmlfullprops)
        .run(unit -> {
          new EmailFactory(fconfig())
              .newEmail(unit.get(HtmlEmail.class));
        });
  }

  @Test(expected = IllegalArgumentException.class)
  public void setbad() throws Exception {
    new MockUnit(SimpleEmail.class)
        .expect(badprops)
        .run(unit -> {
          new EmailFactory(config()
              .withValue("to", ConfigValueFactory.fromAnyRef("")))
              .newEmail(unit.get(SimpleEmail.class));
        });
  }

  private Config fconfig() {
    return config()
        .withValue("username", ConfigValueFactory.fromAnyRef("uname"))
        .withValue("password", ConfigValueFactory.fromAnyRef("pwd"))
        .withValue("bcc", ConfigValueFactory.fromAnyRef("Bcc Name <bcc@bcc.com>"))
        .withValue("bounceAddress", ConfigValueFactory.fromAnyRef("bounceAddress@mail.com"))
        .withValue("cc", ConfigValueFactory.fromAnyRef(Arrays.asList("Cc Name <cc@cc.com>")))
        .withValue("from", ConfigValueFactory.fromAnyRef("from@mail.com"))
        .withValue("hostName", ConfigValueFactory.fromAnyRef("hostname.com"))
        .withValue("msg", ConfigValueFactory.fromAnyRef("msg"))
        .withValue("replyTo",
            ConfigValueFactory.fromAnyRef(Arrays.asList("Reply To <reply@to.com>")))
        .withValue("ssl.checkServerIdentity", ConfigValueFactory.fromAnyRef(false))
        .withValue("ssl.onConnect", ConfigValueFactory.fromAnyRef(false))
        .withValue("startTLSEnabled", ConfigValueFactory.fromAnyRef(false))
        .withValue("startTLSRequired", ConfigValueFactory.fromAnyRef(false))
        .withValue("subject", ConfigValueFactory.fromAnyRef("subject"))
        .withValue("to", ConfigValueFactory.fromAnyRef("To <to@to.com>"));
  }

  protected Config config() {
    return ConfigFactory.parseResources(CommonsEmail.class, "commons-email.conf")
        .getConfig("mail")
        .withValue("application.charset", ConfigValueFactory.fromAnyRef("UTF-8"))
        .resolve();
  }
}

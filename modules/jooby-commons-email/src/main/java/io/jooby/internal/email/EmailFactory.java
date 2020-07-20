/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.email;

import com.typesafe.config.Config;
import io.jooby.SneakyThrows;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.ImageHtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public class EmailFactory {

  private final Config mail;

  public EmailFactory(final Config mail) {
    this.mail = requireNonNull(mail, "Mail config is required.");
  }

  public SimpleEmail newSimpleEmail() {
    return newEmail(new SimpleEmail());
  }

  public HtmlEmail newHtmlEmail() {
    return newEmail(new HtmlEmail());
  }

  public MultiPartEmail newMultiPartEmail() {
    return newEmail(new MultiPartEmail());
  }

  public ImageHtmlEmail newImageHtmlEmail() {
    return newEmail(new ImageHtmlEmail());
  }

  public <T extends Email> T newEmail(final T email) {
    ifset("username", p -> email.setAuthentication(mail.getString(p), mail.getString("password")));

    ifset("bcc", p -> email.setBcc(address(strList(p))));
    ifset("bounceAddress", p -> email.setBounceAddress(mail.getString(p)));
    ifset("cc", p -> email.setCc(address(strList(p))));
    email.setCharset(mail.getString("charset"));
    ifset("debug", p -> email.setDebug(mail.getBoolean(p)));
    ifset("from", p -> email.setFrom(mail.getString(p)));
    ifset("hostName", p -> email.setHostName(mail.getString(p)));
    ifset("msg", p -> {
      if (email instanceof HtmlEmail) {
        ((HtmlEmail) email).setHtmlMsg(mail.getString(p));
      } else {
        email.setMsg(mail.getString(p));
      }
    });
    ifset("replyTo", p -> email.setReplyTo(address(mail.getStringList(p))));
    ifset("sendPartial", p -> email.setSendPartial(mail.getBoolean(p)));
    ifset("smtpPort", p -> email.setSmtpPort(mail.getInt(p)));
    ifset("socketConnectionTimeout",
        p -> email.setSocketConnectionTimeout((int) mail.getDuration(p, TimeUnit.MILLISECONDS)));
    ifset("socketTimeout",
        p -> email.setSocketTimeout((int) mail.getDuration(p, TimeUnit.MILLISECONDS)));
    ifset("ssl.checkServerIdentity", p -> email.setSSLCheckServerIdentity(mail.getBoolean(p)));
    ifset("ssl.onConnect", p -> email.setSSLOnConnect(mail.getBoolean(p)));
    ifset("ssl.smtpPort", p -> email.setSslSmtpPort(mail.getString(p)));
    ifset("startTLSEnabled", p -> email.setStartTLSEnabled(mail.getBoolean(p)));
    ifset("startTLSRequired", p -> email.setStartTLSRequired(mail.getBoolean(p)));
    ifset("subject", p -> email.setSubject(mail.getString(p)));
    ifset("to", p -> email.setTo(address(strList(p))));

    return email;
  }

  private List<String> strList(final String p) {
    Object list = mail.getAnyRef(p);
    if (list instanceof String) {
      return singletonList(list.toString());
    }
    return mail.getStringList(p);
  }

  private List<InternetAddress> address(final List<String> list) throws AddressException {
    List<InternetAddress> addresses = new ArrayList<>(list.size());
    for (String addr : list) {
      addresses.add(new InternetAddress(addr));
    }
    return addresses;
  }

  private void ifset(final String key, final SneakyThrows.Consumer<String> setter) {
    if (mail.hasPath(key)) {
      setter.accept(key);
    }
  }
}

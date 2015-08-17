/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.mail;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.mail.Email;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;

public class EmailFactory {

  private static interface EmailSetter {
    void apply(String p) throws Exception;
  }

  private Config mail;

  public EmailFactory(final Config mail) {
    this.mail = requireNonNull(mail, "Mail config is required.");
  }

  public <T extends Email> T newEmail(final T email) {
    try {
      ifset("username", p -> {
        email.setAuthentication(mail.getString(p), mail.getString("password"));
      });

      ifset("bcc", p -> email.setBcc(address(strList(p))));
      ifset("bounceAddress", p -> email.setBounceAddress(mail.getString(p)));
      ifset("cc", p -> email.setCc(address(strList(p))));
      email.setCharset(mail.getString("charset"));
      ifset("debug", p -> email.setDebug(mail.getBoolean(p)));
      ifset("from", p -> email.setFrom(mail.getString(p)));
      ifset("hostName", p -> email.setHostName(mail.getString(p)));
      ifset("msg", p -> email.setHtmlMsg(mail.getString(p)));
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
    } catch (Exception ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  private List<String> strList(final String p) {
    Object list = mail.getAnyRef(p);
    if (list instanceof String) {
      return ImmutableList.of(list.toString());
    }
    return mail.getStringList(p);
  }

  private List<InternetAddress> address(final List<String> list) throws AddressException {
    ImmutableList.Builder<InternetAddress> builder = ImmutableList.builder();
    for (String address : list) {
      builder.add(new InternetAddress(address));
    }
    return builder.build();
  }

  private void ifset(final String key, final EmailSetter setter) throws Exception {
    if (mail.hasPath(key)) {
      setter.apply(key);
    }
  }
}

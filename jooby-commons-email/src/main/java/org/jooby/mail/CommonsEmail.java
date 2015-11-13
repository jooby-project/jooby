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
package org.jooby.mail;

import static java.util.Objects.requireNonNull;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.ImageHtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.internal.mail.HtmlEmailProvider;
import org.jooby.internal.mail.ImageHtmlEmailProvider;
import org.jooby.internal.mail.MultiPartEmailProvider;
import org.jooby.internal.mail.SimpleEmailProvider;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * <h1>commons email</h1>
 * <p>
 * Small but helpful module that provides access to {@link Email} instances via Guice and
 * {@link Config}.
 * </p>
 *
 * <h1>usage</h1>
 *
 * application.conf:
 *
 * <pre>
 *  mail.hostName = smtp.googlemail.com
 *  mail.ssl.onConnect = true
 *  mail.username = username
 *  mail.password = password
 *  mail.from = user&#64;gmail.com
 *  mail.subject = TestMail
 * </pre>
 *
 * <pre>
 * {
 *   use(new CommonsEmail());
 *
 *   get("/send", req {@literal ->} {
 *     req.require(SimpleEmail.class)
 *        .setMsg("you got an email!")
 *        .setTo("foo&#64;bar.com")
 *        .send();
 *   });
 * }
 * </pre>
 *
 * <p>
 * That's all it does! Every time you require an email, it creates one and setup properties from
 * <code>mail.*</code>.
 * </p>
 *
 * @author edgar
 * @since 0.7.0
 */
public class CommonsEmail implements Jooby.Module {

  private String name;

  private boolean named;

  /**
   * Creates a {@link CommonsEmail}.
   *
   * @param name Name of the property who has the mail information. Default is: <code>mail.*</code>.
   */
  public CommonsEmail(final String name) {
    this.name = requireNonNull(name, "Mail name is required.");
  }

  /**
   * Creates a {@link CommonsEmail}.
   */
  public CommonsEmail() {
    this("mail");
  }

  /**
   * Call this method if you need two or more mail configuration. This method will bind email
   * instances using the provided name.
   *
   * @return This module.
   */
  public CommonsEmail named() {
    named = true;
    return this;
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    Config mail = config.getConfig(name).withFallback(config.getConfig("mail"));

    binder.bind(key(SimpleEmail.class)).toProvider(new SimpleEmailProvider(mail));
    binder.bind(key(HtmlEmail.class)).toProvider(new HtmlEmailProvider(mail));
    binder.bind(key(MultiPartEmail.class)).toProvider(new MultiPartEmailProvider(mail));
    binder.bind(key(ImageHtmlEmail.class)).toProvider(new ImageHtmlEmailProvider(mail));
  }

  private <T> Key<T> key(final Class<T> type) {
    return named ? Key.get(type, Names.named(name)) : Key.get(type);
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "commons-email.conf");
  }

}

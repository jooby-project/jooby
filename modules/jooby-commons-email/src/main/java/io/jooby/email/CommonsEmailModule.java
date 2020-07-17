/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.email;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import io.jooby.internal.email.HtmlEmailProvider;
import io.jooby.internal.email.ImageHtmlEmailProvider;
import io.jooby.internal.email.MultiPartEmailProvider;
import io.jooby.internal.email.SimpleEmailProvider;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.ImageHtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;

import javax.annotation.Nonnull;
import javax.inject.Provider;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * <h1>commons email</h1>
 * <p>
 * Small but helpful module that provides access to {@link Email} instances via
 * the service registry and {@link Config}.
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
 *   install(new CommonsEmailModule());
 *
 *   get("/send", ctx {@literal ->} {
 *     require(SimpleEmail.class)
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
 * @since 2.8.9
 */
public class CommonsEmailModule implements Extension {

  private final String name;

  /**
   * Creates a {@link CommonsEmailModule}.
   *
   * @param name Name of the property who has the mail information. Default is: <code>mail.*</code>.
   */
  public CommonsEmailModule(String name) {
    this.name = requireNonNull(name, "Mail name is required.");
  }

  /**
   * Creates a {@link CommonsEmailModule}.
   */
  public CommonsEmailModule() {
    this("mail");
  }

  @Override
  public void install(@Nonnull Jooby application) throws Exception {
    Config appConfig = application.getConfig();

    Config mail = appConfig.getConfig(name)
        .withFallback(appConfig.getConfig("mail"))
        .withFallback(defaults(appConfig));

    ServiceRegistry services = application.getServices();

    register(services, SimpleEmail.class, new SimpleEmailProvider(mail));
    register(services, HtmlEmail.class, new HtmlEmailProvider(mail));
    register(services, MultiPartEmail.class, new MultiPartEmailProvider(mail));
    register(services, ImageHtmlEmail.class, new ImageHtmlEmailProvider(mail));
  }

  private <T> void register(ServiceRegistry services, Class<T> clazz, Provider<T> provider) {
    services.putIfAbsent(clazz, provider);
    services.put(ServiceKey.key(clazz, name), provider);
  }

  private Config defaults(Config appConfig) {
    final Map<String, Object> defaults = new HashMap<>();
    defaults.put("charset", appConfig.getString("application.charset"));
    return ConfigFactory.parseMap(defaults, CommonsEmailModule.class.getSimpleName() + "#defaults(...)");
  }
}

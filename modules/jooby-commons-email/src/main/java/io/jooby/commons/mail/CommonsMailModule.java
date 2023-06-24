/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.commons.mail;

import static java.util.Objects.requireNonNull;

import java.util.stream.Stream;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.ImageHtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException.Missing;
import com.typesafe.config.ConfigFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.AvailableSettings;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import io.jooby.internal.commons.mail.EmailFactory;
import jakarta.inject.Provider;

/**
 * commons email.
 *
 * <p>Small but helpful module that provides access to {@link Email} instances via the service
 * registry and {@link Config}.
 *
 * <h2>usage</h2>
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
 * <p>That's all it does! Every time you require an email, it creates one and setup properties from
 * <code>mail.*</code>.
 *
 * @author edgar
 * @since 2.8.9
 */
public class CommonsMailModule implements Extension {

  private final String name;

  /**
   * Creates a {@link CommonsMailModule}.
   *
   * @param name Name of the property who has the mail information. Default is: <code>mail.*</code>.
   */
  public CommonsMailModule(String name) {
    this.name = requireNonNull(name, "Mail name is required.");
  }

  /** Creates a {@link CommonsMailModule}. */
  public CommonsMailModule() {
    this("mail");
  }

  @Override
  public void install(@NonNull Jooby application) {
    Config config = mailConfig(application.getConfig(), name);

    ServiceRegistry services = application.getServices();

    EmailFactory factory = new EmailFactory(config);

    register(services, SimpleEmail.class, factory::newSimpleEmail);
    register(services, HtmlEmail.class, factory::newHtmlEmail);
    register(services, MultiPartEmail.class, factory::newMultiPartEmail);
    register(services, ImageHtmlEmail.class, factory::newImageHtmlEmail);
  }

  static Config mailConfig(Config config, String name) {
    Config mail =
        Stream.of(name, "mail")
            .distinct()
            .filter(config::hasPath)
            .map(config::getConfig)
            .reduce(Config::withFallback)
            .orElseThrow(() -> new Missing(Stream.of(name, "mail").distinct().findFirst().get()));

    mail =
        mail.withFallback(
            ConfigFactory.empty().withValue("charset", config.getValue(AvailableSettings.CHARSET)));
    return mail;
  }

  private <T> void register(ServiceRegistry services, Class<T> clazz, Provider<T> provider) {
    services.putIfAbsent(clazz, provider);
    services.put(ServiceKey.key(clazz, name), provider);
  }
}

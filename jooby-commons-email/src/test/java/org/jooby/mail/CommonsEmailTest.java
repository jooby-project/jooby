package org.jooby.mail;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.ImageHtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.jooby.Env;
import org.jooby.internal.mail.HtmlEmailProvider;
import org.jooby.internal.mail.ImageHtmlEmailProvider;
import org.jooby.internal.mail.MultiPartEmailProvider;
import org.jooby.internal.mail.SimpleEmailProvider;
import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class CommonsEmailTest {

  @SuppressWarnings("unchecked")
  @Test
  public void configure() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);

          Config mail = unit.mock(Config.class);
          expect(mail.withFallback(config)).andReturn(mail);

          expect(config.getConfig("mail")).andReturn(mail);
          expect(config.getConfig("mail")).andReturn(config);

          AnnotatedBindingBuilder<SimpleEmail> abbSE = unit.mock(AnnotatedBindingBuilder.class);
          expect(abbSE.toProvider(isA(SimpleEmailProvider.class))).andReturn(null);

          AnnotatedBindingBuilder<HtmlEmail> abbHE = unit.mock(AnnotatedBindingBuilder.class);
          expect(abbHE.toProvider(isA(HtmlEmailProvider.class))).andReturn(null);

          AnnotatedBindingBuilder<MultiPartEmail> abbMPE = unit
              .mock(AnnotatedBindingBuilder.class);
          expect(abbMPE.toProvider(isA(MultiPartEmailProvider.class))).andReturn(null);

          AnnotatedBindingBuilder<ImageHtmlEmail> abbIHE = unit
              .mock(AnnotatedBindingBuilder.class);
          expect(abbIHE.toProvider(isA(ImageHtmlEmailProvider.class))).andReturn(null);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Key.get(SimpleEmail.class))).andReturn(abbSE);
          expect(binder.bind(Key.get(HtmlEmail.class))).andReturn(abbHE);
          expect(binder.bind(Key.get(MultiPartEmail.class))).andReturn(abbMPE);
          expect(binder.bind(Key.get(ImageHtmlEmail.class))).andReturn(abbIHE);
        })
        .run(unit -> {
          new CommonsEmail()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void configureWithName() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);

          Config mail = unit.mock(Config.class);
          expect(mail.withFallback(config)).andReturn(mail);

          expect(config.getConfig("mail")).andReturn(mail);
          expect(config.getConfig("mail")).andReturn(config);

          AnnotatedBindingBuilder<SimpleEmail> abbSE = unit.mock(AnnotatedBindingBuilder.class);
          expect(abbSE.toProvider(isA(SimpleEmailProvider.class))).andReturn(null);

          AnnotatedBindingBuilder<HtmlEmail> abbHE = unit.mock(AnnotatedBindingBuilder.class);
          expect(abbHE.toProvider(isA(HtmlEmailProvider.class))).andReturn(null);

          AnnotatedBindingBuilder<MultiPartEmail> abbMPE = unit
              .mock(AnnotatedBindingBuilder.class);
          expect(abbMPE.toProvider(isA(MultiPartEmailProvider.class))).andReturn(null);

          AnnotatedBindingBuilder<ImageHtmlEmail> abbIHE = unit
              .mock(AnnotatedBindingBuilder.class);
          expect(abbIHE.toProvider(isA(ImageHtmlEmailProvider.class))).andReturn(null);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Key.get(SimpleEmail.class, Names.named("mail")))).andReturn(abbSE);
          expect(binder.bind(Key.get(HtmlEmail.class, Names.named("mail")))).andReturn(abbHE);
          expect(binder.bind(Key.get(MultiPartEmail.class, Names.named("mail")))).andReturn(abbMPE);
          expect(binder.bind(Key.get(ImageHtmlEmail.class, Names.named("mail")))).andReturn(abbIHE);
        })
        .run(unit -> {
          new CommonsEmail()
              .named()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void config() throws Exception {
    assertEquals(ConfigFactory.parseResources(getClass(), "commons-email.conf"),
        new CommonsEmail().config());
  }
}

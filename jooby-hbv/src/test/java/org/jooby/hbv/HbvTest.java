package org.jooby.hbv;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.bootstrap.ProviderSpecificBootstrap;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.jooby.Env;
import org.jooby.MockUnit;
import org.jooby.MockUnit.Block;
import org.jooby.Parser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Hbv.class, Validation.class, Multibinder.class })
public class HbvTest {

  @SuppressWarnings("unchecked")
  private Block hibernateValiConf = unit -> {
    HibernateValidatorConfiguration hvc = unit.get(HibernateValidatorConfiguration.class);

    ProviderSpecificBootstrap<HibernateValidatorConfiguration> provider = unit
        .mock(ProviderSpecificBootstrap.class);
    expect(provider.configure()).andReturn(hvc);

    unit.mockStatic(Validation.class);
    expect(Validation.byProvider(HibernateValidator.class)).andReturn(provider);

    AnnotatedBindingBuilder<HibernateValidatorConfiguration> abbHVC = unit
        .mock(AnnotatedBindingBuilder.class);
    abbHVC.toInstance(hvc);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(HibernateValidatorConfiguration.class)).andReturn(abbHVC);
  };

  @SuppressWarnings("unchecked")
  private Block validatorProvider = unit -> {
    ScopedBindingBuilder sbbHVF = unit.mock(ScopedBindingBuilder.class);
    sbbHVF.asEagerSingleton();

    AnnotatedBindingBuilder<Validator> abbv = unit.mock(AnnotatedBindingBuilder.class);
    expect(abbv.toProvider(HbvFactory.class)).andReturn(sbbHVF);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Validator.class)).andReturn(abbv);
  };

  @SuppressWarnings("unchecked")
  private Block hvparser = unit -> {
    Binder binder = unit.get(Binder.class);

    LinkedBindingBuilder<Parser> lbbp = unit.mock(LinkedBindingBuilder.class);
    lbbp.toInstance(isA(HbvParser.class));

    Multibinder<Parser> parsers = unit.mock(Multibinder.class);
    expect(parsers.addBinding()).andReturn(lbbp);

    unit.mockStatic(Multibinder.class);
    expect(Multibinder.newSetBinder(binder, Parser.class)).andReturn(parsers);
  };

  private Block noproperties = unit -> {
    Config config = unit.get(Config.class);

    expect(config.hasPath("hibernate.validator")).andReturn(false);
  };

  private Block properties = unit -> {
    Config config = unit.get(Config.class);

    expect(config.hasPath("hibernate.validator")).andReturn(true);

    Config props = ConfigFactory.empty()
        .withValue("fail_fast", ConfigValueFactory.fromAnyRef(true));

    expect(config.getConfig("hibernate.validator")).andReturn(props);

    HibernateValidatorConfiguration hvc = unit.get(HibernateValidatorConfiguration.class);
    expect(hvc.addProperty("hibernate.validator.fail_fast", "true")).andReturn(hvc);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, HibernateValidatorConfiguration.class)
        .expect(hibernateValiConf)
        .expect(noproperties)
        .expect(validatorProvider)
        .expect(hvparser)
        .run(unit -> {
          new Hbv()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void defaultsWithProperties() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, HibernateValidatorConfiguration.class)
        .expect(hibernateValiConf)
        .expect(properties)
        .expect(validatorProvider)
        .expect(hvparser)
        .run(unit -> {
          new Hbv()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void defaultsWithConfigurer() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, HibernateValidatorConfiguration.class,
        Consumer.class)
        .expect(hibernateValiConf)
        .expect(noproperties)
        .expect(validatorProvider)
        .expect(hvparser)
        .expect(unit -> {
          Consumer<HibernateValidatorConfiguration> configurer = unit.get(Consumer.class);

          configurer.accept(unit.get(HibernateValidatorConfiguration.class));
        })
        .run(unit -> {
          new Hbv()
              .doWith(unit.get(Consumer.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void defaultsWithFulleConfigurer() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, HibernateValidatorConfiguration.class,
        BiConsumer.class)
        .expect(hibernateValiConf)
        .expect(noproperties)
        .expect(validatorProvider)
        .expect(hvparser)
        .expect(unit -> {
          BiConsumer<HibernateValidatorConfiguration, Config> configurer = unit
              .get(BiConsumer.class);

          configurer.accept(unit.get(HibernateValidatorConfiguration.class),
              unit.get(Config.class));
        })
        .run(unit -> {
          new Hbv()
              .doWith(unit.get(BiConsumer.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

}

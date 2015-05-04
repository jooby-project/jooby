package org.jooby.hbv;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.hibernate.validator.HibernateValidatorConfiguration;
import org.jooby.MockUnit;
import org.jooby.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Injector;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HbvFactory.class, HbvConstraintValidatorFactory.class })
public class HbvFactoryTest {

  private Block constructor = unit -> {
    HbvConstraintValidatorFactory hcvf = unit
        .mockConstructor(HbvConstraintValidatorFactory.class,
            new Class[]{Injector.class }, unit.get(Injector.class));

    HibernateValidatorConfiguration hvc = unit.get(HibernateValidatorConfiguration.class);
    expect(hvc.constraintValidatorFactory(hcvf)).andReturn(null);
  };

  private Block start = unit -> {
    Validator validator = unit.get(Validator.class);

    ValidatorFactory vf = unit.get(ValidatorFactory.class);
    expect(vf.getValidator()).andReturn(validator);

    HibernateValidatorConfiguration hvc = unit.get(HibernateValidatorConfiguration.class);
    expect(hvc.buildValidatorFactory()).andReturn(vf);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(HibernateValidatorConfiguration.class, Injector.class)
        .expect(constructor)
        .run(unit -> {
          new HbvFactory(
              unit.get(HibernateValidatorConfiguration.class),
              unit.get(Injector.class)
            );
          });
  }

  @Test
  public void start() throws Exception {
    new MockUnit(HibernateValidatorConfiguration.class, Injector.class, ValidatorFactory.class,
        Validator.class)
        .expect(constructor)
        .expect(start)
        .run(unit -> {
          HbvFactory hbvFactory = new HbvFactory(
              unit.get(HibernateValidatorConfiguration.class),
              unit.get(Injector.class)
              );
          hbvFactory.start();

          assertEquals(unit.get(Validator.class), hbvFactory.get());
        });
  }

  @Test
  public void stop() throws Exception {
    new MockUnit(HibernateValidatorConfiguration.class, Injector.class, ValidatorFactory.class,
        Validator.class)
        .expect(constructor)
        .expect(start)
        .expect(unit -> {
          ValidatorFactory validatorFactory = unit.get(ValidatorFactory.class);
          validatorFactory.close();
        })
        .run(unit -> {
          HbvFactory hbvFactory = new HbvFactory(
              unit.get(HibernateValidatorConfiguration.class),
              unit.get(Injector.class)
              );
          hbvFactory.start();
          hbvFactory.stop();
          // ignored
          hbvFactory.stop();
        });
  }
}

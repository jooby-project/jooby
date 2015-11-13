package org.jooby.hbv;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.inject.Injector;

public class HbvConstraintValidatorFactoryTest {

  @SuppressWarnings("rawtypes")
  private static class CloseableConstraintValidator implements ConstraintValidator, Closeable {

    private Closeable closeable;

    public CloseableConstraintValidator(final Closeable closeable) {
      this.closeable = closeable;
    }

    @Override
    public void initialize(final Annotation constraintAnnotation) {

    }

    @Override
    public boolean isValid(final Object value, final ConstraintValidatorContext context) {
      return false;
    }

    @Override
    public void close() throws IOException {
      closeable.close();
    }

  }

  @Test
  public void defaults() throws Exception {
    new MockUnit(Injector.class)
        .run(unit -> {
          new HbvConstraintValidatorFactory(unit.get(Injector.class));
        });
  }

  @Test
  public void getInstance() throws Exception {
    new MockUnit(Injector.class, ConstraintValidator.class)
        .expect(unit -> {
          Injector injector = unit.get(Injector.class);
          expect(injector.getInstance(ConstraintValidator.class)).andReturn(
              unit.get(ConstraintValidator.class));
        })
        .run(unit -> {
          assertEquals(unit.get(ConstraintValidator.class),
              new HbvConstraintValidatorFactory(unit.get(Injector.class))
                  .getInstance(ConstraintValidator.class));
        });
  }

  @Test
  public void releaseInstance() throws Exception {
    new MockUnit(Injector.class, ConstraintValidator.class)
        .run(unit -> {
          new HbvConstraintValidatorFactory(unit.get(Injector.class))
              .releaseInstance(unit.get(ConstraintValidator.class));
        });
  }

  @Test
  public void releaseCloseableInstance() throws Exception {
    new MockUnit(Injector.class, Closeable.class)
        .expect(unit -> {
          Closeable closeable = unit.get(Closeable.class);
          closeable.close();
        })
        .run(unit -> {
          new HbvConstraintValidatorFactory(unit.get(Injector.class))
              .releaseInstance(new CloseableConstraintValidator(unit.get(Closeable.class)));
        });
  }

  @Test
  public void releaseCloseableInstanceWithIOError() throws Exception {
    new MockUnit(Injector.class, Closeable.class)
    .expect(unit -> {
      Closeable closeable = unit.get(Closeable.class);
      closeable.close();
      expectLastCall().andThrow(new IOException("intentional err"));
    })
    .run(unit -> {
      new HbvConstraintValidatorFactory(unit.get(Injector.class))
          .releaseInstance(new CloseableConstraintValidator(unit.get(Closeable.class)));
    });
  }

}

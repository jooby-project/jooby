package org.jooby.hbv;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import org.jooby.MockUnit;
import org.jooby.Parser;
import org.jooby.Parser.Context;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.google.inject.TypeLiteral;

public class HbvParserTest {

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test(expected = ConstraintViolationException.class)
  public void validateInvalid() throws Exception {
    Object value = new HbvParserTest();
    new MockUnit(Parser.Context.class)
        .expect(unit -> {
          Context ctx = unit.get(Parser.Context.class);
          expect(ctx.next()).andReturn(value);
        })
        .expect(unit -> {
          ConstraintViolation violation = unit.mock(ConstraintViolation.class);
          Set<ConstraintViolation<Object>> violations = Sets.newHashSet(violation);

          Validator validator = unit.mock(Validator.class);
          expect(validator.validate(value)).andReturn(violations);

          Context ctx = unit.get(Parser.Context.class);
          expect(ctx.require(Validator.class)).andReturn(validator);
        })
        .run(unit -> {
          HbvParser parser = new HbvParser(Hbv.typeIs(new Class[]{HbvParserTest.class }));
          parser.parse(TypeLiteral.get(value.getClass()), unit.get(Parser.Context.class));
        });
  }

  @Test
  public void validate() throws Exception {
    Object value = new HbvParserTest();
    new MockUnit(Parser.Context.class)
        .expect(unit -> {
          Context ctx = unit.get(Parser.Context.class);
          expect(ctx.next()).andReturn(value);
        })
        .expect(unit -> {
          Set<ConstraintViolation<Object>> violations = Sets.newHashSet();

          Validator validator = unit.mock(Validator.class);
          expect(validator.validate(value)).andReturn(violations);

          Context ctx = unit.get(Parser.Context.class);
          expect(ctx.require(Validator.class)).andReturn(validator);
        })
        .run(unit -> {
          HbvParser parser = new HbvParser(Hbv.typeIs(new Class[]{HbvParserTest.class }));
          parser.parse(TypeLiteral.get(value.getClass()), unit.get(Parser.Context.class));
        });
  }

  @Test
  public void ignored() throws Exception {
    Object value = new HbvParserTest();
    new MockUnit(Parser.Context.class)
        .expect(unit -> {
          Context ctx = unit.get(Parser.Context.class);
          expect(ctx.next()).andReturn(value);
        })
        .run(unit -> {
          HbvParser parser = new HbvParser(Hbv.typeIs(new Class[]{Object.class }));
          parser.parse(TypeLiteral.get(value.getClass()), unit.get(Parser.Context.class));
        });
  }

  @Test
  public void none() throws Exception {
    Object value = new HbvParserTest();
    new MockUnit(Parser.Context.class)
        .expect(unit -> {
          Context ctx = unit.get(Parser.Context.class);
          expect(ctx.next()).andReturn(value);
        })
        .run(unit -> {
          HbvParser parser = new HbvParser(Hbv.none());
          parser.parse(TypeLiteral.get(value.getClass()), unit.get(Parser.Context.class));
        });
  }

  @Test
  public void toStr() throws Exception {
    HbvParser parser = new HbvParser(Hbv.typeIs(new Class[]{Object.class }));
    assertEquals("hbv", parser.toString());
  }

}

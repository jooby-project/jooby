package org.jooby.internal.parser;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.jooby.internal.ParameterNameProvider;
import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.common.collect.Sets;

public class BeanPlanTest {

  public static class TwoInject {

    @Inject
    public TwoInject(final String foo) {
    }

    @Inject
    public TwoInject(final int foo) {
    }

  }

  public static class UnknownCons {
    public UnknownCons(final String foo) {
    }

    public UnknownCons(final int foo) {
    }
  }

  public static class Base {
    String foo;
  }

  public static class GraphMethod {
    Base base;

    public Base base() {
      return base;
    }

    public void base(final Base base) {
      this.base = base;
    }
  }

  public static class Ext extends Base {
    String bar;
  }

  public static class SetterLike {
    String bar;

    public SetterLike bar(final String bar) {
      this.bar = "^" + bar;
      return this;
    }
  }

  public static class BadSetter {
    String bar;

    public void setBar() {
    }
  }

  @Test(expected = IllegalStateException.class)
  public void shouldRejectClassWithTwoConsWithInject() throws Exception {
    new MockUnit(ParameterNameProvider.class)
        .run(unit -> {
          new BeanPlan(unit.get(ParameterNameProvider.class), TwoInject.class);
        });
  }

  @Test(expected = IllegalStateException.class)
  public void shouldRejectClassWithTwoCons() throws Exception {
    new MockUnit(ParameterNameProvider.class)
        .run(unit -> {
          new BeanPlan(unit.get(ParameterNameProvider.class), UnknownCons.class);
        });
  }

  @Test
  public void shouldFindMemberOnSuperclass() throws Exception {
    new MockUnit(ParameterNameProvider.class)
        .run(unit -> {
          BeanPlan plan = new BeanPlan(unit.get(ParameterNameProvider.class), Ext.class);
          Ext bean = (Ext) plan.newBean(p -> p.name, Sets.newHashSet("foo", "bar"));
          assertEquals("foo", bean.foo);
          assertEquals("bar", bean.bar);
        });
  }

  @Test
  public void shouldFavorSetterLikeMethod() throws Exception {
    new MockUnit(ParameterNameProvider.class)
        .run(unit -> {
          BeanPlan plan = new BeanPlan(unit.get(ParameterNameProvider.class), SetterLike.class);
          SetterLike bean = (SetterLike) plan.newBean(p -> p.name, Sets.newHashSet("bar"));
          assertEquals("^bar", bean.bar);
        });
  }

  @Test
  public void shouldIgnoreSetterMethodWithZeroOrMoreArg() throws Exception {
    new MockUnit(ParameterNameProvider.class)
        .run(unit -> {
          BeanPlan plan = new BeanPlan(unit.get(ParameterNameProvider.class), BadSetter.class);
          BadSetter bean = (BadSetter) plan.newBean(p -> p.name, Sets.newHashSet("bar"));
          assertEquals("bar", bean.bar);
        });
  }

  @Test
  public void shouldTraverseGraphMethod() throws Exception {
    new MockUnit(ParameterNameProvider.class)
        .run(unit -> {
          BeanPlan plan = new BeanPlan(unit.get(ParameterNameProvider.class), GraphMethod.class);
          GraphMethod bean = (GraphMethod) plan.newBean(p -> p.name, Sets.newHashSet("base[foo]"));
          assertEquals("base[foo]", bean.base.foo);
        });
  }

}

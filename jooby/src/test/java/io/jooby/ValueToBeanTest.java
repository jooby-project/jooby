package io.jooby;

import io.jooby.internal.ValueInjector;
import io.jooby.internal.UrlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValueToBeanTest {

  public static class User {

    private final String name;
    private final String password;

    public User(String name, String password) {
      this.name = name;
      this.password = password;
    }

    @Override public String toString() {
      return name + ":" + password;
    }
  }

  public static class Person {

    private final String name;
    private final int age;
    private final Address address;

    public Person(String name, int age, Address address) {
      this.name = name;
      this.age = age;
      this.address = address;
    }

    @Override public String toString() {
      return name + "; age: " + age + "; street: " + address;
    }
  }

  public static class Address {

    private final String street;

    private final String number;

    public Address(String street, String number) {
      this.street = street;
      this.number = number;
    }

    @Override public String toString() {
      return number + " " + street;
    }
  }

  public static class ListOfSomething {
    private final List<String> list;

    public ListOfSomething(List<String> list) {
      this.list = list;
    }

    @Override public String toString() {
      return list.toString();
    }
  }

  public static class ListOfTwo {
    private final List<Integer> list;

    public ListOfTwo(List<Integer> list) {
      this.list = list;
    }

    @Override public String toString() {
      return list.toString();
    }
  }

  public static class ListOfOne {
    private final int list;

    public ListOfOne(int list) {
      this.list = list;
    }

    @Override public String toString() {
      return Integer.toString(list);
    }
  }

  public static class ListOfUser {
    private final List<User> list;

    public ListOfUser(List<User> list) {
      this.list = list;
    }

    @Override public String toString() {
      return list.toString();
    }
  }

  public enum Letter {
    A, B;
  }

  public static class Abc {
    private final Letter letter;

    public Abc(Letter letter) {
      this.letter = letter;
    }

    @Override public String toString() {
      return letter.toString();
    }
  }

  public static class MultiConstructor {
    private String foo;
    private String bar;

    public MultiConstructor() {
    }

    @Inject
    public MultiConstructor(String foo, String bar) {
      this.foo = foo;
      this.bar = bar;
    }

    @Override public String toString() {
      return foo + ":" + bar;
    }
  }

  public static class AwfulNames {
    private String foo;
    private String bar;

    public AwfulNames(@Named("foo-1") String foo, @Named("b:0") String bar) {
      this.foo = foo;
      this.bar = bar;
    }

    @Override public String toString() {
      return foo + ":" + bar;
    }
  }

  public static class Recursive {
    private String level;
    private List<Recursive> children = Collections.emptyList();

    public void setLevel(String level) {
      this.level = level;
    }

    public void setChildren(List<Recursive> children) {
      this.children = children;
    }

    @Override public String toString() {
      return level + ":" + children;
    }
  }

  public static class Mixed {
    private String foo;
    private String bar;
    private int number;
    private List<String> values;

    public Mixed(String foo) {
      this.foo = foo;
    }

    @Override public String toString() {
      return foo + ":" + bar + ":" + number + ":" + values;
    }

    public void setFoo(String foo) {
      this.foo = "set" + foo;
    }

    public void setBar(String bar) {
      this.bar = bar;
    }

    public void setNumber(int number) {
      this.number = number;
    }

    public void setValues(List<String> values) {
      this.values = values;
    }
  }

  public static class Member {
    private final String firstname;
    private final String lastname;

    public Member(String firstname, String lastname) {
      this.firstname = firstname;
      this.lastname = lastname;
    }

    @Override public String toString() {
      return firstname + ":" + lastname;
    }
  }

  public static class Group {

    private final List<Member> members;

    public Group(List<Member> members) {
      this.members = members;
    }

    @Override
    public String toString() {
      return Optional.ofNullable(members).map(it -> it.toString()).orElse("[]");
    }

  }

  public static class Tree {
    private String name;

    private List<Tree> children = Collections.emptyList();

    public void setChildren(List<Tree> children) {
      this.children = children;
    }

    public void setName(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name + children;
    }
  }

  public static class ListOfStr {
    List<String> children = new ArrayList<>();

    public void setChildren(List<String> children) {
      this.children = children;
    }

    @Override
    public String toString() {
      return children.toString();
    }
  }

  public static class UserId {
    private String id;

    public UserId(String id) {
      this.id = id;
    }

    public static UserId valueOf(String value) {
      return new UserId("valueOf:" + value);
    }

    @Override
    public String toString() {
      return id;
    }
  }

  public static class UserCons {
    private String id;

    public UserCons(String id) {
      this.id = id;
    }

    @Override
    public String toString() {
      return id;
    }
  }

  @Test
  public void constructorInjection() {
    queryString("?name=user&password=pass", queryString -> {
      assertEquals("user:pass", queryString.to(User.class).toString());
    });

    queryString("?name=user", queryString -> {
      assertMessage(Err.Missing.class, () -> queryString.to(User.class),
          "Required value is not present: 'password'");

      ValueInjector injector = new ValueInjector().missingToNull();
      assertEquals("user:null",
          injector.inject(queryString, User.class).toString());
    });

    queryString("?name=Sherlock Holmes&age=42&address.street=Baker&address.number=221B",
        queryString -> {
          assertEquals("Sherlock Holmes; age: 42; street: 221B Baker",
              queryString.to(Person.class).toString());
        });
  }

  @Test
  public void orderOfTabularData() {
    queryString(
        "?members[1]firstname=A&members[1]lastname=1&members[0]firstname=B&members[0]lastname=2",
        queryString -> {
          assertEquals("[B:2, A:1]", queryString.to(Group.class).toString());
        });
    queryString("?children[1]=1&children[2]=2&children[0]=0", queryString -> {
      assertEquals("[0, 1, 2]", queryString.to(ListOfStr.class).toString());
    });
  }

  @Test
  public void tabularData() {
    queryString("?members[0][firstname]=Pedro&members[0][lastname]=PicaPiedra", queryString -> {
      assertEquals("[Pedro:PicaPiedra]", queryString.to(Group.class).toString());
    });
    queryString("?[0][firstname]=Pedro&[0][lastname]=PicaPiedra", queryString -> {
      assertEquals("[Pedro:PicaPiedra]", queryString.toList(Member.class).toString());
    });
    queryString("?name=A&children[0][name]=B", queryString -> {
      assertEquals("A[B[]]", queryString.to(Tree.class).toString());
    });
    queryString("?name=A&children[0][name]=B&children[1][name]=C", queryString -> {
      assertEquals("A[B[], C[]]", queryString.to(Tree.class).toString());
    });
  }

  @Test
  public void constructorSelection() {
    queryString("?foo=foo&bar=bar", queryString -> {
      assertEquals("foo:bar", queryString.to(MultiConstructor.class).toString());
    });
  }

  @Test
  public void awfulNames() {
    queryString("?foo-1=foo&b:0=bar", queryString -> {
      assertEquals("foo:bar", queryString.to(AwfulNames.class).toString());
    });
  }

  @Test
  public void listOfSomething() {
    queryString("?list=a&list=b", queryString -> {
      assertEquals("[a, b]", queryString.to(ListOfSomething.class).toString());
    });

    queryString("?list=1&list=2", queryString -> {
      assertEquals("1", queryString.to(ListOfOne.class).toString());
      assertEquals("[1, 2]", queryString.to(ListOfTwo.class).toString());
    });

    queryString(
        "?list[0]name=user1&list[0]password=pass1&list[1]name=user2&list[1]password=pass2",
        queryString -> {
          assertEquals("[user1:pass1, user2:pass2]",
              queryString.to(ListOfUser.class).toString());
        });

    queryString("?[0]name=user1&[0]password=pass1&[1]name=user2&[1]password=pass2",
        queryString -> {
          assertEquals("[user1:pass1, user2:pass2]",
              queryString.toList(User.class).toString());
        });

    queryString("?[0]=a&[1]=b", queryString -> {
      assertEquals("[a, b]", queryString.toList(String.class).toString());
    });
  }

  @Test
  public void valueOf() {
    queryString("?letter=A&letter=B", queryString -> {
      assertEquals("[A]", queryString.toList(Abc.class).toString());
    });

    queryString("?letter=A", queryString -> {
      assertEquals("A", queryString.to(Abc.class).toString());
      assertEquals("[A]", queryString.toList(Abc.class).toString());
    });

    queryString("?[0]letter=A&[1]letter=B", queryString -> {
      assertEquals("[A, B]", queryString.toList(Abc.class).toString());
    });

    queryString("?id=userId", queryString -> {
      assertEquals("valueOf:userId", queryString.to(UserId.class).toString());
    });
    queryString("?id=userId", queryString -> {
      assertEquals("valueOf:userId", queryString.get("id").to(UserId.class).toString());
    });
  }

  @Test
  public void optional() {
    queryString("?foo=bar", queryString -> {
      assertEquals("Optional[bar]",
          queryString.get("foo").toOptional(String.class).toString());
    });

    queryString("?", queryString -> {
      assertEquals("Optional.empty", queryString.toOptional(User.class).toString());
    });

    queryString("?foo=1&foo=2", queryString -> {
      assertEquals("Optional[1]", queryString.get("foo").toOptional(Long.class).toString());
    });

    queryString("?letter=A", queryString -> {
      assertEquals("Optional[A]",
          queryString.get("letter").toOptional(Letter.class).toString());
    });
  }

  @Test
  public void construnctorAndMixed() {
    queryString("?level=L1&children[0]level=L2", queryString -> {
      assertEquals("L1:[L2:[]]", queryString.to(Recursive.class).toString());
    });

    queryString("?foo=foo&bar=bar", queryString -> {
      assertEquals("foo:bar:0:null", queryString.to(Mixed.class).toString());
    });

    queryString("?foo=foo&bar=bar&number=5", queryString -> {
      assertEquals("foo:bar:5:null", queryString.to(Mixed.class).toString());
    });

    queryString("?foo=foo&bar=bar&values=v1&values=v2", queryString -> {
      assertEquals("foo:bar:0:[v1, v2]", queryString.to(Mixed.class).toString());
    });

    queryString("?id=userId", queryString -> {
      assertEquals("userId", queryString.to(UserCons.class).toString());
    });
  }

  private void queryString(String queryString, Consumer<QueryString> consumer) {
    consumer.accept(UrlParser.queryString(queryString));
  }

  public static <T extends Throwable> void assertMessage(Class<T> expectedType,
      Executable executable, String message) {
    T x = assertThrows(expectedType, executable);
    assertEquals(message, x.getMessage());
  }
}

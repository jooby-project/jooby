package org.jooby.issues;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue480b extends ServerFeature {

  public static class Member {
    String firstname;

    String lastname;

    public void setFirstname(final String firstname) {
      this.firstname = firstname;
    }

    public String getFirstname() {
      return firstname;
    }

    public void setLastname(final String lastname) {
      this.lastname = lastname;
    }

    public String getLastname() {
      return lastname;
    }

    @Override
    public String toString() {
      return firstname + " " + lastname;
    }
  }

  public static class Group {

    List<Member> members;

    public List<Member> members() {
      return members;
    }

    public Group members(final List<Member> members) {
      this.members = members;
      return this;
    }

    @Override
    public String toString() {
      return Optional.ofNullable(members).map(it -> it.toString()).orElse("[]");
    }

  }

  public static class Person {
    String name;

    List<Person> children = new ArrayList<>();

    public List<Person> getChildren() {
      return children;
    }

    public void setChildren(final List<Person> children) {
      this.children = children;
    }

    @Override
    public String toString() {
      return name + children;
    }
  }

  public static class RListOfStr {
    ListOfStr str;

    public void str(final ListOfStr str) {
      this.str = str;
    }

    public ListOfStr str() {
      return str;
    }

    @Override
    public String toString() {
      return str.toString();
    }
  }

  public static class ListOfStr {
    List<String> children = new ArrayList<>();

    public void children(final List<String> children) {
      this.children = children;
    }

    public List<String> children() {
      return children;
    }

    @Override
    public String toString() {
      return children.toString();
    }
  }

  public static class ListOfInt {
    List<Integer> children = new ArrayList<>();

    public void setChildren(final List<Integer> children) {
      this.children = children;
    }

    public List<Integer> getChildren() {
      return children;
    }

    @Override
    public String toString() {
      return children.toString();
    }
  }

  {
    get("/480", req -> {
      return req.params().toList(Member.class);
    });

    get("/480/nested", req -> {
      return req.params(Group.class);
    });

    get("/480/tree", req -> {
      return req.params(Person.class);
    });

    get("/480/listOfStr", req -> {
      return req.params(ListOfStr.class);
    });

    get("/480/rlistOfStr", req -> {
      return req.params(RListOfStr.class);
    });

    get("/480/listOfInt", req -> {
      return req.params(ListOfInt.class);
    });
  }

  @Test
  public void rootList() throws Exception {
    request()
        .get("/480?[0][firstname]=Pedro&[0][lastname]=PicaPiedra")
        .expect("[Pedro PicaPiedra]");

    request()
        .get(
            "/480?[0][firstname]=Pedro&[0][lastname]=PicaPiedra&[1][firstname]=Pablo&[1][lastname]=Marmol")
        .expect("[Pedro PicaPiedra, Pablo Marmol]");
  }

  @Test
  public void nestedList() throws Exception {
    request()
        .get("/480/nested?members[0][firstname]=Pedro&members[0][lastname]=PicaPiedra")
        .expect("[Pedro PicaPiedra]");

    request()
        .get(
            "/480/nested?members[0][firstname]=Pedro&members[0][lastname]=PicaPiedra&members[1][firstname]=Pablo&members[1][lastname]=Marmol")
        .expect("[Pedro PicaPiedra, Pablo Marmol]");

  }

  @Test
  public void skipUnknownPath() throws Exception {
    request()
        .get("/480/nested?unknown=x")
        .expect("[]");

    request()
        .get("/480/nested?members[0][firstname]=Pedro&members[0][lastname]=PicaPiedra&unknown=x")
        .expect("[Pedro PicaPiedra]");
  }

  @Test
  public void skipUnknownNestedPath() throws Exception {
    request()
        .get(
            "/480/nested?members[0][firstname]=Pedro&members[0][lastname]=PicaPiedra&members[0][unknown]=x")
        .expect("[Pedro PicaPiedra]");
  }

  @Test
  public void shouldTraverseTreeLike() throws Exception {
    request()
        .get("/480/tree?name=A&children[0][name]=B")
        .expect("A[B[]]");

    request()
        .get("/480/tree?name=A&children[0][name]=B&children[1][name]=C")
        .expect("A[B[], C[]]");
  }

  @Test
  public void shouldWorkWithListOfStr() throws Exception {
    request()
        .get("/480/listOfStr?children[0]=foo")
        .expect("[foo]");

    request()
        .get("/480/listOfStr?children[0]=foo&children[1]=bar")
        .expect("[foo, bar]");

    request()
        .get("/480/rlistOfStr?str[children][0]=foo&str[children][1]=bar")
        .expect("[foo, bar]");
  }

  @Test
  public void shouldGetItemsInOrder() throws Exception {
    request()
        .get("/480/listOfStr?children[1]=1&children[2]=2&children[0]=0")
        .expect("[0, 1, 2]");
  }

  @Test
  public void shouldWorkWithListInt() throws Exception {
    request()
        .get("/480/listOfInt?children[0]=1")
        .expect("[1]");

    request()
        .get("/480/listOfInt?children[0]=1&children[1]=2")
        .expect("[1, 2]");
  }

}

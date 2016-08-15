package scanner;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;

@Named("foo")
public class NamedFoo {

  @PostConstruct
  public void starting() {
    System.out.println("starting: " + getClass());
  }

  @PreDestroy
  public void stopping() {
    System.out.println("stopping: " + getClass());
  }
}

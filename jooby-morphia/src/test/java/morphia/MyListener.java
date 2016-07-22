package morphia;

import javax.inject.Inject;

import org.mongodb.morphia.annotations.PreLoad;

public class MyListener {

  private Service service;

  @Inject
  public MyListener(final Service service) {
    this.service = service;
  }

  @PreLoad void preLoad(final Beer object) {
    service.doSomething(object);
  }

}

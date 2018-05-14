package org.jooby.hbm.data;

import javax.inject.Inject;
import javax.persistence.PostLoad;

public class MemberListener {
  public static class Service {
    public String alias() {
      return "post-load";
    }
  }

  private Service service;

  @Inject
  public MemberListener(final Service service) {
    this.service = service;
  }

  @PostLoad
  public void onPostLoad(final Member member) {
    member.setAlias(service.alias());
  }
}

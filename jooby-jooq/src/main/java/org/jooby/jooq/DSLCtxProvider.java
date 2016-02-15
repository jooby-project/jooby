package org.jooby.jooq;

import javax.inject.Provider;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

class DSLCtxProvider implements Provider<DSLContext> {

  private Configuration conf;

  public DSLCtxProvider(final Configuration conf) {
    this.conf = conf;
  }

  @Override
  public DSLContext get() {
    return DSL.using(conf);
  }

}

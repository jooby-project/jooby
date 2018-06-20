package org.jooby;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Named;
import java.math.BigDecimal;

import static java.util.Objects.requireNonNull;

public class ModuleProvidesFeature extends ServerFeature {

  public static class M2 implements Module {
    @Override
    public void configure(Env env, Config conf, Binder binder) throws Throwable {
    }
    
    @Provides
    public BigDecimal magicNumber(@Named("m1.prop") final String property) {
      return new BigDecimal(42);
    }
  }
  
  @Path("/p")
  public static class ProvidesInject {
    
    private BigDecimal magicNumber;
    
    @Inject
    public ProvidesInject(BigDecimal magicNumber) {
      this.magicNumber = requireNonNull(magicNumber, "The magicNumber is required.");
    }
    
    @GET
    @Path("/property")
    public Object property() {
      return magicNumber;
    }
  }

  {

    // don't use application.conf
    use(ConfigFactory.empty());

    use(new ModulePropertiesFeature.M1());
  
    use(new M2());

    use(ProvidesInject.class);
  }
  
  @Test
  public void providesProperty() throws Exception {
    request()
        .get("/p/property")
        .expect("42");
  }
}

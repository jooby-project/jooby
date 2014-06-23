package jooby;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;

public class GuiceProofOfConcept {

  public static void main(final String[] args) {
    Injector injector = Guice.createInjector(binder -> {
    });

    injector.createChildInjector(new Module() {
      @Override
      public void configure(final Binder binder) {
        binder.bind(String.class).annotatedWith(Names.named("xxx")).toInstance("XXXX");
      }
    });

    ProofOfConcept proofOfConcept = injector.getInstance(ProofOfConcept.class);
    System.out.println(proofOfConcept);
  }

}

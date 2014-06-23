package jooby;

import javax.inject.Inject;
import javax.inject.Named;

public class ProofOfConcept {
  @Inject
  public ProofOfConcept(@Named("xxx") final String var) {
    System.out.println(var);
  }
}

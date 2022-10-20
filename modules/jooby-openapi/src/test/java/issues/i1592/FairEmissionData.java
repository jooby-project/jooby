/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i1592;

import java.util.List;

public class FairEmissionData {
  private List<Double> co2Emissions;

  public List<Double> getCo2Emissions() {
    return co2Emissions;
  }

  public void setCo2Emissions(List<Double> co2Emissions) {
    this.co2Emissions = co2Emissions;
  }
}

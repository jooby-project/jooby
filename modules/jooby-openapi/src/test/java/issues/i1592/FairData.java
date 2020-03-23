package issues.i1592;

public class FairData {
  private int baseYear;

  private int finalYear;

  private FairEmissionData annualEmissions;

  public int getBaseYear() {
    return baseYear;
  }

  public void setBaseYear(int baseYear) {
    this.baseYear = baseYear;
  }

  public int getFinalYear() {
    return finalYear;
  }

  public void setFinalYear(int finalYear) {
    this.finalYear = finalYear;
  }

  public FairEmissionData getAnnualEmissions() {
    return annualEmissions;
  }

  public void setAnnualEmissions(FairEmissionData annualEmissions) {
    this.annualEmissions = annualEmissions;
  }
}

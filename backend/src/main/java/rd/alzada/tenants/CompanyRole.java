package rd.alzada.tenants;

public enum CompanyRole {
  COMPANY_ADMIN,
  FINANCE,
  SALES_ADVISOR,
  BUYER;

  public static CompanyRole highest(Iterable<CompanyRole> roles) {
    CompanyRole selected = BUYER;
    for (CompanyRole role : roles) {
      if (rank(role) > rank(selected)) {
        selected = role;
      }
    }
    return selected;
  }

  public static int rank(CompanyRole role) {
    return switch (role) {
      case COMPANY_ADMIN -> 4;
      case FINANCE -> 3;
      case SALES_ADVISOR -> 2;
      case BUYER -> 1;
    };
  }
}

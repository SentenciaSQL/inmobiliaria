package rd.alzada.common;

public final class SecurityRoles {

  public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
  public static final String COMPANY_ADMIN = "COMPANY_ADMIN";
  public static final String SALES_ADVISOR = "SALES_ADVISOR";
  public static final String FINANCE = "FINANCE";
  public static final String BUYER = "BUYER";

  public static final String ONLY_ADMIN = "hasRole('COMPANY_ADMIN')";
  public static final String SALES = "hasAnyRole('COMPANY_ADMIN','SALES_ADVISOR')";
  public static final String FINANCE_ACCESS = "hasAnyRole('COMPANY_ADMIN','FINANCE')";
  public static final String STAFF = "hasAnyRole('COMPANY_ADMIN','SALES_ADVISOR','FINANCE')";
  public static final String CONTRACT_READ = "hasAnyRole('COMPANY_ADMIN','FINANCE','SALES_ADVISOR')";
  public static final String BUYER_ONLY = "hasRole('BUYER')";
  public static final String PLATFORM = "hasRole('PLATFORM_ADMIN')";

  private SecurityRoles() {}
}

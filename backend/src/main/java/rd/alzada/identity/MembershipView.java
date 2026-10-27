package rd.alzada.identity;

import java.util.UUID;

public record MembershipView(
    UUID id,
    UUID companyId,
    String companyName,
    String companySlug,
    String companyStatus,
    boolean hasLogo,
    String role) {}

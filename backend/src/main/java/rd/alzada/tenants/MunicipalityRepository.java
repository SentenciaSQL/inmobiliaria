package rd.alzada.tenants;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MunicipalityRepository extends JpaRepository<Municipality, UUID> {

  List<Municipality> findAllByOrderByProvinceCodeAscNameAsc();

  Optional<Municipality> findByIdAndProvinceCode(UUID id, String provinceCode);
}

package rd.alzada.tenants;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProvinceRepository extends JpaRepository<Province, String> {

  List<Province> findAllByOrderByNameAsc();
}

package rd.alzada.tenants;

import rd.alzada.common.ApiException;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationCatalog {

  private final ProvinceRepository provinces;
  private final MunicipalityRepository municipalities;
  private Map<String, Province> provinceByCode = Map.of();
  private Map<UUID, Municipality> municipalityById = Map.of();

  public LocationCatalog(ProvinceRepository provinces, MunicipalityRepository municipalities) {
    this.provinces = provinces;
    this.municipalities = municipalities;
  }

  @PostConstruct
  @Transactional(readOnly = true)
  public void load() {
    provinceByCode = provinces.findAllByOrderByNameAsc().stream()
        .collect(Collectors.toMap(Province::getCode, Function.identity()));
    municipalityById = municipalities.findAll().stream()
        .collect(Collectors.toMap(Municipality::getId, Function.identity()));
  }

  public List<Province> provinces() {
    return provinceByCode.values().stream().sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName())).toList();
  }

  public List<Municipality> municipalities() {
    return municipalityById.values().stream().sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName())).toList();
  }

  public void require(String provinceCode, UUID municipalityId) {
    if (provinceCode == null || !provinceByCode.containsKey(provinceCode)) {
      throw ApiException.badRequest("Seleccione una provincia válida");
    }
    if (municipalityId != null) {
      Municipality municipality = municipalityById.get(municipalityId);
      if (municipality == null || !municipality.getProvinceCode().equals(provinceCode)) {
        throw ApiException.badRequest("El municipio no pertenece a la provincia seleccionada");
      }
    }
  }

  public String provinceName(String code) {
    if (code == null) {
      return null;
    }
    Province province = provinceByCode.get(code);
    return province == null ? code : province.getName();
  }

  public String municipalityName(UUID id) {
    if (id == null) {
      return null;
    }
    Municipality municipality = municipalityById.get(id);
    return municipality == null ? null : municipality.getName();
  }
}

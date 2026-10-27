package rd.alzada.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final TypeReference<List<String>> TYPE = new TypeReference<>() {};

  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    try {
      return MAPPER.writeValueAsString(attribute == null ? List.of() : attribute);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("No se pudo guardar la lista");
    }
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return new ArrayList<>();
    }
    try {
      return new ArrayList<>(MAPPER.readValue(dbData, TYPE));
    } catch (JsonProcessingException exception) {
      return new ArrayList<>();
    }
  }
}

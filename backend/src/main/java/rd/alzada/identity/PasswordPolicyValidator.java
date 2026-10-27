package rd.alzada.identity;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordPolicyValidator implements ConstraintValidator<PasswordPolicy, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    if (value.length() < 10 || value.length() > 80) {
      return false;
    }
    boolean letter = value.chars().anyMatch(Character::isLetter);
    boolean digit = value.chars().anyMatch(Character::isDigit);
    return letter && digit;
  }
}

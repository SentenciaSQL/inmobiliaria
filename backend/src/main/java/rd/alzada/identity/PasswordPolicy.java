package rd.alzada.identity;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = PasswordPolicyValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordPolicy {

  String message() default "La contraseña debe tener al menos 10 caracteres, una letra y un número";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}

package rd.alzada.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import rd.alzada.identity.JwtService;
import rd.alzada.identity.UserAccountService;
import rd.alzada.tenants.CompanyRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthFilter jwtAuthFilter,
      TenantContextFilter tenantContextFilter,
      ObjectMapper objectMapper,
      AppProperties properties) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource(properties)))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(errors -> errors
            .authenticationEntryPoint((request, response, exception) -> write(response, objectMapper, 401, "Debe iniciar sesión"))
            .accessDeniedHandler((request, response, exception) -> write(response, objectMapper, 403, "No tiene permiso para esta operación")))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/api/v1/public/**", "/sitemap.xml").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
            .requestMatchers("/api/v1/platform/**").hasRole("PLATFORM_ADMIN")
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(tenantContextFilter, JwtAuthFilter.class);
    return http.build();
  }

  @Bean
  public JwtAuthFilter jwtAuthFilter(JwtService jwtService, UserAccountService accounts) {
    return new JwtAuthFilter(jwtService, accounts);
  }

  @Bean
  public TenantContextFilter tenantContextFilter(CompanyRepository companies, ObjectMapper objectMapper) {
    return new TenantContextFilter(companies, objectMapper);
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(properties.corsOriginList());
    configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type", "X-Company-Id", "X-Company-Role"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  static void write(HttpServletResponse response, ObjectMapper mapper, int status, String detail) {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", status);
    body.put("detail", detail);
    body.put("code", status == 401 ? "UNAUTHORIZED" : "FORBIDDEN");
    try {
      mapper.writeValue(response.getOutputStream(), body);
    } catch (Exception ignored) {
      // La respuesta de error ya no puede escribirse.
    }
  }
}

package com.opntus;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
 @Bean SecurityFilterChain security(HttpSecurity http)throws Exception {
  http.authorizeHttpRequests(a->a.requestMatchers("/api/opntus").authenticated().anyRequest().permitAll())
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,x)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"error\":\"Please sign in with email and password.\"}");}))
   .headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; media-src 'self' blob:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")))
   .logout(l->l.logoutUrl("/auth/logout").logoutSuccessHandler((req,res,a)->res.setStatus(204)).invalidateHttpSession(true).deleteCookies("JSESSIONID"));
  http.addFilterBefore(new SessionExpiryFilter(),org.springframework.security.web.context.SecurityContextHolderFilter.class);
  return http.build();
 }
}

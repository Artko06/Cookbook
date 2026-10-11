package org.example.cookbook.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcherEntry;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @SuppressWarnings("unchecked")
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        RequestMatcher api = request -> request.getRequestURI().startsWith("/api/");
        DelegatingAuthenticationEntryPoint entryPoint = new DelegatingAuthenticationEntryPoint(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new RequestMatcherEntry<AuthenticationEntryPoint>(
                        api, new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/recipes/my", "/api/v1/recipes/my/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .requestMatchers("/recipes/new", "/recipes/my", "/ingredients/new").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/", "/recipes", "/recipes/{id}", "/users/**",
                                "/ingredients", "/ingredients/{id}").permitAll()
                        .requestMatchers("/register", "/login", "/error",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint))
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

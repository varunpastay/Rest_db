package com.restro.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Only two roles exist: CUSTOMER (no login at all - every /menu, /cart,
 * /order, /t/** route is public) and OWNER (single login, guards everything
 * under /owner/**, which now covers what used to be split across
 * admin/kitchen/counter).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/", "/t/**", "/menu/**", "/cart/**", "/order/**", "/assistance/**",
                        "/assets/**", "/images/**", "/css/**", "/js/**", "/webjars/**",
                        "/ws/**", "/owner/login", "/error"
                ).permitAll()
                .requestMatchers("/owner/**").hasRole("OWNER")
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/owner/login")
                .loginProcessingUrl("/owner/login")
                .defaultSuccessUrl("/owner/dashboard", true)
                .failureUrl("/owner/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/owner/logout")
                .logoutSuccessUrl("/owner/login?logout")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation().changeSessionId()
                .maximumSessions(-1)
            )
            // Customer AJAX endpoints (cart, order status polling, assistance requests) have no
            // authenticated session to protect, so they're exempted from CSRF entirely. The owner
            // dashboard keeps CSRF protection - its <form> POSTs get a token automatically via
            // thymeleaf-extras-springsecurity6, and its fetch()-based POSTs (accept/serve/pay/
            // cancel/resolve, in owner-dashboard.js) attach the token by hand from the <meta> tags
            // in fragments/head.html - see /assets/js/csrf.js.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/cart/**", "/order/place", "/order/status/**", "/assistance/**", "/ws/**"));

        return http.build();
    }
}

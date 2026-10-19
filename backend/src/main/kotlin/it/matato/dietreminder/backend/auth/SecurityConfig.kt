package it.matato.dietreminder.backend.auth

import com.fasterxml.jackson.databind.ObjectMapper
import it.matato.dietreminder.common.model.ApiErrorDto
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.security.MessageDigest
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.OncePerRequestFilter

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class SecurityConfig {
    @Bean
    fun security(http: HttpSecurity, auth: AuthService, mapper: ObjectMapper): SecurityFilterChain {
        val filter = object : OncePerRequestFilter() {
            override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
                val unsafe = request.method !in listOf("GET", "HEAD", "OPTIONS")
                val publicWebLogin = request.servletPath in listOf("/api/web/auth/login", "/api/web/auth/register", "/api/web/auth/google")
                if (request.servletPath.startsWith("/api/web/") && unsafe && request.getHeader("X-DietReminder-Web") != "1") {
                    response.status = 403
                    response.contentType = "application/json"
                    mapper.writeValue(response.outputStream, ApiErrorDto("INVALID_CSRF", "A same-origin web request is required"))
                    return
                }
                val header = request.getHeader("Authorization")
                val bearer = header?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
                val cookie = request.cookies?.firstOrNull { it.name == WEB_SESSION_COOKIE }?.value?.takeUnless { publicWebLogin }
                if (bearer == null && cookie != null && unsafe && !MessageDigest.isEqual(webCsrf(cookie).toByteArray(), request.getHeader("X-CSRF-Token").orEmpty().toByteArray())) {
                    response.status = 403
                    response.contentType = "application/json"
                    mapper.writeValue(response.outputStream, ApiErrorDto("INVALID_CSRF", "Reload the page and try again"))
                    return
                }
                val token = bearer ?: cookie
                if (token != null) {
                    auth.authenticate(token)?.let { account ->
                        val denial = AccountAccess.denial(account, request.servletPath, request.method)
                        if (denial != null) {
                            response.status = 403
                            response.contentType = "application/json"
                            mapper.writeValue(response.outputStream, denial)
                            return
                        }
                        val context = SecurityContextHolder.createEmptyContext()
                        context.authentication = UsernamePasswordAuthenticationToken(account, null, listOf(SimpleGrantedAuthority("ROLE_${account.role}")))
                        SecurityContextHolder.setContext(context)
                    }
                }
                chain.doFilter(request, response)
            }
        }
        return http
            .csrf { it.disable() } // Cookie requests use the session-bound CSRF check above; bearer clients do not need it.
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .requestCache { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/api/admin/**").hasRole("ADMIN")
                it.requestMatchers(HttpMethod.GET, "/api/app/version-policy", "/api/app/shopping-categories", "/api/auth/providers", "/api/google/drive/callback").permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/auth/google", "/api/auth/google/challenge", "/api/web/auth/login", "/api/web/auth/register", "/api/web/auth/google").permitAll()
                it.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                it.requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ ->
                    response.status = 401
                    response.setHeader("WWW-Authenticate", "Bearer")
                    response.contentType = "application/json"
                    mapper.writeValue(response.outputStream, ApiErrorDto("UNAUTHENTICATED", "Sign in to access your account"))
                }
                it.accessDeniedHandler { _, response, _ ->
                    response.status = 403
                    response.contentType = "application/json"
                    mapper.writeValue(response.outputStream, ApiErrorDto("FORBIDDEN", "Access denied"))
                }
            }
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}

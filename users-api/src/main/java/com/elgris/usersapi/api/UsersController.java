    package com.elgris.usersapi.api;

    import com.elgris.usersapi.models.User;
    import com.elgris.usersapi.repository.UserRepository;
    import io.github.resilience4j.circuitbreaker.CircuitBreaker;
    import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
    import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
    import io.jsonwebtoken.Claims;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.security.access.AccessDeniedException;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.http.ResponseEntity;
    import org.springframework.http.HttpStatus;
    import java.util.Collections;
    import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

    import javax.servlet.http.HttpServletRequest;
    import java.util.LinkedList;
    import java.util.List;

    @RestController()
    @RequestMapping("/users")
    public class UsersController {
        @Autowired
        private UserRepository userRepository;

        private final CircuitBreaker usersServiceCircuitBreaker;

        public UsersController() {
            // Crear instancia de forma manual
            CircuitBreakerConfig config = CircuitBreakerConfig.ofDefaults();
            CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
            this.usersServiceCircuitBreaker = registry.circuitBreaker("usersService");
        }

        @RequestMapping(value = "/", method = RequestMethod.GET)
        public List<User> getUsers() {
            try {
                return usersServiceCircuitBreaker.executeSupplier(() -> {
                    List<User> response = new LinkedList<>();
                    userRepository.findAll().forEach(response::add);
                    return response;
                });
            } catch (CallNotPermittedException e) {
                // Fallback: el circuito está abierto
                return Collections.emptyList();
            }
        }

        @RequestMapping(value = "/{username}",  method = RequestMethod.GET)
        public User getUser(HttpServletRequest request, @PathVariable("username") String username) {

            Object requestAttribute = request.getAttribute("claims");
            if((requestAttribute == null) || !(requestAttribute instanceof Claims)){
                throw new RuntimeException("Did not receive required data from JWT token");
            }

            Claims claims = (Claims) requestAttribute;

            if (!username.equalsIgnoreCase((String)claims.get("username"))) {
                throw new AccessDeniedException("No access for requested entity");
            }

            return userRepository.findOneByUsername(username);
        }

    }

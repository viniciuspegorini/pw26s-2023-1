package br.edu.utfpr.pb.pw26s.server.security.social;

import br.edu.utfpr.pb.pw26s.server.model.AuthProvider;
import br.edu.utfpr.pb.pw26s.server.model.Authority;
import br.edu.utfpr.pb.pw26s.server.model.User;
import br.edu.utfpr.pb.pw26s.server.repository.AuthorityRepository;
import br.edu.utfpr.pb.pw26s.server.repository.UserRepository;
import br.edu.utfpr.pb.pw26s.server.security.SecurityConstants;
import br.edu.utfpr.pb.pw26s.server.security.dto.AuthenticationResponse;
import br.edu.utfpr.pb.pw26s.server.security.dto.UserResponseDTO;
import br.edu.utfpr.pb.pw26s.server.service.AuthService;
import br.edu.utfpr.pb.pw26s.server.service.UserService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashSet;

@RestController
@RequestMapping("auth-social")
public class AuthController {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final AuthService authService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;

    public AuthController(GoogleTokenVerifier googleTokenVerifier, AuthService authService,
                          UserService userService,
                          UserRepository userRepository,
                          AuthorityRepository authorityRepository) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.authService = authService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
    }

    @PostMapping
    public ResponseEntity<AuthenticationResponse> auth(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }
}

package com.moople.gitpals.MainApplication.Controller.API;

import com.moople.gitpals.MainApplication.Configuration.GitPalsUserDetails;
import com.moople.gitpals.MainApplication.Configuration.JWTUtil;
import com.moople.gitpals.MainApplication.Model.AuthRequest;
import com.moople.gitpals.MainApplication.Model.AuthResponse;
import com.moople.gitpals.MainApplication.Model.Response;
import com.moople.gitpals.MainApplication.Model.User;
import com.moople.gitpals.MainApplication.Service.Data;
import com.moople.gitpals.MainApplication.Service.Encrypt;
import com.moople.gitpals.MainApplication.Service.UserInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/auth")
public class AuthAPIController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private GitPalsUserDetails userDetailsService;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private UserInterface userInterface;

    /**
     * This function checks a request sent by a user and creates a jwt token for the user
     *
     * @param request contains username and password (key) so the system can identify the user
     * @return jwt token
     */
    @PostMapping("/login")
    public ResponseEntity createToken(@RequestBody AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), Encrypt.MD5(request.getPassword()))
            );
        } catch (Exception e) {
            return ResponseEntity.ok(Response.FAILED);
        }

        final UserDetails userDetails = userDetailsService
                .loadUserByUsername(request.getUsername());

        final String jwt = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(jwt));
    }

    /**
     * This function finds a user by jwt token and returns user object
     *
     * @param map is a map which contains token sent by the user
     * @return user object
     */
    @PostMapping("/get")
    public User getUser(@RequestBody Map<String, String> map) {
        try {
            String token = map.get("token");
            return userInterface.findByUsername(jwtUtil.extractUsername(token));
        } catch (Exception e) {
            return Data.EMPTY_USER;
        }
    }
}
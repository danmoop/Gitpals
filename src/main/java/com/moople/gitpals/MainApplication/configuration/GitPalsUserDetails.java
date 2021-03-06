package com.moople.gitpals.MainApplication.configuration;


import com.moople.gitpals.MainApplication.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class GitPalsUserDetails implements UserDetailsService {

    @Autowired
    private UserService userService;

    /**
     * @param s is a username of a user that is trying to authenticate
     * @return user's credentials if the username is present in a database
     * @throws UsernameNotFoundException if user is not in the database
     */
    @Override
    public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        com.moople.gitpals.MainApplication.model.User user = userService.findByUsername(s);

        if (user == null) {
            throw new UsernameNotFoundException("No user with username " + s);
        }

        return new User(user.getUsername(), user.getMobileAuthPassword(), new ArrayList<>());
    }
}
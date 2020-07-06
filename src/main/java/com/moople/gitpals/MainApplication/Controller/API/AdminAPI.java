package com.moople.gitpals.MainApplication.Controller.API;

import com.moople.gitpals.MainApplication.Model.*;
import com.moople.gitpals.MainApplication.Service.ForumInterface;
import com.moople.gitpals.MainApplication.Service.KeyStorageInterface;
import com.moople.gitpals.MainApplication.Service.ProjectInterface;
import com.moople.gitpals.MainApplication.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminAPI {

    @Autowired
    private UserService userService;

    @Autowired
    private ForumInterface forumInterface;

    @Autowired
    private ProjectInterface projectInterface;

    @Autowired
    private KeyStorageInterface keyStorageInterface;

    private final String ADMIN_NAME = "danmoop";

    /**
     * This function is only for admin
     * It performs some manipulations with user DB
     * When User.java gets new parameter, this function adds it to all the users in DB
     *
     * @param admin is an admin authentication
     * @return a response whether changes were made
     */
    @GetMapping("/set")
    public Response setter(Principal admin) {
        if (admin == null || !admin.getName().equals(ADMIN_NAME)) {
            return Response.FAILED;
        }

        userService.findAll().forEach(user -> {
            user.setDialogs(new HashMap<>());
            userService.save(user);
        });

        return Response.OK;
    }


    /**
     * This function serves as a backup for projects
     *
     * @param admin is an admin's authentication
     * @return a list of projects
     */
    @GetMapping(value = "/backupProjects", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Project> backupProjects(Principal admin) {
        if (admin == null || !admin.getName().equals(ADMIN_NAME)) {
            return new ArrayList<>();
        }

        return projectInterface.findAll();
    }

    /**
     * This function serves as a backup for projects
     *
     * @param admin is an admin's authentication
     * @return a list of users
     */
    @GetMapping(value = "/backupUsers", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<User> backupUsers(Principal admin) {
        if (admin == null || !admin.getName().equals(ADMIN_NAME)) {
            return new ArrayList<>();
        }

        return userService.findAll();
    }


    /**
     * This function serves as a backup for projects
     *
     * @param admin is an admin's authentication
     * @return a list of forum posts
     */
    @GetMapping(value = "/backupForum", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ForumPost> backupForum(Principal admin) {
        if (admin == null || !admin.getName().equals(ADMIN_NAME)) {
            return new ArrayList<>();
        }

        return forumInterface.findAll();
    }


    /**
     * This function serves as a backup for projects
     *
     * @param admin is an admin's authentication
     * @return a list of keys
     */
    @GetMapping(value = "/backupKeys", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<KeyStorage> backupKeys(Principal admin) {
        if (admin == null || !admin.getName().equals(ADMIN_NAME)) {
            return new ArrayList<>();
        }

        return keyStorageInterface.findAll();
    }
}
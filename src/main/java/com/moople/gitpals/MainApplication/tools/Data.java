package com.moople.gitpals.MainApplication.tools;

import com.moople.gitpals.MainApplication.model.Comment;
import com.moople.gitpals.MainApplication.model.ForumPost;
import com.moople.gitpals.MainApplication.model.Project;
import com.moople.gitpals.MainApplication.model.User;

public class Data {
    public static final User EMPTY_USER = new User();
    public static final Project EMPTY_PROJECT = new Project();
    public static final Comment EMPTY_COMMENT = new Comment();
    public static final ForumPost EMPTY_FORUM_POST = new ForumPost();

    // This key is different from a real one used on gitpals.com
    public static final String ENCRYPTION_KEY = "5da99c95fa5c10a2cda3e4fef53a85cc2dc833a1";
}

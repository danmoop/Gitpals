package com.moople.gitpals.MainApplication.controller.api;

import com.moople.gitpals.MainApplication.configuration.JWTUtil;
import com.moople.gitpals.MainApplication.model.Comment;
import com.moople.gitpals.MainApplication.model.ForumPost;
import com.moople.gitpals.MainApplication.model.Response;
import com.moople.gitpals.MainApplication.model.User;
import com.moople.gitpals.MainApplication.service.ForumService;
import com.moople.gitpals.MainApplication.service.UserService;
import com.moople.gitpals.MainApplication.tools.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/forum")
public class ForumAPIController {

    @Autowired
    private ForumService forumService;

    @Autowired
    private UserService userService;

    @Autowired
    private JWTUtil jwtUtil;

    /**
     * @return all forum posts fetched from the database
     */
    @GetMapping(value = "/getAll", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ForumPost> getAll() {
        return forumService.findAll();
    }

    /**
     * This function returns a forum post object obtained by its key
     *
     * @param key is a unique forum post's key
     * @return a forum post object
     */
    @GetMapping(value = "/getForumPostById/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ForumPost getForumPostById(@PathVariable String key) {
        return forumService.findByKey(key);
    }

    /**
     * This function adds a user to a forum post's view set if user has not yet seen the post
     *
     * @param data contains information about the user (jwt) and forum post key
     * @return response if user has been added to a view set successfully
     */
    @PostMapping(value = "/addUserToViewSet", produces = MediaType.APPLICATION_JSON_VALUE)
    public Response addUserToViewSet(@RequestBody Map<String, String> data) {
        String jwt = data.get("jwt");
        String postKey = data.get("postKey");

        User user = userService.findByUsername(jwtUtil.extractUsername(jwt));
        ForumPost post = forumService.findByKey(postKey);

        if (user == null || post == null) {
            return Response.FAILED;
        }

        post.getViewSet().add(user.getUsername());
        forumService.save(post);

        return Response.OK;
    }

    /**
     * This request is handled when user submits their forum post and it is added to forum
     *
     * @param data is information sent by a user (contains post's title & description)
     * @return response if post has been added successfully
     */
    @PostMapping(value = "/addForumPost", produces = MediaType.APPLICATION_JSON_VALUE)
    public ForumPost addForumPost(@RequestBody Map<String, String> data) {
        String jwt = data.get("jwt");
        String title = data.get("title");
        String description = data.get("description");
        User user = userService.findByUsername(jwtUtil.extractUsername(jwt));

        if (title.trim().equals("") || description.trim().equals("") || user == null || user.isBanned()) {
            return Data.EMPTY_FORUM_POST;
        }

        ForumPost post = new ForumPost(user.getUsername(), title, description);
        post.getViewSet().add(user.getUsername());
        forumService.save(post);

        return post;
    }

    /**
     * This function removes the forum post from the forum
     *
     * @param data contains information sent by the user (contains forum post key & jwt)
     * @return response if post has been deleted successfully
     */
    @PostMapping(value = "/deleteForumPost", produces = MediaType.APPLICATION_JSON_VALUE)
    public Response deleteForumPost(@RequestBody Map<String, String> data) {
        String jwt = data.get("jwt");
        String postKey = data.get("postKey");

        User user = userService.findByUsername(jwtUtil.extractUsername(jwt));
        ForumPost post = forumService.findByKey(postKey);

        if (user == null || post == null) {
            return Response.FAILED;
        }

        if (user.isBanned()) {
            return Response.YOU_ARE_BANNED;
        }

        if (user.getUsername().equals(post.getAuthor())) {
            forumService.delete(post);

            return Response.OK;
        }

        return Response.FAILED;
    }

    /**
     * This request is handled when user sends their comments to a forum post
     * A comment will be added and changes will be saved to database
     *
     * @param data is information sent by a user (contains comment text and post's key so server could find it)
     * @return response if comment has been added successfully
     */
    @PostMapping(value = "/addComment", produces = MediaType.APPLICATION_JSON_VALUE)
    public Comment addComment(@RequestBody Map<String, String> data) {
        String jwt = data.get("jwt");
        String commentText = data.get("text");
        String postKey = data.get("postKey");
        String author = data.get("author");

        User user = userService.findByUsername(jwtUtil.extractUsername(jwt));
        ForumPost post = forumService.findByKey(postKey);

        if (user == null || user.isBanned()) {
            return Data.EMPTY_COMMENT;
        }

        if (user.getUsername().equals(author)) {
            Comment comment = new Comment(author, commentText);
            forumService.addComment(post, user.getUsername(), comment);

            return comment;
        }

        return Data.EMPTY_COMMENT;
    }

    /**
     * This function edits a comment in a forum post (changes comment's context & marks it as edited)
     *
     * @param data contains information sent by a user about user (jwt), post and comment
     * @return response if comment has been edited successfully
     */
    @PostMapping(value = "/editComment", produces = MediaType.APPLICATION_JSON_VALUE)
    public Response editForumPostComment(@RequestBody Map<String, String> data) {
        String jwt = data.get("jwt");
        String postKey = data.get("postKey");
        String commentKey = data.get("commentKey");
        String commentText = data.get("commentText");

        User user = userService.findByUsername(jwtUtil.extractUsername(jwt));
        ForumPost post = forumService.findByKey(postKey);

        if (user == null || post == null) {
            return Response.FAILED;
        }

        if (user.isBanned()) {
            return Response.YOU_ARE_BANNED;
        }

        forumService.editComment(post, user.getUsername(), commentKey, commentText);

        return Response.OK;
    }

    /**
     * This function deletes a comment added on forum post
     *
     * @param data contains information sent by a user about user (jwt), post and comment
     * @return response if comment has been removed successfully
     */
    @PostMapping(value = "/deleteComment", produces = MediaType.APPLICATION_JSON_VALUE)
    public Response deleteForumPostComment(@RequestBody Map<String, String> data) {
        String jwt = data.get("jwt");
        String postKey = data.get("postKey");
        String commentKey = data.get("commentKey");

        User user = userService.findByUsername(jwtUtil.extractUsername(jwt));
        ForumPost post = forumService.findByKey(postKey);

        if (user == null || post == null) {
            return Response.FAILED;
        }

        if (user.isBanned()) {
            return Response.YOU_ARE_BANNED;
        }

        if (forumService.deleteComment(post, user.getUsername(), commentKey)) {
            return Response.OK;
        }

        return Response.FAILED;
    }
}
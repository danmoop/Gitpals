package com.moople.gitpals.MainApplication.controller;

import com.moople.gitpals.MainApplication.model.Comment;
import com.moople.gitpals.MainApplication.model.ForumPost;
import com.moople.gitpals.MainApplication.model.User;
import com.moople.gitpals.MainApplication.service.ForumService;
import com.moople.gitpals.MainApplication.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
public class ForumController {

    @Autowired
    private ForumService forumService;

    @Autowired
    private UserService userService;

    /**
     * This request is handled when user wants to open forum page
     *
     * @return forum page
     */
    @GetMapping("/forum")
    public String forumPage(Principal auth, Model model) {
        if (auth != null) {
            User userDB = userService.findByUsername(auth.getName());

            if (userDB.isBanned()) {
                return "sections/users/banned";
            }
        }

        List<ForumPost> posts = forumService.findAll();

        model.addAttribute("posts", posts);
        model.addAttribute("user", auth);

        return "sections/forum/forum";
    }

    /**
     * This request is handled when user wants to open a posts's page
     *
     * @param key is a post's key which is taken from an address field
     * @return post page
     */
    @GetMapping("/forum/post/{key}")
    public String getForumPost(@PathVariable String key, Principal auth, Model model) {
        if (auth != null) {
            User userDB = userService.findByUsername(auth.getName());

            if (userDB.isBanned()) {
                return "sections/users/banned";
            }
        }

        ForumPost post = forumService.findByKey(key);

        if (post == null) {
            return "redirect:/forum";
        }

        if (auth != null && !post.getViewSet().contains(auth.getName())) {
            post.getViewSet().add(auth.getName());
            forumService.save(post);
        }

        model.addAttribute("userDB", userService.findByUsername(auth != null ? auth.getName() : null));
        model.addAttribute("post", post);

        return "sections/forum/viewForumPost";
    }

    /**
     * This request is handled when user submits their forum post and it is added to forum
     *
     * @param auth    is assigned automatically using spring
     * @param content is taken from html input, it is post's description
     * @return forum post page
     */
    @PostMapping("/addForumPost")
    public String addForumPost(Principal auth, @RequestParam String title, @RequestParam String content) {
        if (auth == null) {
            return "redirect:/";
        } else {
            User userDB = userService.findByUsername(auth.getName());

            if (userDB.isBanned()) {
                return "sections/users/banned";
            }
        }

        ForumPost post = new ForumPost(auth.getName(), title, content);
        forumService.save(post);

        return "redirect:/forum/post/" + post.getKey();
    }

    /**
     * This request is handled when user sends their comments to a forum post
     * A comment will be added and changes will be saved to database
     *
     * @param auth        is a user session, assigned automatically
     * @param commentText is a comment text, taken from html input field
     * @param postKey     is a forum post's key, taken from a hidden html input field, assigned by thymeleaf
     * @return forum post page
     */
    @PostMapping("/addCommentToPost")
    public String addCommentToPost(Principal auth, @RequestParam String commentText, @RequestParam String postKey) {
        if (auth == null) {
            return "redirect:/";
        } else {
            User userDB = userService.findByUsername(auth.getName());

            if (userDB.isBanned()) {
                return "sections/users/banned";
            }
        }

        ForumPost post = forumService.findByKey(postKey);

        if (post == null) {
            return "redirect:/forum";
        }

        Comment comment = new Comment(auth.getName(), commentText);

        forumService.addComment(post, auth.getName(), comment);

        return "redirect:/forum/post/" + postKey;
    }

    /**
     * This function removed a forum post from the database by key
     *
     * @param auth is a forum post's author
     * @param key  is a forum post's key, it is used to find a post among many others
     * @return home page
     */
    @PostMapping("/deleteForumPost")
    public String deleteForumPost(Principal auth, @RequestParam String key) {
        if (auth != null) {
            User userDB = userService.findByUsername(auth.getName());

            if (userDB.isBanned()) {
                return "sections/users/banned";
            }
        }

        ForumPost post = forumService.findByKey(key);

        if (auth != null && post != null && auth.getName().equals(post.getAuthor())) {
            forumService.delete(post);
        }

        return "redirect:/forum";
    }

    /**
     * This function deletes a comment added on forum post
     *
     * @param auth       is comment's author
     * @param postKey    is a forum post's key
     * @param commentKey is a post's comment key, so it would be easier to find it
     * @return to the forum page
     */
    @PostMapping("/deleteForumPostComment")
    public String deleteForumPostComment(Principal auth, @RequestParam String postKey, @RequestParam String commentKey) {
        if (auth != null) {
            User userDB = userService.findByUsername(auth.getName());

            if (userDB.isBanned()) {
                return "sections/users/banned";
            }
        }

        ForumPost post = forumService.findByKey(postKey);
        if (post == null || auth == null) {
            return "redirect:/";
        }

        if (forumService.deleteComment(post, auth.getName(), commentKey)) {
            return "redirect:/forum/post/" + postKey;
        }

        return "redirect:/";
    }

    /**
     * This function edits a comment in a forum post (changes comment's context & marks it as edited)
     *
     * @param auth         is an author's authentication
     * @param forumPostKey is forum post's key required to find a forum post in the database
     * @param text         is a new text that will be set to a comment
     * @param commentKey   is a comment key required to find a comment in a list of comments added to a post
     * @return forum post page with edited comment contents
     */
    @PostMapping("/editForumPostComment")
    public String editComment(Principal auth, @RequestParam String forumPostKey, @RequestParam String text, @RequestParam String commentKey) {
        if (auth != null) {
            User userDB = userService.findByUsername(auth.getName());

            if (userDB.isBanned()) {
                return "sections/users/banned";
            }
        }

        ForumPost post = forumService.findByKey(forumPostKey);

        if (auth == null || post == null) {
            return "redirect:/";
        }

        forumService.editComment(post, auth.getName(), commentKey, text);

        return "redirect:/forum/post/" + forumPostKey;
    }
}
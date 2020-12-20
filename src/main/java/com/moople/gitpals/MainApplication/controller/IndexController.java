package com.moople.gitpals.MainApplication.controller;

import com.moople.gitpals.MainApplication.model.*;
import com.moople.gitpals.MainApplication.repository.ForumRepository;
import com.moople.gitpals.MainApplication.repository.GlobalMessageRepository;
import com.moople.gitpals.MainApplication.repository.KeyStorageRepository;
import com.moople.gitpals.MainApplication.repository.ProjectRepository;
import com.moople.gitpals.MainApplication.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.*;

@Controller
public class IndexController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ForumRepository forumRepository;

    @Autowired
    private KeyStorageRepository keyStorageRepository;

    @Autowired
    private GlobalMessageRepository globalMessageRepository;

    private final long ONE_DAY = 1000 * 86400;
    private final int PROJECTS_PER_PAGE = 20;

    @GetMapping("/")
    public String index() {
        return "redirect:/page/1";
    }

    /**
     * This request is handled when user opens index page
     * Add attributes about user and later display them on the page
     *
     * @return html index page with a list of projects and TECHS
     */
    @GetMapping("/page/{page}")
    public String indexPage(OAuth2Authentication auth, Model model, RedirectAttributes redirectAttributes, @PathVariable(value = "page", required = false) int page) {

        int numberOfPages = projectRepository.findAll().size() / PROJECTS_PER_PAGE;
        numberOfPages = numberOfPages == 0 ? 1 : numberOfPages;

        if (projectRepository.findAll().size() - numberOfPages * PROJECTS_PER_PAGE > 0) {
            numberOfPages += 1;
        }

        // Check if the user is not trying to open a page, which doesn't exist
        if (page > numberOfPages) {
            return "redirect:/page/" + numberOfPages;
        } else if (page < 1) {
            return "redirect:/page/1";
        }

        // If we are logged in, display information about us on the index page
        if (auth != null) {
            LinkedHashMap<String, Object> properties = (LinkedHashMap<String, Object>) auth.getUserAuthentication().getDetails();

            // Extract information about the user from their GitHub account
            String email = properties.get("email") == null ? null : properties.get("email").toString();
            String country = properties.get("location") == null ? null : properties.get("location").toString();
            String bio = properties.get("bio") == null ? null : properties.get("bio").toString();

            /*
             *  When authentication exists, however, there is no such user in the database,
             *  it means that this user has just logged in for the first time
             */
            if (userService.findByUsername(auth.getName()) == null) {
                User newRegisteredUser = new User(auth.getName(), "https://github.com/" + auth.getName(), email, country, bio);

                userService.save(newRegisteredUser);
                keyStorageRepository.save(new KeyStorage(auth.getName()));

                redirectAttributes.addFlashAttribute("message", "You have just registered! Fill in the information about yourself - choose skills you know on this page!");
                return "redirect:/dashboard";
            }

            User userDB = userService.findByUsername(auth.getName());

            if (userDB.isBanned()) {
                return "sections/users/banned";
            } else if (userDB.getSkillList().size() == 0) {
                redirectAttributes.addFlashAttribute("error", "You should have at least one skill!");
                return "redirect:/dashboard";
            }
            /*
            // TODO: uncomment when mobile version of GitPals is finished
            else if (userDB.getMobileAuthPassword().equals("")) {
                redirectAttributes.addFlashAttribute("error", "You should set up your mobile app auth password!");
                return "redirect:/dashboard";
            }*/

            checkIfDataHasChanged(userDB, properties);

            model.addAttribute("userDB", userDB);
            model.addAttribute("unreadMessages", countUnreadMessages(userDB));
        }

        List<Project> allProjects = projectRepository.findAll();
        int projectsAmount = allProjects.size();

        List<Project> projects;

        if (projectsAmount <= PROJECTS_PER_PAGE) {
            projects = allProjects;
            Collections.reverse(projects);
        } else {
            projects = new ArrayList<>();

            int start = projectsAmount - 1 - (PROJECTS_PER_PAGE * (page - 1));
            int end = projectsAmount - (PROJECTS_PER_PAGE * page);
            end = Math.max(end, 0);

            for (int i = start; i >= end; i--) {
                projects.add(allProjects.get(i));
            }
        }

        List<GlobalMessage> globalMessages = globalMessageRepository.findAll();
        if (globalMessages.size() != 0) {
            model.addAttribute("globalMessage", globalMessages.get(0));
        }

        model.addAttribute("projects", projects);
        model.addAttribute("pagesLength", numberOfPages);
        model.addAttribute("page", page);

        return "sections/users/index";
    }

    /**
     * This request is handled when user wants to log out
     * Session will be cleared
     *
     * @return html index page with logged-out user
     */
    @GetMapping("/signout")
    public String logout(HttpSession httpSession) {
        httpSession.invalidate();
        return "redirect:/";
    }

    /**
     * This function checks if data in user's GitHub profile has changed
     * If so, data will also change in the user's GitPals profile
     *
     * @param userDB     is a user object from the database
     * @param properties is information extracted from GitHub profile
     */
    private void checkIfDataHasChanged(User userDB, LinkedHashMap<String, Object> properties) {
        boolean shouldSaveChanges = false;

        // Email
        if (properties.get("email") == null) {
            if (userDB.getEmail() != null) {
                userDB.setEmail(null);
                shouldSaveChanges = true;
            }
        } else {
            if (userDB.getEmail() == null || !userDB.getEmail().equals(properties.get("email").toString())) {
                userDB.setEmail(properties.get("email").toString());
                shouldSaveChanges = true;
            }
        }

        // Location
        if (properties.get("location") == null) {
            if (userDB.getCountry() != null) {
                userDB.setCountry(null);
                shouldSaveChanges = true;
            }
        } else {
            if (userDB.getCountry() == null || !userDB.getCountry().equals(properties.get("location").toString())) {
                userDB.setCountry(properties.get("location").toString());
                shouldSaveChanges = true;
            }
        }

        // Bio
        if (properties.get("bio") == null) {
            if (userDB.getBio() != null) {
                userDB.setBio(null);
                shouldSaveChanges = true;
            }
        } else {
            if (userDB.getBio() == null || !userDB.getBio().equals(properties.get("bio").toString())) {
                userDB.setBio(properties.get("bio").toString());
                shouldSaveChanges = true;
            }
        }

        // Last Online Date (update if there is 1day difference to avoid multiple database updates on the same day)
        long currentTime = new Date().getTime();
        if (currentTime - userDB.getLastOnlineDate() >= ONE_DAY) {
            userDB.setLastOnlineDate(currentTime);
            shouldSaveChanges = true;
        }

        if (shouldSaveChanges) {
            userService.save(userDB);
        }
    }

    /**
     * This function counts a number of new unread messages received by another users
     *
     * @param user is a user object in the database
     * @return number of messages that are unread
     */
    private int countUnreadMessages(User user) {
        int unreadMessages = 0;

        for (Map.Entry<String, Pair<Integer, List<Message>>> entry : user.getDialogs().entrySet()) {
            unreadMessages += entry.getValue().getKey();
        }

        return unreadMessages;
    }
}
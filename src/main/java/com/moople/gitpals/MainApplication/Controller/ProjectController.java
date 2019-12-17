package com.moople.gitpals.MainApplication.Controller;

import com.moople.gitpals.MainApplication.Model.Comment;
import com.moople.gitpals.MainApplication.Model.Message;
import com.moople.gitpals.MainApplication.Model.Project;
import com.moople.gitpals.MainApplication.Model.User;
import com.moople.gitpals.MainApplication.Service.Data;
import com.moople.gitpals.MainApplication.Service.ProjectInterface;
import com.moople.gitpals.MainApplication.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ProjectController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectInterface projectInterface;

    /**
     * This request is handled when user wants to see a page where they can create a project
     *
     * @return html page where users can submit their project
     */
    @GetMapping("/submitProject")
    public String projectForm(Principal user, Model model) {
        if (user != null) {
            model.addAttribute("UserObject", user);
            model.addAttribute("techs", Data.technologiesMap);
            model.addAttribute("projectObject", new Project());

            return "sections/projectSubmitForm";
        }

        return "redirect:/";
    }

    /**
     * This request is handled when user created their project
     * If there is no project with same name, create and save
     * Otherwise display an error message
     *
     * @param project is taken from html form with all the data (project name, description etc.)
     * @param techs   is a checkbox list of technologies for a project that user selects
     * @return create project and redirect to its page, otherwise show an error messaging about identical project name
     **/
    @PostMapping("/projectSubmitted")
    public String projectSubmitted(
            Principal user,
            @ModelAttribute Project project,
            @RequestParam(value = "techInput", required = false) List<String> techs,
            RedirectAttributes redirectAttributes) {

        // If authenticated user is null (so there is no auth), redirect to main page
        if (user == null) {
            return "redirect:/";
        }

        if (techs == null) {
            redirectAttributes.addFlashAttribute("warning", "Your project should have some requirements");
            return "redirect:/submitProject";
        }

        Project projectDB = projectInterface.findByTitle(project.getTitle());

        if (projectDB == null) {
            Project userProject = new Project(
                    project.getTitle(),
                    project.getDescription(),
                    project.getGithubProjectLink(),
                    userService.findByUsername(user.getName()).getUsername(),
                    techs
            );

            User userInDB = userService.findByUsername(user.getName());

            userInDB.getProjects().add(userProject.getTitle());

            userService.save(userInDB);
            projectInterface.save(userProject);

            return "redirect:/projects/" + userProject.getTitle();
        } else {
            return "error/projectExists";
        }
    }

    /**
     * This request is handled when user wants to see a project's page
     * All the data (project name, applied users, author etc.) will be added & displayed
     *
     * @param projectName is taken from an address field - like "/project/UnrealEngine"
     * @return html project page with it's title, author, description, technologies etc
     **/
    @GetMapping("/projects/{projectName}")
    public String projectPage(@PathVariable String projectName, Model model, Principal user) {
        Project project = projectInterface.findByTitle(projectName);

        if (project == null) {
            return "error/projectDeleted";
        } else {
            model.addAttribute("project", project);

            if (user != null) {
                model.addAttribute("userDB", userService.findByUsername(user.getName()));
            }

            return "sections/projectViewPage";
        }
    }

    /**
     * This request is handled when user wants to apply to a project
     *
     * @param link is project's title which is taken from a hidden html textfield (value assigned automatically with thymeleaf)
     * @return redirect to the same project page
     **/
    @PostMapping("/applyForProject")
    public String applyForProject(@RequestParam("linkInput") String link, Principal user) {

        // If authenticated user is null (so there is no auth), redirect to main page
        if (user == null) {
            return "redirect:/";
        }

        User userForApply = userService.findByUsername(user.getName());
        Project project = projectInterface.findByTitle(link);

        // Users that already submitted can't submit another time, only once per project
        if (!project.getUsersSubmitted().contains(user.getName())) {
            project.getUsersSubmitted().add(userForApply.getUsername());
            userForApply.getProjectsAppliedTo().add(project.getTitle());

            projectInterface.save(project);
            userService.save(userForApply);
        }

        return "redirect:/projects/" + link;
    }

    /**
     * This request is handled when user wants to un-apply from a project
     * They will be removed from applied list
     *
     * @param link is project's title which is taken from a hidden html textfield (value assigned automatically with thymeleaf)
     * @return redirect to the same project page
     **/
    @PostMapping("/unapplyForProject")
    public String unapplyForProject(@RequestParam("linkInput") String link, Principal user) {

        // If authenticated user is null (so there is no auth), redirect to main page
        if (user == null) {
            return "redirect:/";
        }

        Project projectDB = projectInterface.findByTitle(link);

        User userDB = userService.findByUsername(user.getName());

        if (projectDB.getUsersSubmitted().contains(userDB.getUsername())) {
            projectDB.getUsersSubmitted().remove(userDB.getUsername());
            userDB.getProjectsAppliedTo().remove(projectDB.getTitle());

            projectInterface.save(projectDB);
            userService.save(userDB);
        }

        return "redirect:/projects/" + link;
    }

    /**
     * This request is handled when user wants to delete project
     * It will be deleted and applied users will be notified about that
     *
     * @param projectName is project's title which is taken from a html textfield
     * @return redirect to the index page
     **/
    @PostMapping("/deleteProject")
    public String projectDeleted(Principal user, @RequestParam("projectName") String projectName) {

        // If authenticated user is null (so there is no auth), redirect to main page
        if (user == null) {
            return "redirect:/";
        }

        User userDB = userService.findByUsername(user.getName());
        Project project = projectInterface.findByTitle(projectName);

        // Remove project from author's projects list
        if (userDB != null && userDB.getUsername().equals(project.getAuthorName())) {
            projectInterface.delete(project);

            if (userDB.getProjects().contains(project.getTitle())) {
                userDB.getProjects().remove(project.getTitle());

                userService.save(userDB);
            }

            // Remove project from everyone who applied to this project
            // First we stream, it returns list of Strings, map them to User object
            List<User> allUsers = project.getUsersSubmitted().stream()
                    .map(submittedUser -> userService.findByUsername(submittedUser))
                    .collect(Collectors.toList());

            // Every applied user will receive a message about project deletion
            Message notification = new Message(project.getAuthorName(), "Project " + projectName + " you were applied to has been deleted", Message.TYPE.INBOX_MESSAGE);

            for (User _user : allUsers) {
                _user.getProjectsAppliedTo().remove(project.getTitle());
                _user.getMessages().add(notification);

                userService.save(_user);
            }

            return "redirect:/";

        } else {
            return "error/siteBroken";
        }
    }

    /**
     * This request is handled when user wants to sort projects by language
     * They will be sorted and displayed
     *
     * @param data     is a list of technologies checkboxes user select manually
     * @param isUnique is a condition whether there are any other techs EXCEPT what users choose (null if checkbox is not selected, "off" if selected)
     * @return a list of projects according to user's preference
     **/
    @PostMapping("/sortProjects")
    public String projectsSorted(@RequestParam("sort_projects") List<String> data, @RequestParam(required = false, name = "isUnique") boolean isUnique, Model model) {
        List<Project> allProjects = projectInterface.findAll();

        List<Project> matchProjects;

        /** @param isUnique does the following:
         * if there is a project with some requirements and we mark a checkbox then
         * it will find a project with chosen requirements ONLY
         *
         * if checkbox is not selected it will find the same project by ONE of the requirements
         */
        if (isUnique) { // true - if checkbox IS selected
            matchProjects = allProjects.stream()
                    .filter(project -> project.getRequirements().equals(data))
                    .collect(Collectors.toList());
        } else { // false - checkbox IS NOT selected
            matchProjects = allProjects.stream()
                    .filter(project -> data.stream()
                            .anyMatch(req -> project.getRequirements().contains(req)))
                    .collect(Collectors.toList());
        }

        model.addAttribute("matchProjects", matchProjects);

        return "sections/projectsAfterSorting";
    }

    /**
     * This request is handled when user submits their comment
     * It will be added to comments list and saved
     *
     * @param projectName is taken from a hidden html textfield
     * @param text        is taken from a html textfield
     * @param user        is assigned automatically using thymeleaf
     * @return project comments page with new comment
     */
    @PostMapping("/sendComment")
    public String sendComment(@RequestParam("projectName") String projectName, @RequestParam("text") String text, Principal user) {

        Project project = projectInterface.findByTitle(projectName);

        if (user != null && project != null) {
            Comment comment = new Comment(user.getName(), text);

            project.getComments().add(comment);
            projectInterface.save(project);
        }

        return "redirect:/projects/" + projectName;
    }
}
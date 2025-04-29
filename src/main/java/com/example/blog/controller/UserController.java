package com.example.blog.controller;

import com.example.blog.model.Post;
import com.example.blog.model.User;
import com.example.blog.service.CommentService;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;

    @Autowired
    public UserController(UserService userService, PostService postService, CommentService commentService) {
        this.userService = userService;
        this.postService = postService;
        this.commentService = commentService;
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "user-register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        if (userService.existsByEmail(user.getEmail())) {
            redirectAttributes.addFlashAttribute("error", "Email already exists");
            return "redirect:/users/register";
        }

        userService.saveUser(user);
        redirectAttributes.addFlashAttribute("success", "Registration successful");
        return "redirect:/users/dashboard/" + user.getId();
    }

    @GetMapping("/dashboard/{id}")
    public String userDashboard(@PathVariable Long id, Model model) {
        Optional<User> userOpt = userService.findUserById(id);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("user", user);
            model.addAttribute("posts", postService.findPostsByAuthor(user));
            return "user-dashboard";
        } else {
            return "redirect:/";
        }
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        List<User> users = userService.findAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("totalUsers", users.size());

        List<Post> posts = postService.findAllPosts();
        model.addAttribute("posts", posts);
        model.addAttribute("totalPosts", posts.size());

        return "admin-dashboard";
    }

    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        Optional<User> userOpt = userService.findUserById(id);

        if (userOpt.isPresent()) {
            model.addAttribute("user", userOpt.get());
            return "user-edit";
        } else {
            return "redirect:/";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateUser(@PathVariable Long id, @ModelAttribute User user, RedirectAttributes redirectAttributes) {
        Optional<User> existingUserOpt = userService.findUserById(id);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            existingUser.setName(user.getName());

            // Only update email if it's changed and not already taken
            if (!existingUser.getEmail().equals(user.getEmail())) {
                if (userService.existsByEmail(user.getEmail())) {
                    redirectAttributes.addFlashAttribute("error", "Email already exists");
                    return "redirect:/users/" + id + "/edit";
                }
                existingUser.setEmail(user.getEmail());
            }

            // Only admin can change admin status
            if (user.isAdmin()) {
                existingUser.setAdmin(true);
            }

            userService.saveUser(existingUser);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
            return "redirect:/users/dashboard/" + id;
        } else {
            return "redirect:/";
        }
    }

    @GetMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/users/admin";
    }

    @GetMapping("/admin/{id}/ban")
    public String banUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.banUser(id, true);
        if (user != null) {
            redirectAttributes.addFlashAttribute("success", "User " + user.getName() + " has been banned");
        }
        return "redirect:/users/admin";
    }

    @GetMapping("/admin/{id}/unban")
    public String unbanUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.banUser(id, false);
        if (user != null) {
            redirectAttributes.addFlashAttribute("success", "User " + user.getName() + " has been unbanned");
        }
        return "redirect:/users/admin";
    }
}

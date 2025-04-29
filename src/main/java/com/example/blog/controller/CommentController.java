package com.example.blog.controller;

import com.example.blog.model.Comment;
import com.example.blog.model.Post;
import com.example.blog.model.User;
import com.example.blog.service.CommentService;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;
    private final PostService postService;
    private final UserService userService;

    @Autowired
    public CommentController(CommentService commentService, PostService postService, UserService userService) {
        this.commentService = commentService;
        this.postService = postService;
        this.userService = userService;
    }

    @PostMapping("/add")
    public String addComment(@ModelAttribute Comment comment, 
                            @RequestParam Long postId,
                            RedirectAttributes redirectAttributes,
                            java.security.Principal principal) {

        // Check if user is authenticated
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to post comments");
            return "redirect:/posts/" + postId;
        }

        Optional<Post> postOpt = postService.findPostById(postId);

        // Get the currently authenticated user
        String username = principal.getName();
        Optional<User> userOpt = userService.findUserByEmail(username);

        if (postOpt.isPresent() && userOpt.isPresent()) {
            comment.setPost(postOpt.get());
            comment.setUser(userOpt.get());
            commentService.saveComment(comment);
            redirectAttributes.addFlashAttribute("success", "Comment added successfully");
            return "redirect:/posts/" + postId;
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to add comment");
            return "redirect:/posts/" + postId;
        }
    }

    @GetMapping("/{id}/delete")
    public String deleteComment(@PathVariable Long id, @RequestParam Long postId) {
        commentService.deleteComment(id);
        return "redirect:/posts/" + postId;
    }
}

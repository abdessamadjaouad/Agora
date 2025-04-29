package com.example.blog.controller;

import com.example.blog.model.Comment;
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

import java.util.Optional;

@Controller
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final UserService userService;
    private final CommentService commentService;

    @Autowired
    public PostController(PostService postService, UserService userService, CommentService commentService) {
        this.postService = postService;
        this.userService = userService;
        this.commentService = commentService;
    }

    @GetMapping("/{id}")
    public String viewPost(@PathVariable Long id, Model model) {
        Optional<Post> postOpt = postService.findPostById(id);
        
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            model.addAttribute("post", post);
            model.addAttribute("comments", commentService.findCommentsByPost(post));
            model.addAttribute("newComment", new Comment());
            return "post-view";
        } else {
            return "redirect:/";
        }
    }

    @GetMapping("/new")
    public String newPostForm(Model model) {
        model.addAttribute("post", new Post());
        return "post-form";
    }

    @PostMapping("/new")
    public String createPost(@ModelAttribute Post post, @RequestParam Long userId, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userService.findUserById(userId);
        
        if (userOpt.isPresent()) {
            post.setAuthor(userOpt.get());
            Post savedPost = postService.savePost(post);
            return "redirect:/posts/" + savedPost.getId();
        } else {
            redirectAttributes.addFlashAttribute("error", "User not found");
            return "redirect:/posts/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editPostForm(@PathVariable Long id, Model model) {
        Optional<Post> postOpt = postService.findPostById(id);
        
        if (postOpt.isPresent()) {
            model.addAttribute("post", postOpt.get());
            return "post-form";
        } else {
            return "redirect:/";
        }
    }

    @PostMapping("/{id}/edit")
    public String updatePost(@PathVariable Long id, @ModelAttribute Post post) {
        Optional<Post> existingPostOpt = postService.findPostById(id);
        
        if (existingPostOpt.isPresent()) {
            Post existingPost = existingPostOpt.get();
            existingPost.setTitle(post.getTitle());
            existingPost.setContent(post.getContent());
            postService.savePost(existingPost);
            return "redirect:/posts/" + id;
        } else {
            return "redirect:/";
        }
    }

    @GetMapping("/{id}/delete")
    public String deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return "redirect:/";
    }
}
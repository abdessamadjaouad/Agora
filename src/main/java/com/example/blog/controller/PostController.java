package com.example.blog.controller;

import com.example.blog.model.Comment;
import com.example.blog.model.Post;
import com.example.blog.model.User;
import com.example.blog.service.CommentService;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final UserService userService;
    private final CommentService commentService;

    private final Path uploadPath;

    @Autowired
    public PostController(PostService postService, UserService userService, CommentService commentService,
                         @Value("${upload.dir}") String uploadDir) {
        this.postService = postService;
        this.userService = userService;
        this.commentService = commentService;

        this.uploadPath = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
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
    public String createPost(@ModelAttribute Post post, 
                            @RequestParam("photo") MultipartFile photo,
                            RedirectAttributes redirectAttributes, 
                            java.security.Principal principal) {
        String username = principal.getName();
        Optional<User> userOpt = userService.findUserByEmail(username);

        if (userOpt.isPresent()) {
            post.setAuthor(userOpt.get());

            // Handle photo upload
            if (!photo.isEmpty()) {
                try {
                    // Generate unique filename
                    String filename = UUID.randomUUID().toString() + "_" + photo.getOriginalFilename();
                    Path targetLocation = uploadPath.resolve(filename);
                    Files.copy(photo.getInputStream(), targetLocation);

                    // Save photo path to post
                    post.setPhotoPath(filename);
                } catch (IOException e) {
                    redirectAttributes.addFlashAttribute("error", "Failed to upload photo: " + e.getMessage());
                    return "redirect:/posts/new";
                }
            }

            Post savedPost = postService.savePost(post);
            return "redirect:/posts/" + savedPost.getId();
        } else {
            redirectAttributes.addFlashAttribute("error", "User not found");
            return "redirect:/posts/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editPostForm(@PathVariable Long id, Model model, java.security.Principal principal) {
        Optional<Post> postOpt = postService.findPostById(id);

        if (postOpt.isPresent()) {
            Post post = postOpt.get();

            // Check if the current user is the author of the post
            String username = principal.getName();
            if (!post.getAuthor().getEmail().equals(username)) {
                return "redirect:/posts/" + id;
            }

            model.addAttribute("post", post);
            return "post-form";
        } else {
            return "redirect:/";
        }
    }

    @PostMapping("/{id}/edit")
    public String updatePost(@PathVariable Long id, 
                           @ModelAttribute Post post, 
                           @RequestParam(value = "photo", required = false) MultipartFile photo,
                           RedirectAttributes redirectAttributes,
                           java.security.Principal principal) {
        Optional<Post> existingPostOpt = postService.findPostById(id);

        if (existingPostOpt.isPresent()) {
            Post existingPost = existingPostOpt.get();

            // Check if the current user is the author of the post
            String username = principal.getName();
            if (!existingPost.getAuthor().getEmail().equals(username)) {
                return "redirect:/posts/" + id;
            }

            existingPost.setTitle(post.getTitle());
            existingPost.setContent(post.getContent());
            existingPost.setUpdatedAt(LocalDateTime.now());

            // Handle photo upload
            if (photo != null && !photo.isEmpty()) {
                try {
                    // Generate unique filename
                    String filename = UUID.randomUUID().toString() + "_" + photo.getOriginalFilename();
                    Path targetLocation = uploadPath.resolve(filename);
                    Files.copy(photo.getInputStream(), targetLocation);

                    // Delete old photo if exists
                    if (existingPost.getPhotoPath() != null) {
                        try {
                            Files.deleteIfExists(uploadPath.resolve(existingPost.getPhotoPath()));
                        } catch (IOException e) {
                            // Log error but continue
                            System.err.println("Could not delete old photo: " + e.getMessage());
                        }
                    }

                    // Save new photo path to post
                    existingPost.setPhotoPath(filename);
                } catch (IOException e) {
                    redirectAttributes.addFlashAttribute("error", "Failed to upload photo: " + e.getMessage());
                    return "redirect:/posts/" + id + "/edit";
                }
            }

            postService.savePost(existingPost);
            return "redirect:/posts/" + id;
        } else {
            return "redirect:/";
        }
    }

    @GetMapping("/{id}/delete")
    public String deletePost(@PathVariable Long id, java.security.Principal principal) {
        Optional<Post> postOpt = postService.findPostById(id);

        if (postOpt.isPresent()) {
            Post post = postOpt.get();

            // Check if the current user is the author of the post
            String username = principal.getName();
            if (!post.getAuthor().getEmail().equals(username)) {
                return "redirect:/posts/" + id;
            }

            // Delete photo if exists
            if (post.getPhotoPath() != null) {
                try {
                    Files.deleteIfExists(uploadPath.resolve(post.getPhotoPath()));
                } catch (IOException e) {
                    // Log error but continue with post deletion
                    System.err.println("Could not delete photo: " + e.getMessage());
                }
            }

            postService.deletePost(id);
        }

        return "redirect:/";
    }

    // Admin endpoint for deleting any post
    @GetMapping("/admin/{id}/delete")
    public String adminDeletePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Post> postOpt = postService.findPostById(id);

        if (postOpt.isPresent()) {
            Post post = postOpt.get();

            // Delete photo if exists
            if (post.getPhotoPath() != null) {
                try {
                    Files.deleteIfExists(uploadPath.resolve(post.getPhotoPath()));
                } catch (IOException e) {
                    // Log error but continue with post deletion
                    System.err.println("Could not delete photo: " + e.getMessage());
                }
            }

            postService.deletePost(id);
            redirectAttributes.addFlashAttribute("success", "Post deleted successfully");
        } else {
            redirectAttributes.addFlashAttribute("error", "Post not found");
        }

        return "redirect:/users/admin";
    }

    // Method to serve uploaded files
    @GetMapping("/uploads/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path file = uploadPath.resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + filename, e);
        }
    }
}

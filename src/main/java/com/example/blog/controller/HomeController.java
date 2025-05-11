package com.example.blog.controller;

import com.example.blog.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final PostService postService;

    @Autowired
    public HomeController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/")
    public String home(Model model, @RequestParam(defaultValue = "0") int page) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && 
                                 authentication.isAuthenticated() && 
                                 !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/welcome";
        }

        int pageSize = 5; // Number of posts per page
        Page<?> posts = postService.findPaginated(page, pageSize);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", posts.getTotalPages());
        model.addAttribute("totalItems", posts.getTotalElements());
        model.addAttribute("posts", posts.getContent());

        return "index";
    }

    @GetMapping("/welcome")
    public String welcome() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && 
                                 authentication.isAuthenticated() && 
                                 !authentication.getName().equals("anonymousUser");

        if (isAuthenticated) {
            return "redirect:/";
        }

        return "welcome";
    }

    @GetMapping("/search")
    public String search(@RequestParam String query, Model model) {
        model.addAttribute("posts", postService.searchPostsByTitle(query));
        model.addAttribute("query", query);
        return "search-results";
    }
}

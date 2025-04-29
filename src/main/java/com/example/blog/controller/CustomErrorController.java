package com.example.blog.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    public CustomErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Get the status code
        Object status = request.getAttribute("javax.servlet.error.status_code");

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            // Add status code to the model
            model.addAttribute("statusCode", statusCode);

            // Add error message based on status code
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("errorMessage", "The page you are looking for does not exist.");
                return "error/404";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                model.addAttribute("errorMessage", "You don't have permission to access this resource.");
                return "error/403";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                model.addAttribute("errorMessage", "An internal server error occurred.");
                return "error/500";
            } else {
                model.addAttribute("errorMessage", "An unexpected error occurred.");
            }
        } else {
            model.addAttribute("statusCode", 500);
            model.addAttribute("errorMessage", "An unexpected error occurred.");
        }

        return "error";
    }

    // For compatibility with older versions of Spring Boot
    public String getErrorPath() {
        return "/error";
    }
}

package org.arjunkashyap.learningbot.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebComponentsController {
    @Value("${spring.application.name}")
    String appName;

    @GetMapping("/")
    public String homePage(Model model) {
        model.addAttribute("appName", appName);
        return "home";
    }
    @GetMapping("/admin")
    public String adminPage(Model model) {
        model.addAttribute("appName", appName);
        return "admin";
    }
    @GetMapping("/adminscripts.js")
    public String adminScripts(Model model) {
        model.addAttribute("appName", appName);
        return "adminscripts.js";
    }
    @GetMapping("/commonscripts.js")
    public String commonScripts(Model model) {
        model.addAttribute("appName", appName);
        return "commonscripts.js";
    }
    @GetMapping("/homepagescripts.js")
    public String homepageScripts(Model model) {
        model.addAttribute("appName", appName);
        return "homepagescripts.js";
    }
}
package com.kimanga.afyacheck.controllers;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class HealthCenterController {

    @Value("${google.maps.api.key}")
    private String apiKey;

    @GetMapping("/health-centers")
    public String healthCentersPage(Model model) {
        System.out.println("=== API KEY DEBUG ===");
        System.out.println("API Key from properties: " + (apiKey != null ? "PRESENT" : "NULL"));
        System.out.println("Key length: " + (apiKey != null ? apiKey.length() : 0));
        System.out.println("Key starts with: " + (apiKey != null ? apiKey.substring(0, Math.min(10, apiKey.length())) : "N/A"));

        model.addAttribute("apiKey", apiKey);
        return "health-centers";
    }
}
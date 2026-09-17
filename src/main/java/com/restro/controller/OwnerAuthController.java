package com.restro.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OwnerAuthController {

    @GetMapping("/owner/login")
    public String loginPage() {
        return "owner/login";
    }
}

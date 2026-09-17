package com.restro.controller;

import org.springframework.beans.factory.annotation.Autowired;

import com.restro.entity.Owner;
import com.restro.Repo.OwnerRepository;
import com.restro.security.OwnerUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Owner's own login/password management (single account - no "manage staff" screen needed anymore). */
@Controller
public class OwnerAccountController {

    @Autowired
    private OwnerRepository ownerRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/owner/account")
    public String account(@AuthenticationPrincipal OwnerUserDetails principal, Model model) {
        model.addAttribute("owner", principal.getOwner());
        return "owner/account";
    }

    @PostMapping("/owner/account/password")
    public String changePassword(@AuthenticationPrincipal OwnerUserDetails principal,
                                  @RequestParam String currentPassword,
                                  @RequestParam String newPassword,
                                  Model model) {
        Owner owner = ownerRepository.findById(principal.getOwnerId()).orElseThrow();
        if (!passwordEncoder.matches(currentPassword, owner.getPasswordHash())) {
            model.addAttribute("owner", owner);
            model.addAttribute("error", "Current password is incorrect.");
            return "owner/account";
        }
        owner.setPasswordHash(passwordEncoder.encode(newPassword));
        ownerRepository.save(owner);
        model.addAttribute("owner", owner);
        model.addAttribute("success", "Password updated.");
        return "owner/account";
    }
}

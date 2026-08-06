package com.example.demo

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class UserController {

    /**
     * Page 1: Renders the initial input form with name, phone number, and FIFA 2026 match selection.
     */
    @GetMapping("/")
    fun showForm(): String {
        return "index"
    }

    /**
     * Page 2: Receives form submission and renders confirmation details page.
     */
    @PostMapping("/submit")
    fun processForm(
        @RequestParam("name", defaultValue = "") name: String,
        @RequestParam("phoneNumber", defaultValue = "") phoneNumber: String,
        @RequestParam("favoriteMatch", defaultValue = "Not Selected") favoriteMatch: String,
        model: Model
    ): String {
        model.addAttribute("userName", name.ifBlank { "Guest User" })
        model.addAttribute("userPhone", phoneNumber.ifBlank { "Not Provided" })
        model.addAttribute("favoriteMatch", favoriteMatch)
        return "details"
    }
}

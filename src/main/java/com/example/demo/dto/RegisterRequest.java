package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, message = "Password must have at least 6 characters")
    private String password;

    @Size(max = 500, message = "Bio must have at most 500 character")
    private String bio;

    private String avatarUrl;

    private String phone;
    public RegisterRequest() {
    }

    public RegisterRequest(String username, String email, String password, String bio, String avatarUrl, String phone ) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.phone = phone;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone(){
        return phone;
    }
    public void setPhone(String phone){
        this.phone = phone;
    }

    public String getAvatarUrl(){
        return avatarUrl;
    }
    public void setAvatarUrl(String avatarUrl){
        this.avatarUrl = avatarUrl;
    }

    public String getBio(){
        return bio;
    }
    public void setBio(String bio){
        this.bio = bio;
    }
}

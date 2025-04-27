package com.bookstore;

public class Admin extends User {
    public Admin(String login, String firstName, String lastName, String email, String birthDate, String password, String avatarPath) {
        super(login, firstName, lastName, email, birthDate, password, avatarPath);
        this.role = "Admin";
    }

    @Override
    public String getRole() {
        return "Admin";
    }
}
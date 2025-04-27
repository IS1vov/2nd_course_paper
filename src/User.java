package com.bookstore;

import org.mindrot.jbcrypt.BCrypt;

public abstract class User {
    protected String login;
    protected String firstName;
    protected String lastName;
    protected String email;
    protected String birthDate;
    protected String password;
    protected String avatarPath;
    protected String role;

    public User(String login, String firstName, String lastName, String email, String birthDate, String password, String avatarPath) {
        this.login = login;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.birthDate = birthDate;
        this.password = password;
        this.avatarPath = avatarPath;
        this.role = "Client";
    }

    public boolean authenticate(String password) {
        return BCrypt.checkpw(password, this.password);
    }

    public String getLogin() {
        return login;
    }

    public String getName() {
        return firstName + " " + lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
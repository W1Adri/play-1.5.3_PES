package com.playframework.webapp;

public class User {
    private final int id;
    private final String username;
    private String password;
    private String email;
    private String fullName;
    private Role role;

    public User(int id, String username, String password, String email, String fullName, Role role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public User(int id, String username, String email, String fullName, Role role) {
        this(id, username, "", email, fullName, role);
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}

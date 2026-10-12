package com.example.kaboom.entities;


import java.time.*;




public class User {
    private int id;
    private String name;
    private String email;
    private String passwordHash;
    private int coin;
    private String role;
    private LocalTime createdAt;

    public User(int id, String name, String email, String passwordHash) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }








    private static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9._]+@[A-Za-z0-9.]+\\.[A-Za-z]{2,}$");
     }

     private static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8 && password.matches(".*\\\\d.*");
     }

}

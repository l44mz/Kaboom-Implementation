package com.example.kaboom.entities;

import java.util.UUID;

public class Admin extends User {
    public Admin() {
        super();
        setRole("admin");
    }

    public Admin(UUID id, String name, String email) {
        super(id, name, email);
        setRole("admin");
    }
}
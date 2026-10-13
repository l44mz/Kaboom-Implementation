package com.example.kaboom.entities;

import java.util.UUID;

public class Admin extends User{
        public Admin() {
            super();
            setRole("admin");   // works because the setter is protected
        }

        public Admin(UUID id, String name, String email) {
            super(id, name, email);
            setRole("admin");
        }

        /*public boolean addProduct(Product product) {
            if (product == null
                    || product.getName() == null || product.getName().isBlank()
                    || product.getCategory() == null || product.getCategory().isBlank()
                    || product.getPrice() < 0) {
                return false;
            }

        } */

}

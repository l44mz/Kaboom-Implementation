package com.example.kaboom.dtos;

import com.example.kaboom.entities.Product.Category;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductDto {
    private String name;
    private Category category;
    private int price;
    private String description;

    public ProductDto(String name, Category category, int price, String description) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
    }
}

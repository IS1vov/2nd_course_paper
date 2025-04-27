package com.bookstore;

import java.util.ArrayList;
import java.util.List;

public class Category {
    private String name;
    private List<Book> books;

    public Category(String name) {
        this.name = name;
        this.books = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<Book> readBooks() {
        return books;
    }

    public void createBook(int id, String name, double price, String description, String coverPath, int stock) {
        books.add(new Book(id, name, price, description, this, coverPath, stock));
    }
}
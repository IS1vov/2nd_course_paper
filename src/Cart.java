package com.bookstore;

import java.util.ArrayList;
import java.util.List;

public class Cart {
    private List<Book> books;

    public Cart() {
        books = new ArrayList<>();
    }

    public boolean addBook(Book book) {
        if (book.getStock() > 0) {
            books.add(book);
            return true;
        }
        return false;
    }

    public void removeBook(int index) {
        if (index >= 0 && index < books.size()) {
            books.remove(index);
        }
    }

    public List<Book> getBooks() {
        return books;
    }

    public void clear() {
        books.clear();
    }
}
package com.bookstore;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookStore {
    private DatabaseManager db;

    public BookStore() {
        db = new DatabaseManager();
        try {
            db.connect();
            db.createTables();
            User admin = findUser("admin");
            if (admin == null) {
                System.out.println("Creating admin user...");
                try {
                    db.registerAdmin("admin", "Admin", "User", "admin@example.com", "1980-01-01", "admin123", "avatars/ava.jpg");
                    admin = findUser("admin");
                    if (admin != null) {
                        System.out.println("Admin created successfully: login=admin, password=admin123, role=" + admin.getRole());
                    } else {
                        System.out.println("Error: Admin not found after creation!");
                    }
                } catch (SQLException e) {
                    System.err.println("Error creating admin: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("Admin already exists: " + admin.getName() + ", role: " + admin.getRole());
            }
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public DatabaseManager getDb() {
        return db;
    }

    public User findUser(String login) {
        try {
            return db.findUser(login);
        } catch (SQLException e) {
            System.err.println("Error finding user: " + e.getMessage());
            return null;
        }
    }

    public void registerUser(String login, String firstName, String lastName, String email, String birthDate, String password, String avatarPath) throws SQLException {
        db.registerUser(login, firstName, lastName, email, birthDate, password, avatarPath);
    }

    public List<User> getAllUsers() {
        try {
            return db.getAllUsers();
        } catch (SQLException e) {
            System.err.println("Error retrieving users: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void removeUser(String login) throws SQLException {
        db.removeUser(login);
    }

    public List<Category> readCategories() {
        List<Category> categories = new ArrayList<>();
        String[] categoryNames = {"Fiction", "Non-Fiction", "Science", "Fantasy", "Mystery", "Romance", "History"};
        try {
            for (String name : categoryNames) {
                if (!db.categoryExists(name)) {
                    db.saveCategory(name);
                }
                categories.add(new Category(name));
            }
        } catch (SQLException e) {
            System.err.println("Error handling categories: " + e.getMessage());
            for (String name : categoryNames) {
                if (!categories.stream().anyMatch(c -> c.getName().equals(name))) {
                    categories.add(new Category(name));
                }
            }
        }
        System.out.println("Loaded categories: " + categories.size());
        return categories;
    }

    public void addReview(int bookId, String userLogin, String text, Integer parentId) throws SQLException {
        db.saveReview(bookId, userLogin, text, parentId);
    }

    public List<Review> getReviews(int bookId) {
        return db.getReviews(bookId);
    }

    public void sendMessage(String senderLogin, String receiverLogin, String text) throws SQLException {
        db.saveMessage(senderLogin, receiverLogin, text);
    }

    public List<Message> getMessages(String userLogin) {
        try {
            return db.getMessages(userLogin);
        } catch (SQLException e) {
            System.err.println("Error retrieving messages: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Book> getFilteredBooks(String categoryName, String filterType) throws SQLException {
        return db.getFilteredBooks(categoryName, filterType);
    }

    public int getPurchaseCount(String categoryName) throws SQLException {
        return db.getPurchaseCount(categoryName);
    }

    public int getReviewCount(String categoryName) throws SQLException {
        return db.getReviewCount(categoryName);
    }

    public double getAverageRating(String categoryName) throws SQLException {
        return db.getAverageRating(categoryName);
    }
}
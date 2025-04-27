package com.bookstore;

import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private Connection conn;

    public void connect() throws SQLException {
        new File("avatars").mkdirs();
        new File("covers").mkdirs();
        String dbPath = "bookstore.db";
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        System.out.println("Connected to database: " + dbPath);
        conn.setAutoCommit(true);
    }

    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
            System.out.println("Database connection closed");
        }
    }

    public void createTables() throws SQLException {
        Statement stmt = conn.createStatement();

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS users (
                login TEXT PRIMARY KEY,
                first_name TEXT NOT NULL,
                last_name TEXT NOT NULL,
                email TEXT NOT NULL,
                birth_date TEXT NOT NULL,
                password TEXT NOT NULL,
                avatar_path TEXT,
                role TEXT NOT NULL DEFAULT 'Client'
            )""");

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS categories (
                name TEXT PRIMARY KEY
            )""");

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS books (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                price REAL NOT NULL,
                description TEXT,
                category_name TEXT,
                cover_path TEXT,
                stock INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (category_name) REFERENCES categories(name)
            )""");

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS reviews (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id INTEGER NOT NULL,
                user_login TEXT NOT NULL,
                text TEXT NOT NULL,
                parent_id INTEGER,
                likes INTEGER DEFAULT 0,
                dislikes INTEGER DEFAULT 0,
                FOREIGN KEY (book_id) REFERENCES books(id),
                FOREIGN KEY (user_login) REFERENCES users(login),
                FOREIGN KEY (parent_id) REFERENCES reviews(id)
            )""");

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS reactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_login TEXT NOT NULL,
                review_id INTEGER NOT NULL,
                reaction TEXT NOT NULL,
                FOREIGN KEY (user_login) REFERENCES users(login),
                FOREIGN KEY (review_id) REFERENCES reviews(id)
            )""");

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS book_reactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_login TEXT NOT NULL,
                book_id INTEGER NOT NULL,
                rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
                FOREIGN KEY (user_login) REFERENCES users(login),
                FOREIGN KEY (book_id) REFERENCES books(id),
                UNIQUE(user_login, book_id)
            )""");

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sender_login TEXT NOT NULL,
                receiver_login TEXT NOT NULL,
                text TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (sender_login) REFERENCES users(login),
                FOREIGN KEY (receiver_login) REFERENCES users(login)
            )""");

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS purchases (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_login TEXT NOT NULL,
                book_id INTEGER NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_login) REFERENCES users(login),
                FOREIGN KEY (book_id) REFERENCES books(id)
            )""");
    }

    public boolean categoryExists(String name) throws SQLException {
        String query = "SELECT COUNT(*) FROM categories WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public void saveCategory(String name) throws SQLException {
        String query = "INSERT INTO categories (name) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            System.out.println("Category saved: " + name);
        }
    }

    public List<Book> getFilteredBooks(String categoryName, String filterType) throws SQLException {
        String query;
        switch (filterType) {
            case "Price (Ascending)":
                query = "SELECT * FROM books WHERE category_name = ? ORDER BY price ASC";
                break;
            case "Price (Descending)":
                query = "SELECT * FROM books WHERE category_name = ? ORDER BY price DESC";
                break;
            case "Popularity (Descending)":
                query = """
                    SELECT b.* FROM books b
                    LEFT JOIN purchases p ON b.id = p.book_id
                    WHERE b.category_name = ?
                    GROUP BY b.id
                    ORDER BY COUNT(p.id) DESC
                    """;
                break;
            case "Rating (Descending)":
                query = """
                    SELECT b.* FROM books b
                    LEFT JOIN book_reactions r ON b.id = r.book_id
                    WHERE b.category_name = ?
                    GROUP BY b.id
                    ORDER BY COALESCE(AVG(r.rating), 0) DESC
                    """;
                break;
            case "Reviews (Descending)":
                query = """
                    SELECT b.* FROM books b
                    LEFT JOIN reviews r ON b.id = r.book_id
                    WHERE b.category_name = ?
                    GROUP BY b.id
                    ORDER BY COUNT(r.id) DESC
                    """;
                break;
            default:
                query = "SELECT * FROM books WHERE category_name = ?";
        }
        List<Book> books = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, categoryName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getString("description"),
                        new Category(rs.getString("category_name")),
                        rs.getString("cover_path"),
                        rs.getInt("stock")
                ));
            }
        }
        return books;
    }

    public void saveBook(Book book) throws SQLException {
        String query = """
            INSERT INTO books (name, price, description, category_name, cover_path, stock)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, book.getName());
            stmt.setDouble(2, book.getPrice());
            stmt.setString(3, book.getDescription());
            stmt.setString(4, book.getCategory().getName());
            stmt.setString(5, book.getCoverPath());
            stmt.setInt(6, book.getStock());
            stmt.executeUpdate();
        }
    }

    public void updateBook(int id, String name, double price, String description, String coverPath, int stock) throws SQLException {
        String query = """
            UPDATE books
            SET name = ?, price = ?, description = ?, cover_path = ?, stock = ?
            WHERE id = ?
            """;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setDouble(2, price);
            stmt.setString(3, description);
            stmt.setString(4, coverPath);
            stmt.setInt(5, stock);
            stmt.setInt(6, id);
            stmt.executeUpdate();
        }
    }

    public void deleteBook(int id) throws SQLException {
        String query = "DELETE FROM books WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public User findUser(String login) throws SQLException {
        String query = "SELECT * FROM users WHERE login = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = rs.getString("role").equals("Admin") ?
                        new Admin(
                                rs.getString("login"),
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                rs.getString("email"),
                                rs.getString("birth_date"),
                                rs.getString("password"),
                                rs.getString("avatar_path")
                        ) :
                        new Client(
                                rs.getString("login"),
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                rs.getString("email"),
                                rs.getString("birth_date"),
                                rs.getString("password"),
                                rs.getString("avatar_path")
                        );
                user.setRole(rs.getString("role"));
                System.out.println("Found user: " + login + ", role: " + rs.getString("role"));
                return user;
            }
            System.out.println("User " + login + " not found");
            return null;
        }
    }

    public void registerUser(String login, String firstName, String lastName, String email, String birthDate, String password, String avatarPath) throws SQLException {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String query = """
            INSERT INTO users (login, first_name, last_name, email, birth_date, password, avatar_path, role)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'Client')
            """;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, login);
            stmt.setString(2, firstName);
            stmt.setString(3, lastName);
            stmt.setString(4, email);
            stmt.setString(5, birthDate);
            stmt.setString(6, hashedPassword);
            stmt.setString(7, avatarPath);
            stmt.executeUpdate();
            System.out.println("Registered user: " + login);
        }
    }

    public void registerAdmin(String login, String firstName, String lastName, String email, String birthDate, String password, String avatarPath) throws SQLException {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String query = """
            INSERT INTO users (login, first_name, last_name, email, birth_date, password, avatar_path, role)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'Admin')
            """;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, login);
            stmt.setString(2, firstName);
            stmt.setString(3, lastName);
            stmt.setString(4, email);
            stmt.setString(5, birthDate);
            stmt.setString(6, hashedPassword);
            stmt.setString(7, avatarPath);
            stmt.executeUpdate();
            System.out.println("Registered admin: " + login);
        }
    }

    public void updateUserRole(String login, String role) throws SQLException {
        String query = "UPDATE users SET role = ? WHERE login = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, role);
            stmt.setString(2, login);
            stmt.executeUpdate();
            System.out.println("Updated role for " + login + " to " + role);
        }
    }

    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                User user = rs.getString("role").equals("Admin") ?
                        new Admin(
                                rs.getString("login"),
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                rs.getString("email"),
                                rs.getString("birth_date"),
                                rs.getString("password"),
                                rs.getString("avatar_path")
                        ) :
                        new Client(
                                rs.getString("login"),
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                rs.getString("email"),
                                rs.getString("birth_date"),
                                rs.getString("password"),
                                rs.getString("avatar_path")
                        );
                user.setRole(rs.getString("role"));
                users.add(user);
            }
        }
        return users;
    }

    public void removeUser(String login) throws SQLException {
        String query = "DELETE FROM users WHERE login = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, login);
            stmt.executeUpdate();
        }
    }

    public void saveReview(int bookId, String userLogin, String text, Integer parentId) throws SQLException {
        String query = """
            INSERT INTO reviews (book_id, user_login, text, parent_id)
            VALUES (?, ?, ?, ?)
            """;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, bookId);
            stmt.setString(2, userLogin);
            stmt.setString(3, text);
            if (parentId != null) {
                stmt.setInt(4, parentId);
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            stmt.executeUpdate();
        }
    }

    public List<Review> getReviews(int bookId) {
        try {
            List<Review> reviews = new ArrayList<>();
            String query = "SELECT * FROM reviews WHERE book_id = ? ORDER BY id";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, bookId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Review review = new Review(
                            rs.getInt("id"),
                            rs.getInt("book_id"),
                            rs.getString("user_login"),
                            rs.getString("text"),
                            rs.getInt("likes"),
                            rs.getInt("dislikes")
                    );
                    if (rs.getObject("parent_id") != null) {
                        review.setParentId(rs.getInt("parent_id"));
                    }
                    reviews.add(review);
                }
            }
            return reviews;
        } catch (SQLException e) {
            System.err.println("Error retrieving reviews: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void saveReaction(String userLogin, int reviewId, String reaction) throws SQLException {
        String query = """
            INSERT INTO reactions (user_login, review_id, reaction)
            VALUES (?, ?, ?)
            """;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userLogin);
            stmt.setInt(2, reviewId);
            stmt.setString(3, reaction);
            stmt.executeUpdate();
        }
        String updateQuery = """
            UPDATE reviews
            SET likes = (SELECT COUNT(*) FROM reactions WHERE review_id = ? AND reaction = 'Like'),
                dislikes = (SELECT COUNT(*) FROM reactions WHERE review_id = ? AND reaction = 'Dislike')
            WHERE id = ?
            """;
        try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
            stmt.setInt(1, reviewId);
            stmt.setInt(2, reviewId);
            stmt.setInt(3, reviewId);
            stmt.executeUpdate();
        }
    }

    public String getUserReaction(String userLogin, int reviewId) throws SQLException {
        String query = "SELECT reaction FROM reactions WHERE user_login = ? AND review_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userLogin);
            stmt.setInt(2, reviewId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("reaction") : null;
        }
    }

    public void saveBookRating(String userLogin, int bookId, int rating) throws SQLException {
        String query = """
            INSERT INTO book_reactions (user_login, book_id, rating)
            VALUES (?, ?, ?)
            """;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userLogin);
            stmt.setInt(2, bookId);
            stmt.setInt(3, rating);
            stmt.executeUpdate();
        }
    }

    public Integer getUserBookRating(String userLogin, int bookId) throws SQLException {
        String query = "SELECT rating FROM book_reactions WHERE user_login = ? AND book_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userLogin);
            stmt.setInt(2, bookId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("rating") : null;
        }
    }

    public double getBookAverageRating(int bookId) throws SQLException {
        String query = "SELECT AVG(rating) AS avg_rating FROM book_reactions WHERE book_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getDouble("avg_rating") : 0.0;
        }
    }

    public int getBookRatingCount(int bookId) throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM book_reactions WHERE book_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("count") : 0;
        }
    }

    public void saveMessage(String senderLogin, String receiverLogin, String text) throws SQLException {
        String query = """
            INSERT INTO messages (sender_login, receiver_login, text)
            VALUES (?, ?, ?)
            """;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, senderLogin);
            stmt.setString(2, receiverLogin);
            stmt.setString(3, text);
            stmt.executeUpdate();
        }
    }

    public List<Message> getMessages(String userLogin) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String query = """
            SELECT * FROM messages
            WHERE sender_login = ? OR receiver_login = ?
            ORDER BY timestamp
            """;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userLogin);
            stmt.setString(2, userLogin);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                messages.add(new Message(
                        rs.getInt("id"),
                        rs.getString("sender_login"),
                        rs.getString("receiver_login"),
                        rs.getString("text"),
                        rs.getString("timestamp")
                ));
            }
        }
        return messages;
    }

    public void savePurchase(String userLogin, int bookId) throws SQLException {
        String query = """
            INSERT INTO purchases (user_login, book_id)
            VALUES (?, ?)
            """;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userLogin);
            stmt.setInt(2, bookId);
            stmt.executeUpdate();
        }
    }

    public void decreaseStock(int bookId) throws SQLException {
        String query = "UPDATE books SET stock = stock - 1 WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, bookId);
            stmt.executeUpdate();
        }
    }

    public int getPurchaseCount(String categoryName) throws SQLException {
        String query = """
            SELECT COUNT(*) FROM purchases p
            JOIN books b ON p.book_id = b.id
            WHERE b.category_name = ?
            """;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, categoryName);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int getReviewCount(String categoryName) throws SQLException {
        String query = """
            SELECT COUNT(*) FROM reviews r
            JOIN books b ON r.book_id = b.id
            WHERE b.category_name = ?
            """;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, categoryName);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public double getAverageRating(String categoryName) throws SQLException {
        String query = """
            SELECT AVG(r.rating) FROM book_reactions r
            JOIN books b ON r.book_id = b.id
            WHERE b.category_name = ?
            """;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, categoryName);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }
}
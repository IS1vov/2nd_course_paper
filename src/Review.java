package com.bookstore;

import java.util.ArrayList;
import java.util.List;

public class Review {
    private int id;
    private int bookId;
    private String userLogin;
    private String text;
    private Integer parentId;
    private int likes;
    private int dislikes;
    private List<Review> replies;

    public Review(int id, int bookId, String userLogin, String text, int likes, int dislikes) {
        this.id = id;
        this.bookId = bookId;
        this.userLogin = userLogin;
        this.text = text;
        this.likes = likes;
        this.dislikes = dislikes;
        this.replies = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public int getBookId() {
        return bookId;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public String getText() {
        return text;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public int getLikes() {
        return likes;
    }

    public int getDislikes() {
        return dislikes;
    }

    public List<Review> getReplies() {
        return replies;
    }

    public void setReplies(List<Review> replies) {
        this.replies = replies;
    }
}
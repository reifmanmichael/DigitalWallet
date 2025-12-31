package com.example.digitalwallet.Model;

public class User {
    public String uid;
    public String displayName;
    public String email;
    public String phone;
    public double balance;
    public String profileColor; // New Field

    public User() {}

    public User(String uid, String displayName, String email, String phone, double balance, String profileColor) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.balance = balance;
        this.profileColor = profileColor;
    }
}
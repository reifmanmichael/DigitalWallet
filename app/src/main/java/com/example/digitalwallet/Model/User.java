package com.example.digitalwallet.Model;

public class User {
    public String uid, fullName, displayName, email, phone, address, accountType;
    public double balance; // Shekels

    public User() {} // Required for Firebase

    public User(String uid, String fullName, String displayName, String email, String phone, String address, String accountType) {
        this.uid = uid;
        this.fullName = fullName;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.accountType = accountType;
        this.balance = 0.00; // Default balance
    }
}
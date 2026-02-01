package com.example.digitalwallet.Model;

import java.util.HashMap;
import java.util.Map;

public class User {
    public String uid;
    public String displayName;
    public String email;
    public String phone;
    public double balance;
    public double bankBalance;
    public String profileColor;
    
    // Fields to prevent Firebase mapping warnings
    public Map<String, Object> transactions = new HashMap<>();
    public Map<String, Integer> frequencies = new HashMap<>();
    public Map<String, Boolean> saved_contacts = new HashMap<>();

    public User() {}

    public User(String uid, String displayName, String email, String phone, double balance, String profileColor) {
        this(uid, displayName, email, phone, balance, 0.0, profileColor);
    }

    public User(String uid, String displayName, String email, String phone, double balance, double bankBalance, String profileColor) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.balance = balance;
        this.bankBalance = bankBalance;
        this.profileColor = profileColor;
    }
}
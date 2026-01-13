package com.example.digitalwallet.Model;

public class Pocket {
    public String id;
    public String name;
    public double amount;
    public String type; // "All-purpose" or "Savings"
    public boolean isLocked;
    public boolean isClosed; // New field
    public String iconName;
    
    // Savings-specific fields
    public double interestRate;
    public long lockEndDate;
    public double initialDeposit;

    public Pocket() {}

    public Pocket(String id, String name, double amount, String type, boolean isLocked) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.type = type;
        this.isLocked = isLocked;
        this.isClosed = false;
    }
}
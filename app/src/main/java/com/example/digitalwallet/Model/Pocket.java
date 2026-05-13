package com.example.digitalwallet.Model;

public class Pocket {
    public String id;
    public String name;
    public double amount;
    public String type;
    public boolean isLocked;
    public boolean isClosed;
    public String iconName;


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
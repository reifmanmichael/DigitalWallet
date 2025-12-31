package com.example.digitalwallet.Model;

public class Transaction {
    public String id;
    public String type;
    public double amount;
    public long timestamp;
    public String relatedUserUid;
    public String relatedUserName;
    public String relatedUserColor; // New Field

    public Transaction() {}

    public Transaction(String id, String type, double amount, long timestamp, String relatedUserUid, String relatedUserName, String relatedUserColor) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
        this.relatedUserUid = relatedUserUid;
        this.relatedUserName = relatedUserName;
        this.relatedUserColor = relatedUserColor;
    }
}
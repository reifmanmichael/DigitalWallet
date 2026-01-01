package com.example.digitalwallet.Model;

public class Transaction {
    public String id;
    public String type; // sent, received
    public String status; // pending, completed, declined
    public double amount;
    public long timestamp;
    public String relatedUserUid;
    public String relatedUserName;
    public String relatedUserColor;
    public String initiatorUid; // Who started the transaction

    public Transaction() {}

    public Transaction(String id, String type, String status, double amount, long timestamp, String relatedUserUid, String relatedUserName, String relatedUserColor, String initiatorUid) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.amount = amount;
        this.timestamp = timestamp;
        this.relatedUserUid = relatedUserUid;
        this.relatedUserName = relatedUserName;
        this.relatedUserColor = relatedUserColor;
        this.initiatorUid = initiatorUid;
    }
}
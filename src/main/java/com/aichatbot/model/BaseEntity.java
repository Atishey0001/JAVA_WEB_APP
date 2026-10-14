package com.aichatbot.model;

import java.sql.Timestamp;

/** Inheritance with a purpose: every table row has an id and a creation time. */
public abstract class BaseEntity {

    private int id;
    private Timestamp createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

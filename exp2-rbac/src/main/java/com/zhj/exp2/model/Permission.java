package com.zhj.exp2.model;

public class Permission {
    private int id;
    private String permCode;
    private String permName;
    private String permType;

    public Permission() {
    }

    public Permission(int id, String permCode, String permName, String permType) {
        this.id = id;
        this.permCode = permCode;
        this.permName = permName;
        this.permType = permType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPermCode() {
        return permCode;
    }

    public void setPermCode(String permCode) {
        this.permCode = permCode;
    }

    public String getPermName() {
        return permName;
    }

    public void setPermName(String permName) {
        this.permName = permName;
    }

    public String getPermType() {
        return permType;
    }

    public void setPermType(String permType) {
        this.permType = permType;
    }

    @Override
    public String toString() {
        return String.format("%-5d %-15s %-15s %-10s", id, permCode, permName, permType);
    }
}
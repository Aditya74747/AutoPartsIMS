package com.autoparts.model;

public class User {
// User roles predefined in the system
    public enum Role { MANAGER, EMPLOYEE }

    private final int    userId;
    private final String username;
    private final Role   role;
    private final String fullName;
    private final String email;

    public User(int userId, String username, Role role, String fullName, String email) {
        this.userId   = userId;
        this.username = username;
        this.role     = role;
        this.fullName = fullName;
        this.email    = email;
    }

    public int     getUserId()  { return userId;   }
    public String  getUsername(){ return username; }
    public Role    getRole()    { return role;     }
    public String  getFullName(){ return fullName; }
    public String  getEmail()   { return email;    }
    public boolean isManager()  { return role == Role.MANAGER; }

    @Override
    public String toString() { return fullName + " [" + role + "]"; }
}

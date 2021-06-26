package com.avairebot.contracts.verification;

public class VerificationEntity {
    private long robloxId;
    private String robloxUsername;

    public VerificationEntity(Long id, String username) {
        this.robloxId = id;
        this.robloxUsername = username;
    }

    public long getRobloxId() {
        return robloxId;
    }

    public String getRobloxUsername() {
        return robloxUsername;
    }
}

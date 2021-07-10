package com.avairebot.contracts.verification;

public class VerificationEntity {

    private long robloxId;
    private String robloxUsername;
    private String provider;

    public VerificationEntity(Long id, String username, String provider) {
        this.robloxId = id;
        this.robloxUsername = username;
        this.provider = provider;
    }

    public long getRobloxId() {
        return robloxId;
    }

    public String getRobloxUsername() {
        return robloxUsername;
    }

    public String getProvider() {
        return provider;
    }
}

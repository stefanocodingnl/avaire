package com.avairebot.roblox;

import com.avairebot.roblox.api.user.RobloxUserAPIRoutes;
import com.avairebot.roblox.verification.VerificationManager;

public class RobloxAPIManager {
    private final RobloxUserAPIRoutes userAPI;
    private final VerificationManager verification;

    public RobloxAPIManager(RobloxUserAPIRoutes userAPI, VerificationManager verification) {
        this.userAPI = userAPI;
        this.verification = verification;
    }

    public RobloxUserAPIRoutes getUserAPI() {
        return userAPI;
    }

    public VerificationManager getVerification() {
        return verification;
    }
}

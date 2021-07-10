package com.avairebot.servlet.routes.v1;

import com.avairebot.AvaIre;
import com.avairebot.contracts.metrics.SparkRoute;
import com.avairebot.contracts.verification.VerificationEntity;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

public class GetRobloxUserByDiscordId extends SparkRoute {
    @Override
    public Object handle(Request request, Response response) throws Exception {
        String id = request.params("discordId");

        JSONObject root = new JSONObject();
        VerificationEntity verificationEntity = AvaIre.getInstance().getRobloxAPIManager().getVerification().fetchVerification(id, false);

        if (verificationEntity != null) {
            root.put("username", verificationEntity.getRobloxUsername());
            root.put("robloxId", verificationEntity.getRobloxId());
            root.put("provider", verificationEntity.getProvider());
            response.status(200);
        } else {
            root.put("error", true);
            root.put("message", "The ID " + id + " doesn't have a user in any verification database.");
            response.status(404);
        }

        return root;
    }
}

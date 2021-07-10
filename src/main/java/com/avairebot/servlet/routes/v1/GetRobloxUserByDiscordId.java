package com.avairebot.servlet.routes.v1;

import com.avairebot.AvaIre;
import com.avairebot.contracts.metrics.SparkRoute;
import com.avairebot.contracts.verification.VerificationEntity;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class GetRobloxUserByDiscordId extends SparkRoute {
    private static final Logger log = LoggerFactory.getLogger(PostGuildCleanup.class);

    @Override
    public Object handle(Request request, Response response) throws Exception {
        if (!hasValidVerificationAuthorizationHeader(request)) {
            log.warn("Unauthorized request, missing or invalid \"Authorization\" header give.");
            return buildResponse(response, 401, "Unauthorized request, missing or invalid \"Authorization\" header give.");
        }
        String id = request.params("discordId");

        JSONObject root = new JSONObject();
        VerificationEntity pinewood = AvaIre.getInstance().getRobloxAPIManager().getVerification().fetchVerification(id, true, "pinewood");

        if (pinewood == null) {
            pinewood = AvaIre.getInstance().getRobloxAPIManager().getVerification().fetchVerification(id, true, "rover");
        }

        if (pinewood == null) {
            pinewood = AvaIre.getInstance().getRobloxAPIManager().getVerification().fetchVerification(id, true, "bloxlink");
        }

        if (pinewood != null) {
            root.put("username", pinewood.getRobloxUsername());
            root.put("robloxId", pinewood.getRobloxId());
            root.put("provider", pinewood.getProvider());
            response.status(200);
        } else {
            root.put("error", true);
            root.put("message", "The ID " + id + " doesn't have a user in any verification database.");
            response.status(404);
        }

        return root;
    }
}

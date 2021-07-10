package com.avairebot.servlet.routes.v1;

import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Route;

public class GetDiscordIdsByRobloxId implements Route {
    @Override
    public Object handle(Request request, Response response) throws Exception {
        String id = request.params("robloxId");

        JSONObject root = new JSONObject();
        root.put("error", true);
        root.put("message", "This API has not been implemented yet.");
        response.status(404);

        return root;
    }
}

package com.avairebot.roblox.api.user;

import com.avairebot.AppInfo;
import com.avairebot.AvaIre;
import com.avairebot.requests.service.user.rank.RobloxUserGroupRankService;
import okhttp3.*;

import java.io.IOException;
import java.util.List;

public class RobloxUserAPIRoutes {

    private final MediaType json = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();
    private final AvaIre avaire;

    public RobloxUserAPIRoutes(AvaIre avaire) {this.avaire = avaire;}

    public List<RobloxUserGroupRankService.Data> getUserRanks(Long botAccount) {
        Request.Builder request = new Request.Builder()
            .addHeader("User-Agent", "Xeus v" + AppInfo.getAppInfo().version)
            .url("https://groups.roblox.com/v2/users/{userId}/groups/roles".replace("{userId}", botAccount.toString()));

        try (Response response = client.newCall(request.build()).execute()) {
            if (response.code() == 200) {
                RobloxUserGroupRankService grs = (RobloxUserGroupRankService) toService(response, RobloxUserGroupRankService.class);
                if (grs.hasData()) {
                    return grs.getData();
                }
            }
        } catch (IOException e) {
            AvaIre.getLogger().error("Failed sending request to Roblox API: " + e.getMessage());
        }
        return null;
    }

    public Object toService(Response response, Class<?> clazz) {
        return AvaIre.gson.fromJson(toString(response), clazz);
    }

    public String toString(Response response) {
        try {
            try (ResponseBody body = response.body()) {
                if (body != null) {
                    return body.string();
                }
            }
        } catch (IOException e) {
            AvaIre.getLogger().error("ERROR: ", e);
        }
        return null;
    }

}

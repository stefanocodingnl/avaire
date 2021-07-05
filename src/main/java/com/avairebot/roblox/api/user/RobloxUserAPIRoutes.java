package com.avairebot.roblox.api.user;

import com.avairebot.AppInfo;
import com.avairebot.AvaIre;
import com.avairebot.requests.service.user.inventory.RobloxGamePassService;
import com.avairebot.requests.service.user.rank.RobloxUserGroupRankService;
import com.avairebot.roblox.RobloxAPIManager;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;

public class RobloxUserAPIRoutes {

    private final AvaIre avaire;
    private final RobloxAPIManager manager;
    public RobloxUserAPIRoutes(AvaIre avaire, RobloxAPIManager robloxAPIManager) {this.avaire = avaire; this.manager = robloxAPIManager;}

    public List<RobloxUserGroupRankService.Data> getUserRanks(Long botAccount) {
        Request.Builder request = new Request.Builder()
            .addHeader("User-Agent", "Xeus v" + AppInfo.getAppInfo().version)
            .url("https://groups.roblox.com/v2/users/{userId}/groups/roles".replace("{userId}", botAccount.toString()));

        try (Response response = manager.getClient().newCall(request.build()).execute()) {
            if (response.code() == 200) {
                RobloxUserGroupRankService grs = (RobloxUserGroupRankService) manager.toService(response, RobloxUserGroupRankService.class);
                if (grs.hasData()) {
                    return grs.getData();
                }
            }
        } catch (IOException e) {
            AvaIre.getLogger().error("Failed sending request to Roblox API: " + e.getMessage());
        }
        return null;
    }


    public List<RobloxGamePassService.Datum> getUserGamePass(Long userId, Long gamepassId) {
        Request.Builder request = new Request.Builder()
                .addHeader("User-Agent", "Xeus v" + AppInfo.getAppInfo().version)
                .url("https://inventory.roblox.com/v1/users/{userId}/items/GamePass/{gamepassId}"
                        .replace("{userId}", userId.toString())
                        .replace("{gamepassId}", gamepassId.toString()));

        try (Response response = manager.getClient().newCall(request.build()).execute()) {
            if (response.code() == 200) {
                RobloxGamePassService grs = (RobloxGamePassService) manager.toService(response, RobloxGamePassService.class);
                if (grs.hasData()) {
                    return grs.getData();
                }
            }
        } catch (IOException e) {
            AvaIre.getLogger().error("Failed sending request to Roblox API: " + e.getMessage());
        }
        return null;
    }

}

package com.avairebot.roblox.api.group;

import com.avairebot.AppInfo;
import com.avairebot.AvaIre;
import com.avairebot.requests.service.group.GroupRanksService;
import com.avairebot.roblox.RobloxAPIManager;
import com.avairebot.utilities.CacheUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import okhttp3.Request;
import okhttp3.Response;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GroupAPIRoutes {

    private final Cache<Integer, GroupRanksService> cache = CacheBuilder.newBuilder()
            .recordStats()
            .expireAfterWrite(300, TimeUnit.SECONDS)
            .build();

    private final RobloxAPIManager manager;
    private final AvaIre avaire;

    public GroupAPIRoutes(AvaIre avaire, RobloxAPIManager robloxAPIManager) {
        this.avaire = avaire;
        this.manager = robloxAPIManager;
    }


    @Nullable
    @CheckReturnValue
    public GroupRanksService fetchGroupRanks(Integer groupId, boolean fromCache) {
        if (!fromCache) {
            return forgetAndCache(groupId);
        }
        return (GroupRanksService) CacheUtil.getUncheckedUnwrapped(cache, groupId, () -> callGroupRanksFromRobloxAPI(groupId));
    }

    private GroupRanksService forgetAndCache(Integer groupId) {
        if (cache.getIfPresent(groupId) != null) {
            cache.invalidate(groupId);
            return callGroupRanksFromRobloxAPI(groupId);
        }
        return callGroupRanksFromRobloxAPI(groupId);
    }

    private GroupRanksService callGroupRanksFromRobloxAPI(Integer groupId) {
        Request.Builder request = new Request.Builder()
                .addHeader("User-Agent", "Xeus v" + AppInfo.getAppInfo().version)
                .url("https://groups.roblox.com/v1/groups/{groupId}/roles".replace("{groupId}", String.valueOf(groupId)));

        try (Response response = manager.getClient().newCall(request.build()).execute()) {
            if (response.code() == 200) {
                GroupRanksService grs = (GroupRanksService) manager.toService(response, GroupRanksService.class);
                if (grs.getRoles().size() > 0) {
                    return grs;
                }
            }
        } catch (IOException e) {
            AvaIre.getLogger().error("Failed sending request to Roblox API: " + e.getMessage());
        }
        return null;
    }

}

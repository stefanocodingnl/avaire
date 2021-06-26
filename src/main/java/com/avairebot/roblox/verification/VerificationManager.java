package com.avairebot.roblox.verification;

import com.avairebot.AppInfo;
import com.avairebot.AvaIre;
import com.avairebot.contracts.verification.VerificationEntity;
import com.avairebot.utilities.CacheUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.reflect.TypeToken;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class VerificationManager {

    private final AvaIre avaire;

    private final MediaType json = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    public final Cache<String, VerificationEntity> cache = CacheBuilder.newBuilder()
        .recordStats()
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build();

    public VerificationManager(AvaIre avaire) {
        this.avaire = avaire;
    }



    @Nullable
    @CheckReturnValue
    public VerificationEntity fetchVerification(String discordUserId, boolean fromCache) {
        if (!fromCache) {
            return forgetAndCache(discordUserId);
        }
        return (VerificationEntity) CacheUtil.getUncheckedUnwrapped(cache, discordUserId, () -> callUserFromRoverAPI(discordUserId));
    }

    private VerificationEntity forgetAndCache(String discordUserId) {
        if (cache.getIfPresent(discordUserId) != null) {
            cache.invalidate(discordUserId);
            return callUserFromRoverAPI(discordUserId);
        }
        return callUserFromRoverAPI(discordUserId);
    }

    @Nullable
    private VerificationEntity callUserFromRoverAPI(String discordUserId) {
        Request.Builder request = new Request.Builder()
            .addHeader("User-Agent", "Xeus v" + AppInfo.getAppInfo().version)
            .url("https://verify.eryn.io/api/user/" + discordUserId);

        try (Response response = client.newCall(request.build()).execute()) {
            if (response.code() == 200 && response.body() != null) {
                HashMap<String, String> responseJson = AvaIre.gson.fromJson(response.body().string(), new TypeToken<HashMap<String, String>>(){}.getType());
                VerificationEntity verificationEntity = new VerificationEntity(Long.valueOf(responseJson.get("robloxId")), responseJson.get("robloxUsername"));

                cache.put(discordUserId, verificationEntity);
                return verificationEntity;
            } else if (response.code() == 404) {
                return null;
            } else {
                throw new Exception("Rover API returned something else then 200, please retry.");
            }
        } catch (IOException e) {
            AvaIre.getLogger().error("Failed sending request to Roblox API: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}

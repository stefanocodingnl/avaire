package com.avairebot.roblox.verification;

import com.avairebot.AppInfo;
import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.verification.VerificationEntity;
import com.avairebot.database.transformers.VerificationTransformer;
import com.avairebot.requests.service.group.GuildRobloxRanksService;
import com.avairebot.requests.service.user.rank.RobloxUserGroupRankService;
import com.avairebot.roblox.RobloxAPIManager;
import com.avairebot.utilities.CacheUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import okhttp3.Request;
import okhttp3.Response;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VerificationManager {

    public final Cache<String, VerificationEntity> cache = CacheBuilder.newBuilder()
            .recordStats()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();
    private final AvaIre avaire;
    private final RobloxAPIManager manager;

    public VerificationManager(AvaIre avaire, RobloxAPIManager robloxAPIManager) {
        this.avaire = avaire;
        this.manager = robloxAPIManager;
    }

    public boolean verify(CommandMessage context, boolean useCache) {
        return verify(context, useCache);
    }

    public boolean verify(CommandMessage context, Member m) {
        return verify(context, m, true);
    }

    public boolean verify(CommandMessage context) {
        return verify(context, context.member, true);
    }

    public boolean verify(CommandMessage c, Member m, boolean useCache) {
        if (c.getMember() == null) {
            return errorMessage(c, "Member entity doesn't exist. Verification cancelled on " + m.getEffectiveName());
        }


        VerificationEntity verificationEntity = fetchVerification(m.getId(), useCache);
        if (verificationEntity == null) {
            return errorMessage(c, "Xeus coudn't find your profile on the RoVer API. Please verify your account on https://verify.eryn.io/.");
        }

        VerificationTransformer v = c.getVerificationTransformer();
        if (v == null) {
            return errorMessage(c, "The VerificationTransformer seems to have broken, please consult the developer of the bot.");
        }

        if (v.getNicknameFormat() == null) {
            return errorMessage(c, "The nickname format is not set (Wierd, it's the default but ok).");
        }

        c.getGuild().modifyNickname(m, v.getNicknameFormat().replace("%USERNAME%", verificationEntity.getRobloxUsername())).queue(l -> {}, f -> {
            c.makeError("I do not have the permission to modify your nickname, or your highest rank is above mine.").queue();
        });

        if (v.getRanks() == null) {
            return errorMessage(c, "Ranks have not been setup on this guild yet. Please ask the admins to setup the roles on this server.");
        }

        List<RobloxUserGroupRankService.Data> robloxRanks = manager.getUserAPI().getUserRanks(verificationEntity.getRobloxId());

        if (robloxRanks == null || robloxRanks.size() == 0) {
            return errorMessage(c, verificationEntity.getRobloxUsername() + " does not have any ranks or groups on his name.");
        }


        List<Role> rolesToGive = new ArrayList<>();
        List<Role> rolesToRemove = new ArrayList<>();
        for (RobloxUserGroupRankService.Data robloxRank : robloxRanks) {
            long groupId = robloxRank.getGroup().getId();
            int rankId = robloxRank.getRole().getRank();

            GuildRobloxRanksService guildRanks = (GuildRobloxRanksService) manager.toService(v.getRanks(), GuildRobloxRanksService.class);
            List<GuildRobloxRanksService.GroupRankBinding> ranksList = guildRanks.getGroupRankBindings();

            for (GuildRobloxRanksService.GroupRankBinding rankBinding : ranksList) {
                for (GuildRobloxRanksService.Group groupRanks : rankBinding.getGroups()) {
                    if (groupId == groupRanks.getId()) {
                        if (groupRanks.getRanks().contains(rankId)) {
                            Role r = c.getGuild().getRoleById(rankBinding.getRole());

                            if (r != null) {
                                rolesToGive.add(r);
                            }
                        }
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();

        if (rolesToGive.size() > 0) {
            sb.append("Roles to add: ");
            for (Role r : rolesToGive) {
                sb.append("\n- `").append(r.getName()).append("`");
            }
        } else {
            sb.append("No roles have been added.");
        }

        c.makeInfo(sb.toString()).queue();

        return false;
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

        try (Response response = manager.getClient().newCall(request.build()).execute()) {
            if (response.code() == 200 && response.body() != null) {
                HashMap<String, String> responseJson = AvaIre.gson.fromJson(response.body().string(), new TypeToken<HashMap<String, String>>() {
                }.getType());
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

    private boolean errorMessage(CommandMessage em, String s) {
        em.makeError(s).setTitle("Error during verification!").requestedBy(em).setTimestamp(Instant.now()).queue();
        return false;
    }

}

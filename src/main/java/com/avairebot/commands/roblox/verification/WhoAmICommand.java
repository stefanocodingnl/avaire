package com.avairebot.commands.roblox.verification;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.verification.VerificationEntity;
import com.avairebot.database.collection.Collection;
import com.avairebot.database.collection.DataRow;
import com.avairebot.requests.service.user.rank.RobloxUserGroupRankService;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WhoAmICommand extends Command {
    public WhoAmICommand(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "WhoAmI Command";
    }

    @Override
    public String getDescription() {
        return "Tells you about what commands AvaIre has, what they do, and how you can use them.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Arrays.asList(
            "`:command` - Who are you on roblox?"
        );
    }

    @Override
    public List<String> getExampleUsage() {
        return Arrays.asList(
            "`:command`"
        );
    }

    @Override
    public List<String> getTriggers() {
        return Collections.singletonList("whoami");
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        VerificationEntity verifiedRobloxUser = avaire.getRobloxAPIManager().getVerification().fetchVerification(context.getMember().getId(), true);
        if (verifiedRobloxUser == null) {
            context.makeError("No account found on the RoVer API. Please verify yourself on: https://verify.eryn.io/").requestedBy(context).queue();
            return false;
        }

        try {
            Collection qb = avaire.getDatabase().newQueryBuilder(Constants.GUILD_TABLE_NAME).orderBy("roblox_group_id").get();
            StringBuilder sb = new StringBuilder();

            for (DataRow data : qb) {
                if (data.getString("roblox_group_id") != null) {
                    List<RobloxUserGroupRankService.Data> ranks = avaire.getRobloxAPIManager().getUserAPI().getUserRanks(verifiedRobloxUser.getRobloxId());
                    for (RobloxUserGroupRankService.Data rank : ranks) {
                        if (rank.getGroup().getId() == data.getLong("roblox_group_id")) {
                            sb.append("\n**").append(data.getString("name")).append("** - `").append(rank.getRole().getName()).append("` (*").append(rank.getRole().getRank()).append("*)");
                        }
                    }
                }
            }

            context.makeInfo("**Roblox Username**: :rusername\n**Roblox ID**: :userId\nRanks:\n:userRanks")
                .set("rusername", verifiedRobloxUser.getRobloxUsername())
                .set("userId", verifiedRobloxUser.getRobloxId())
                .set("userRanks", sb.toString()).queue();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }
}

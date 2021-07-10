package com.avairebot.commands.roblox.verification;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.database.controllers.VerificationController;
import com.avairebot.database.transformers.VerificationTransformer;
import com.avairebot.requests.service.group.GroupRanksService;
import com.avairebot.requests.service.group.GuildRobloxRanksService;
import com.avairebot.roblox.RobloxAPIManager;
import com.avairebot.utilities.MentionableUtil;
import com.avairebot.utilities.NumberUtil;
import com.avairebot.utilities.RoleUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.*;

public class VerificationCommand extends Command {
    public VerificationCommand(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "Verification Command";
    }

    @Override
    public String getDescription() {
        return "Control multiple settings for the bots verification counterparts.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Arrays.asList(
                "`:command` - Bind a role to a rank."
        );
    }

    @Override
    public List<String> getExampleUsage() {
        return Arrays.asList(
                "`:command`"
        );
    }

    @Override
    public List<String> getMiddleware() {
        return Arrays.asList(
                "isOfficialPinewoodGuild",
                "isManagerOrHigher"
        );
    }

    @Override
    public List<String> getTriggers() {
        return Collections.singletonList("verification");
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (context.getGuildTransformer() == null) {
            context.makeError("Can't get the GuildTransformer, please try again later.").queue();
            return false;
        }

        if (context.getVerificationTransformer() == null) {
            context.makeError("Can't get the VerificationTransformer, please try again later.").queue();
            return false;
        }


        RobloxAPIManager manager = avaire.getRobloxAPIManager();
        if (args.length > 0) {
            switch (args[0]) {
                case "sac":
                case "set-announce-channel":
                    return setAnnouncementChannel(context, args);
                case "vc":
                case "verifychannel":
                    return setVerifyChannel(context, args);
                case "b":
                case "bind":
                    return runBindCommand(context, args);
                case "createverifychannel":
                    return createVerifyChannel(context, args);
                case "joindm":
                    return setJoinDM(context, args);
                case "nickname":
                    return setNicknameUsers(context, args);
                case "unbindall":
                    return unbindAllSubcommand(context, args);
                case "list":
                    return listBoundRoles(context, manager);
                case "creategroupranks":
                    return createGroupRanks(context, args, manager);
                default:
                    context.makeError("Invalid argument given.").queue();
                    return false;
            }
        }
        return false;
    }

    private boolean listBoundRoles(CommandMessage context, RobloxAPIManager manager) {
        GuildRobloxRanksService guildRanks = (GuildRobloxRanksService) manager.toService(context.getVerificationTransformer().getRanks(), GuildRobloxRanksService.class);
        if (guildRanks != null) {
            StringBuilder sb = new StringBuilder();
            for (GuildRobloxRanksService.GroupRankBinding groupRankBinding : guildRanks.getGroupRankBindings()) {
                Guild g = context.getGuild();
                Role r = g.getRoleById(groupRankBinding.getRole());

                if (r != null) {
                    sb.append("**").append(r.getName()).append("** - ``");
                    for (GuildRobloxRanksService.Group s : groupRankBinding.getGroups()) {
                        sb.append(s.getId()).append("`` > ```yaml\n").append(s.getRanks().get(0)).append(s.getRanks().get(s.getRanks().size() - 1)).append("```");
                    }
                }
            }
            context.makeInfo(sb.toString()).queue();
            return true;
        } else {
            context.makeError("Groups have not been setup yet. Please set them up using `:command creategroupranks (group id)`").queue();
            return false;
        }
    }

    private boolean createGroupRanks(CommandMessage context, String[] args, RobloxAPIManager manager) {
        if (args.length > 1) {
            if (args.length == 2 && NumberUtil.isNumeric(args[1])) {
                GroupRanksService ranks = manager.getGroupAPI().fetchGroupRanks(Integer.valueOf(args[1]), false);
                context.makeInfo("Ranks have not been setup yet, loading ranks from Roblox API and binding them to the guild based on the roblox group `:gId`.").set("gId", args[1]).queue();
                return generateRobloxRanks(context, ranks);
            }
        }
        if (context.getGuildTransformer().getRobloxGroupId() != 0) {
            GroupRanksService ranks = manager.getGroupAPI().fetchGroupRanks(context.getGuildTransformer().getRobloxGroupId(), false);
            context.makeInfo("Ranks have not been setup yet, loading ranks from Roblox API and binding them to the guild based on the main group ID.").queue();
            return generateRobloxRanks(context, ranks);
        }
        return sendErrorMessage(context, "Guild doesn't have a main group ID configured, neither did you supply one. Please try again.");
    }

    private boolean setJoinDM(CommandMessage context, String[] args) {
        return false;
    }

    private boolean unbindAllSubcommand(CommandMessage context, String[] args) {
        return false;
    }

    private boolean createVerifyChannel(CommandMessage context, String[] args) {
        createVerificationCategory(context.guild).queue();
        return false;
    }

    private boolean runBindCommand(CommandMessage context, String[] args) {
        if (args.length < 2) {
            //return goToBindStart(context, args);
        }
        VerificationTransformer verificationTransformer = context.getVerificationTransformer();
        if (verificationTransformer == null) {
            context.makeError("The VerificationTransformer seems to have broken, please consult the developer of the bot.").queue();
            return false;
        }

        if (verificationTransformer.getNicknameFormat() == null) {
            context.makeError("The nickname format is not set (Wierd, it's the default but ok then, command cancelled).").queue();
            return false;
        }

        if (verificationTransformer.getRanks() == null || verificationTransformer.getRanks().length() < 2) {
            context.makeError("Ranks have not been setup on this guild yet. Please ask the admins to setup the roles on this server.").queue();
            return false;
        }

        GuildRobloxRanksService guildRanks = (GuildRobloxRanksService) avaire.getRobloxAPIManager().toService(context.getVerificationTransformer().getRanks(), GuildRobloxRanksService.class);


        return false;
    }

    private boolean setNicknameUsers(CommandMessage context, String[] args) {
        return false;
    }


    public RestAction<Message> createVerificationCategory(Guild guild) {
        return guild.createCategory("Verification")
                .addPermissionOverride(guild.getSelfMember(), EnumSet.of(Permission.MESSAGE_WRITE, Permission.MESSAGE_READ), null)
                .flatMap((category) -> category.createTextChannel("verify"))
                .flatMap((channel) -> channel.sendMessage("Hello! In this channel, all verification commands are being posted. All messages (eventually) get deleted!"))
                .flatMap((channel) -> channel.getCategory().createTextChannel("verify-instructions")
                        .addPermissionOverride(guild.getPublicRole(), EnumSet.of(Permission.MESSAGE_READ), EnumSet.of(Permission.MESSAGE_WRITE)))
                .flatMap((channel) -> channel.sendMessage("This server uses a Roblox verification system. In order to unlock all the features of this server, you'll need to verify your Roblox account with your Discord account!\n" +
                        "\n" +
                        "Visit https://verify.eryn.io/ and follow the instructions. Then, say !verify in #verify and it will update you accordingly."));
    }


    private boolean setAnnouncementChannel(CommandMessage context, String[] args) {
        if (args.length == 1) {
            context.makeError("Please run this argument again with the ID of the text channel you want to use (Or mention the channel).").queue();
            return false;
        }

        if (args.length > 2) {
            context.makeError("Please only give me the channel id you'd like to use.").queue();
            return false;
        }

        GuildChannel gc = MentionableUtil.getChannel(context.getMessage(), args);
        if (gc != null) {
            if (gc.getType().equals(ChannelType.TEXT)) {
                return changeSettingTo(context, gc.getIdLong(), "announce_channel");
            }
        }
        context.makeError("Unable to update channel id.").queue();
        return false;
    }

    private boolean setVerifyChannel(CommandMessage context, String[] args) {
        if (args.length == 1) {
            context.makeError("Please run this argument again with the ID of the text channel you want to use (Or mention the channel).").queue();
            return false;
        }

        if (args.length > 2) {
            context.makeError("Please only give me the channel id you'd like to use.").queue();
            return false;
        }

        GuildChannel gc = MentionableUtil.getChannel(context.getMessage(), args);
        if (gc != null) {
            if (gc.getType().equals(ChannelType.TEXT)) {
                return changeSettingTo(context, gc.getIdLong(), "verify_channel");
            }
        }
        context.makeError("Unable to update channel id.").queue();
        return false;
    }

    private boolean changeSettingTo(CommandMessage context, long tc, String setting) {
        try {
            avaire.getDatabase().newQueryBuilder(Constants.VERIFICATION_TABLE_NAME).where("id", context.getGuild().getId())
                    .update(m -> {
                        m.set(setting, tc);
                        context.makeSuccess("Updated `:setting` to `:value` in the VerificationTransformer")
                                .set("value", tc).set("setting", setting).queue();
                    });
            return true;
        } catch (SQLException throwables) {
            context.makeError("Something went wrong trying to update `:setting` to `:value`.")
                    .set("value", tc).set("setting", setting)
                    .queue();
            return false;
        }
    }

    private boolean generateRobloxRanks(CommandMessage context, GroupRanksService ranks) {
        context.makeInfo("```json\n" + buildPayload(context, ranks) + "\n```").queue();
        try {
            avaire.getDatabase().newQueryBuilder(Constants.VERIFICATION_TABLE_NAME).where("id", context.getGuild().getId()).update(m -> {
                m.set("ranks", buildPayload(context, ranks), true);
            });
            VerificationController.forgetCache(context.getGuild().getIdLong());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }

    private JSONObject buildPayload(CommandMessage context, GroupRanksService ranks) {
        JSONObject groupRankBindings = new JSONObject();
        JSONArray groupRanksBindings = new JSONArray();
        for (GroupRanksService.Role role : ranks.getRoles()) {
            Guild g = context.getGuild();
            List<Role> r = g.getRolesByName(role.getName(), true);
            if (r.size() > 0) {
                if (RoleUtil.canBotInteractWithRole(context.getMessage(), r.get(0))) {
                    JSONObject roleObject = new JSONObject();
                    roleObject.put("role", r.get(0).getId());

                    JSONArray groups = new JSONArray();
                    JSONObject group = new JSONObject();
                    group.put("id", ranks.getGroupId());
                    List<Integer> Rranks = new ArrayList<>();
                    Rranks.add(role.getRank());
                    group.put("ranks", Rranks);
                    groups.put(group);

                    roleObject.put("groups", groups);

                    groupRanksBindings.put(roleObject);
                }
            }
        }
        groupRankBindings.put("groupRankBindings", groupRanksBindings);
        context.makeInfo("```json\n" + groupRanksBindings.toString() + "\n```").queue();
        return groupRankBindings;
    }
}

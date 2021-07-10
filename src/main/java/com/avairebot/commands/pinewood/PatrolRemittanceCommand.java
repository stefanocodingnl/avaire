package com.avairebot.commands.pinewood;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.blacklist.features.FeatureScope;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import com.avairebot.database.collection.DataRow;
import com.avairebot.database.query.QueryBuilder;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.factories.RequestFactory;
import com.avairebot.requests.Request;
import com.avairebot.requests.Response;
import com.avairebot.requests.service.user.rank.RobloxUserGroupRankService;
import com.avairebot.utilities.CheckPermissionUtil;
import com.avairebot.utilities.MentionableUtil;
import com.avairebot.utilities.NumberUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.avairebot.utils.JsonReader.readJsonFromUrl;

public class PatrolRemittanceCommand extends Command {

    public static final Cache<Long, Guild> cache = CacheBuilder.newBuilder()
        .recordStats()
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build();

    public PatrolRemittanceCommand(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "Patrol Remmittance Command";
    }

    @Override
    public String getDescription() {
        return "Run this command to request points for your division (If they allow so).";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Collections.singletonList(
            "`:command` - Start the command, questions for evidence will be asked. Linked to the roblox account you're currently linked with on roblox."
        );
    }

    @Override
    public List<String> getExampleUsage() {
        return Arrays.asList(
            "`:command` - Start the command, questions for evidence will be asked.",
            "`:command set-channel #channel/722805575053738034` - Set the channel mentioned or the ID to the id"
        );
    }


    @Override
    public List<String> getTriggers() {
        return Arrays.asList("patrol-remittance", "pr");
    }

    @Nonnull
    @Override
    public List<CommandGroup> getGroups() {
        return Collections.singletonList(
            CommandGroups.MISCELLANEOUS
        );
    }

    @Override
    public List <String> getMiddleware() {
        return Arrays.asList(
            "isOfficialPinewoodGuild",
            "throttle:guild,1,30"
        );
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        GuildTransformer transformer = context.getGuildTransformer();
        if (transformer == null) {
            context.makeError("Something went wrong loading the guild settings. Please try again later.").queue();
            return false;
        }
        if (args.length > 0) {
            if (CheckPermissionUtil.getPermissionLevel(context).getLevel() >= CheckPermissionUtil.GuildPermissionCheckType.MANAGER.getLevel()) {
                switch (args[0]) {
                    case "sc":
                    case "set-channel": {
                        return runSetRemittanceChannel(context, args);
                    }
                    case "clear":
                    case "reset": {
                        return runClearAllChannelsFromDatabase(context);
                    }
                    default: {
                        return sendErrorMessage(context, "Invalid argument given.");
                    }
                }
            }
        }

        return startRemittanceWaiter(context, args);
    }

    private boolean startRemittanceWaiter(CommandMessage context, String[] args) {
        if (checkAccountAge(context)) {
            context.makeError("Sorry, but only discord accounts that are older then 3 days are allowed to make actual reports.\nIf this is an important violation, please contact a trainer.").queue();
            return false;
        }

        context.makeInfo("<a:loading:742658561414266890> Loading servers that are using recorded remittance...").queue(l -> {
            QueryBuilder qb = avaire.getDatabase().newQueryBuilder(Constants.GUILD_TABLE_NAME).orderBy("patrol_remittance_channel");
            try {

                StringBuilder sb = new StringBuilder();
                qb.get().forEach(dataRow -> {
                    if (dataRow.getString("patrol_remittance_channel") != null) {
                        Guild g = avaire.getShardManager().getGuildById(dataRow.getString("id"));
                        Emote e = avaire.getShardManager().getEmoteById(dataRow.getString("patrol_remittance_emote_id"));

                        if (g != null && e != null) {
                            sb.append("``").append(g.getName()).append("`` - ").append(e.getAsMention()).append("\n");
                            l.addReaction(e).queue();
                        } else {
                            context.makeError("Either the guild or the emote can't be found in the database, please check with the developer.").queue();
                        }
                    }

                });
                l.addReaction("❌").queue();
                l.editMessageEmbeds(context.makeInfo("Welcome to the recorded remittance system. With this feature, you can record your patrolling/raiding for groups that have this enabled!\n\n" + sb.toString()).buildEmbed()).queue(
                    message -> {
                        avaire.getWaiter().waitForEvent(GuildMessageReactionAddEvent.class, event -> {
                                return event.getMember().equals(context.member) && event.getMessageId().equalsIgnoreCase(message.getId());
                            }, react -> {
                                try {
                                    if (react.getReactionEmote().getName().equalsIgnoreCase("❌")) {
                                        message.editMessageEmbeds(context.makeWarning("Cancelled the system").buildEmbed()).queue();
                                        message.clearReactions().queue();
                                        return;
                                    }
                                    DataRow d = qb.where("patrol_remittance_emote_id", react.getReactionEmote().getId()).get().get(0);

                                    TextChannel c = avaire.getShardManager().getTextChannelById(d.getString("patrol_remittance_channel"));
                                    if (c != null) {
                                        if (avaire.getFeatureBlacklist().isBlacklisted(context.getAuthor(), c.getGuild().getIdLong(), FeatureScope.PATROL_REMITTANCE)) {
                                            message.editMessageEmbeds(context.makeError("You have been blacklisted from requesting a remittance for this guild. Please ask a **Level 4** (Or higher) member to remove you from the ``"+c.getGuild().getName()+"`` remittance blacklist.").buildEmbed()).queue();
                                            return;
                                        }
                                        message.editMessageEmbeds(context.makeInfo(d.getString("patrol_remittance_message", "A remittance message for ``:guild`` could not be found. Ask the HR's of ``:guild`` to set one.\n" +
                                            "If you'd like to request remittance, please enter evidence of this in right now." +"``` ```\n\nPlease enter a **LINK** to evidence. " +
                                            "\n**Remember, you may only post once per 24 hours. The video may *only be 2 hours* and has to have a *minimum of 30 minutes* in duration**\n\n" +
                                            "**We're accepting**:\n" +
                                            "- [YouTube Links](https://www.youtube.com/upload)\n" +
                                            "- [Streamable](https://streamable.com)\n" +
                                            "If you want a link/video service added, please ask ``Stefano#7366``")).set("guild", d.getString("name")).set(":user", context.member.getEffectiveName()).buildEmbed()).queue(
                                            nameMessage -> {
                                                avaire.getWaiter().waitForEvent(GuildMessageReceivedEvent.class, m -> m.getMember().equals(context.member) && message.getChannel().equals(l.getChannel()) && checkEvidenceAcceptance(context, m),
                                                    content -> {
                                                        goToStep2(context, message, content, d, c);
                                                    },
                                                    90, TimeUnit.SECONDS,
                                                    () -> message.editMessage("You took to long to respond, please restart the report system!").queue());


                                            }
                                        );
                                        message.clearReactions().queue();
                                    } else {
                                        context.makeError("The guild doesn't have a (valid) channel for suggestions").queue();
                                    }

                                } catch (SQLException throwables) {
                                    context.makeError("Something went wrong while checking the database, please check with the developer for any errors.").queue();
                                }
                            },
                            5, TimeUnit.MINUTES,
                            () -> {
                                message.editMessage("You took to long to respond, please restart the report system!").queue();
                            });
                    }
                );

            } catch (SQLException throwables) {
                context.makeError("Something went wrong while checking the database, please check with the developer for any errors.").queue();
            }
        });
        return true;
    }


    private boolean checkIfBlacklisted(Long requestedId, TextChannel c) {

        if (c.getGuild().getId().equalsIgnoreCase("438134543837560832")) {
            return avaire.getBlacklistManager().getPBSTBlacklist().contains(requestedId);
        } else if (c.getGuild().getId().equalsIgnoreCase("572104809973415943")) {
            return avaire.getBlacklistManager().getTMSBlacklist().contains(requestedId);
        } else {
            return false;
        }
    }

    private void goToStep2(CommandMessage context, Message message, GuildMessageReceivedEvent content, DataRow d, TextChannel c) {
        {
            List<Message> messagesToRemove = new ArrayList<>();
            messagesToRemove.add(content.getMessage());
            if (content.getMessage().getContentRaw().equalsIgnoreCase("cancel")) {
                message.editMessageEmbeds(context.makeWarning("Cancelled the system").buildEmbed()).queue();
                removeAllUserMessages(messagesToRemove);
                return;
            }

            Long requestedId = getRobloxId(context.getMember().getEffectiveName());
            if (requestedId == 0L) {
                context.makeError("Sorry, but your nickname does not exist on roblox. Please run ``!verify`` to get your nickname updated.").queue();
                removeAllUserMessages(messagesToRemove);
                return;
            }

            boolean isBlacklisted = checkIfBlacklisted(requestedId, c);
            if (isBlacklisted) {
                message.editMessageEmbeds(context.makeWarning("You're blacklisted in ``" + c.getGuild().getName() + "``, for this reason you will not be allowed to request a remittance.").buildEmbed()).queue();
                removeAllUserMessages(messagesToRemove);
                return;
            }

            boolean hasNotExpired = cache.getIfPresent(requestedId) == c.getGuild();
            if (hasNotExpired) {
                context.makeError("You've already submitted a remittance request for `:guildName`, please wait 24 hours after the last time you've submitted a remittance request.")
                    .set("guildName", c.getGuild().getName()).queue();
                message.editMessage("ERROR. PLEASE CHECK BELOW").embed(null).queue();
                removeAllUserMessages(messagesToRemove);
                return;
            }

            if (d.getInt("roblox_group_id") != 0) {
                Request requestedRequest = RequestFactory.makeGET("https://groups.roblox.com/v1/users/" + requestedId + "/groups/roles");
                requestedRequest.send((Consumer<Response>) response -> {
                    if (response.getResponse().code() == 200) {
                        RobloxUserGroupRankService grs = (RobloxUserGroupRankService) response.toService(RobloxUserGroupRankService.class);
                        Optional<RobloxUserGroupRankService.Data> b = grs.getData().stream().filter(g -> g.getGroup().getId() == d.getInt("roblox_group_id")).findFirst();

                        if (b.isPresent()) {
                            startConfirmationWaiter(context, message, b, d, content, messagesToRemove);
                        } else {
                            //context.makeInfo(String.valueOf(response.getResponse().code())).queue();
                            context.makeError("You're not in ``:guild``, please check if this is correct or not.").set("guild", d.getString("name")).queue();
                            removeAllUserMessages(messagesToRemove);
                        }
                    }
                });
            } else {
                startConfirmationWaiter(context, message, Optional.empty(), d, content, messagesToRemove);
            }
        }
    }

    private void startConfirmationWaiter(CommandMessage context, Message message, Optional<RobloxUserGroupRankService.Data> b, DataRow d, GuildMessageReceivedEvent content, List<Message> messagesToRemove) {
        Button b1 = Button.success("yes:" + message.getId(), "Yes").withEmoji(Emoji.fromUnicode("✅"));
        Button b2 = Button.danger("no:" + message.getId(), "No").withEmoji(Emoji.fromUnicode("❌"));


        message.editMessageEmbeds(context.makeInfo("Ok, so. I've collected everything you've told me. And this is the data I got:\n\n" +
            "**Username**: " + context.getMember().getEffectiveName() + "\n" +
            "**Group**: " + d.getString("name") + "\n" + (b.map(data -> "**Rank**: " + data.getRole().getName() + "\n").orElse("\n")) +
            "**Evidence**: \n" + content.getMessage().getContentRaw() +
            "\n\nIs this correct?").buildEmbed()).setActionRow(b1.asEnabled(), b2.asEnabled()).queue(l -> {
            //l.addReaction("✅").queue();
            //l.addReaction("❌").queue();
            avaire.getWaiter().waitForEvent(ButtonClickEvent.class, r -> isValidMember(r, context, l), send -> {
                if (send.getButton().getEmoji().getName().equalsIgnoreCase("❌") || send.getButton().getEmoji().getName().equalsIgnoreCase("x")) {
                    message.editMessageEmbeds(context.makeSuccess("Remittance has been canceled, if you want to restart the report. Do ``!pr`` in any bot-commands channel.").buildEmbed()).setActionRows(Collections.emptyList()).queue();
                    removeAllUserMessages(messagesToRemove);
                } else if (send.getButton().getEmoji().getName().equalsIgnoreCase("✅")) {
                    message.editMessage("Report has been \"sent\".").setActionRows(Collections.emptyList()).queue();
                    sendReport(context, message, b, d, context.getMember().getEffectiveName(), content.getMessage().getContentRaw(), messagesToRemove);
                    removeAllUserMessages(messagesToRemove);
                } else {
                    message.editMessage("Invalid button given, report deleted!").setActionRows(Collections.emptyList()).queue();
                    removeAllUserMessages(messagesToRemove);
                }
            }, 5, TimeUnit.MINUTES, () -> {
                removeAllUserMessages(messagesToRemove);
                message.editMessage("You took to long to respond, please restart the report system!").queue();
            });
        });
    }

    private void removeAllUserMessages(List<Message> messagesToRemove) {
        for (Message m : messagesToRemove) {
            m.delete().queue();
        }
    }

    private void sendReport(CommandMessage context, Message message, Optional<RobloxUserGroupRankService.Data> groupInfo, DataRow dataRow, String username, String evidence, List<Message> messagesToRemove) {
        TextChannel tc = avaire.getShardManager().getTextChannelById(dataRow.getString("patrol_remittance_channel"));

        if (tc != null) {
            Button b1 = Button.success("accept:" + message.getId(), "Accept").withEmoji(Emoji.fromUnicode("✅"));
            Button b2 = Button.danger("reject:" + message.getId(), "Reject").withEmoji(Emoji.fromUnicode("❌"));
            Button b3 = Button.secondary("remove:" + message.getId(), "Delete").withEmoji(Emoji.fromUnicode("\uD83D\uDEAB"));


            tc.sendMessageEmbeds(context.makeEmbeddedMessage(new Color(32, 34, 37))
                .setAuthor("Remittance created for: " + username, null, getImageByName(context.guild, username))
                .setDescription(
                    "**Username**: " + username + "\n" +
                        (groupInfo.map(data -> "**Rank**: " + data.getRole().getName() + "\n").orElse("")) +
                        "**Evidence**: \n" + evidence)
                .requestedBy(context)
                .setTimestamp(Instant.now())
                .buildEmbed())
                .setActionRow(b1.asEnabled(), b2.asEnabled(), b3.asEnabled())
                .queue(
                    finalMessage -> {

                        message.editMessageEmbeds(context.makeSuccess("[Your remittance has been created in the correct channel.](:link).").set("link", finalMessage.getJumpUrl())
                            .buildEmbed())
                            .queue();
                        //createReactions(finalMessage);
                        try {
                            avaire.getDatabase().newQueryBuilder(Constants.REMITTANCE_DATABASE_TABLE_NAME).insert(data -> {
                                data.set("pb_server_id", finalMessage.getGuild().getId());
                                data.set("request_message_id", finalMessage.getId());
                                data.set("requester_discord_id", context.getAuthor().getId());
                                data.set("requester_discord_name", context.getMember().getEffectiveName(), true);
                                data.set("requester_roblox_id", getRobloxId(username));
                                data.set("requester_roblox_name", username);
                                data.set("requester_evidence", evidence, true);
                                data.set("requester_roblox_rank", groupInfo.map(value -> value.getRole().getName()).orElse(null));
                            });

                            cache.put(getRobloxId(username), context.getGuild());
                        } catch (SQLException throwables) {
                            AvaIre.getLogger().error("ERROR: ", throwables);
                        }

                    }
                );
        } else {
            context.makeError("Channel can't be found for the guild ``" + dataRow.getString("name") + "``. Please contact the bot developer, or guild HRs.");
        }
    }

    private String getImageByName(Guild guild, String username) {
        List<Member> members = guild.getMembersByEffectiveName(username, true);

        if (members.size() < 1) return null;
        if (members.size() > 1) return null;
        else return members.get(0).getUser().getEffectiveAvatarUrl();
    }

    private static boolean isValidMember(ButtonClickEvent r, CommandMessage context, Message l) {
        return context.getMember().equals(r.getMember()) && r.getMessageId().equalsIgnoreCase(l.getId());
    }

    private boolean checkAccountAge(CommandMessage context) {
        if (context.member != null) {
            return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - context.member.getUser().getTimeCreated().toInstant().toEpochMilli()) < 3;
        }
        return false;
    }

    private boolean checkEvidenceAcceptance(CommandMessage context, GuildMessageReceivedEvent pm) {
        String message = pm.getMessage().getContentRaw();
        if (!(message.startsWith("https://youtu.be") ||
            message.startsWith("http://youtu.be") ||
            message.startsWith("https://www.youtube.com/") ||
            message.startsWith("http://www.youtube.com/") ||
            message.startsWith("https://youtube.com/") ||
            message.startsWith("http://youtube.com/") ||
            message.startsWith("https://streamable.com/") ||
            message.contains("cdn.discordapp.com") ||
            message.contains("media.discordapp.com"))) {
            pm.getChannel().sendMessage(context.makeError("Sorry, but we are only accepting [YouTube links](https://www.youtube.com/upload) or [Streamable](https://streamable.com/) as evidence. Try again").buildEmbed()).queue();
            return false;
        }
        return true;
    }

    private boolean runClearAllChannelsFromDatabase(CommandMessage context) {
        QueryBuilder qb = avaire.getDatabase().newQueryBuilder(Constants.GUILD_TABLE_NAME).where("id", context.guild.getId());
        try {
            qb.update(q -> {
                q.set("patrol_remittance_channel", null);
                q.set("patrol_remittance_emote_id", null);
            });

            context.makeSuccess("Any information about the remittance channels has been removed from the database.").queue();
            return true;
        } catch (SQLException throwables) {
            context.makeError("Something went wrong in the database, please check with the developer. (Stefano#7366)").queue();
            return false;
        }

    }

    private boolean runSetRemittanceChannel(CommandMessage context, String[] args) {
        if (args.length < 3) {
            return sendErrorMessage(context, "Incorrect arguments");
        }
        Emote e;
        GuildChannel c = MentionableUtil.getChannel(context.message, args, 1);
        if (c == null) {
            return sendErrorMessage(context, "Something went wrong please try again or report this to a higher up! (Channel)");
        }

        if (NumberUtil.isNumeric(args[1])) {
            e = avaire.getShardManager().getEmoteById(args[1]);
            if (e == null) {
                return sendErrorMessage(context, "Something went wrong please try again or report this to a higher up! (Emote - ID)");
            }
        } else if (context.message.getEmotes().size() == 1) {
            e = context.message.getEmotes().get(0);
            if (e == null) {
                return sendErrorMessage(context, "Something went wrong please try again or report this to a higher up! (Emote - Mention)");
            }
        } else {
            return sendErrorMessage(context, "Something went wrong (To many emotes).");
        }

        if (NumberUtil.isNumeric(args[1])) {
            e = avaire.getShardManager().getEmoteById(args[1]);
            if (e == null) {
                return sendErrorMessage(context, "Something went wrong please try again or report this to a higher up! (Emote - ID)");
            }
        } else if (context.message.getEmotes().size() == 1) {
            e = context.message.getEmotes().get(0);
            if (e == null) {
                return sendErrorMessage(context, "Something went wrong please try again or report this to a higher up! (Emote - Mention)");
            }
        } else {
            return sendErrorMessage(context, "Something went wrong (To many emotes).");
        }

        GuildTransformer transformer = context.getGuildTransformer();
        if (transformer == null) {
            context.makeError("I can't pull the guilds information, please try again later.").queue();
            return false;
        }
        return updateChannelAndEmote(transformer, context, (TextChannel) c, e);
    }




    private boolean updateChannelAndEmote(GuildTransformer transformer, CommandMessage context, TextChannel channel, Emote emote) {
        transformer.setPatrolRemittanceChannel(channel.getId());
        transformer.setPatrolRemittanceEmoteId(emote.getId());

        QueryBuilder qb = avaire.getDatabase().newQueryBuilder(Constants.GUILD_TABLE_NAME).where("id", context.guild.getId());
        try {
            qb.update(q -> {
                q.set("patrol_remittance_channel", transformer.getPatrolRemittanceChannel());
                q.set("patrol_remittance_emote_id", transformer.getPatrolRemittanceEmoteId());
            });

            context.makeSuccess("Remittance have been enabled for :channel with the emote :emote").set("channel", channel.getAsMention()).set("emote", emote.getAsMention()).queue();
            return true;
        } catch (SQLException throwables) {
            context.makeError("Something went wrong in the database, please check with the developer. (Stefano#7366)").queue();
            return false;
        }

    }

    public static void createReactions(Message r) {
        r.addReaction("\uD83D\uDC4D").queue();   // 👍
        r.addReaction("\uD83D\uDC4E").queue();  // 👎
        r.addReaction("✅").queue();
        r.addReaction("❌").queue();
        r.addReaction("🚫").queue();
        r.addReaction("\uD83D\uDD04").queue(); // 🔄
    }

    private static Long getRobloxId(String un) {
        try {
            JSONObject json = readJsonFromUrl("https://api.roblox.com/users/get-by-username?username=" + un);
            return Double.valueOf(json.getDouble("Id")).longValue();
        } catch (Exception e) {
            return 0L;
        }
    }


}

package com.avairebot.commands.pinewood;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.Environment;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.awt.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.avairebot.pinewood.waiters.HandbookReportWaiters.*;
import static com.avairebot.utils.JsonReader.readJsonFromUrl;

public class RequestRewardCommand extends Command {

    public RequestRewardCommand(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "Request Reward Command";
    }

    @Override
    public String getDescription() {
        return "Request a reward for someone who did their job well in PBST.";
    }

    @Override
    public List <String> getUsageInstructions() {
        return Collections.singletonList(
            "`:command` - Start the reward system."
        );
    }

    @Override
    public List <String> getExampleUsage() {
        return Collections.singletonList(
            "`:command` - Start the reward system."
        );
    }


    @Override
    public List <String> getTriggers() {
        return Arrays.asList("rr", "request-reward");
    }

    @Nonnull
    @Override
    public List <CommandGroup> getGroups() {
        return Collections.singletonList(
            CommandGroups.REPORTS
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
        if (!context.guild.getId().equalsIgnoreCase("438134543837560832")) {
            context.makeError("Sorry, but this command can only be used in the PBST Discord.").queue();
        }
        if (!checkUserRanks(context)) {
            context.makeError("Sorry, but only Tier 3's and SD's are able to request a reward.").queue();
        }
        context.makeInfo("I will be sending you a DM to request more info!").setColor(new Color(0, 255, 0)).queue(
            p -> {
                context.getMember().getUser().openPrivateChannel().queue(x -> {
                    x.sendMessage("You're able to request a reward for Tier 2 or lower. Who would you like to request it for?").queue(v -> {
                        avaire.getWaiter().waitForEvent(PrivateMessageReceivedEvent.class, check -> context.getMember().getUser().equals(check.getAuthor()) && getRequestedUserRank(check), action -> {
                            if (action.getMessage().getContentRaw().equalsIgnoreCase("cancel")) { action.getChannel().sendMessage("Cancelled report!").queue();
                                return;
                            }
                            v.getChannel().sendMessage("The user did something good? How many points do you think he earned? :flushed:").queue(c -> {
                                startRequestDescriptionWaiter(action.getMessage().getContentRaw(), c, context);
                            });
                        }, 1, TimeUnit.MINUTES, () -> v.getChannel().sendMessage(getTimeOutMessage()).queue());
                    });
                });
            }
        );
        return false;
    }


    private String getTimeOutMessage() {
        return "You took to long to respond, please restart the report system!";
    }

    private void startRequestDescriptionWaiter(String username, Message c, CommandMessage context) {
        avaire.getWaiter().waitForEvent(PrivateMessageReceivedEvent.class, v -> context.member.getUser().equals(v.getAuthor()), action -> {
            if (action.getMessage().getContentRaw().equalsIgnoreCase("cancel")) { action.getChannel().sendMessage("Cancelled report!").queue();
                                return;
                            }
            action.getMessage().getChannel().sendMessage(context.makeInfo("Thanks! Do you have evidence with you? (You can add multiple links, just post them in the same message). Make sure to add the reason for the amount of points in this message as well\n\n"
                + "**We're accepting**:\n" +
                "- [YouTube Links](https://www.youtube.com/upload)\n" +
                "- [Imgur Links](https://imgur.com/upload)\n" +
                "- [Discord Image Links](https://cdn.discordapp.com/attachments/689520756891189371/733599719351123998/unknown.png)\n" +
                "- [Gyazo Links](https://gyazo.com)\n" +
                "- [LightShot Links](https://app.prntscr.com/)\n" +
                "- [Streamable](https://streamable.com)\n").buildEmbed()).queue(evi_mess -> startRequestEvidenceWaiter(username, evi_mess, context, action.getMessage().getContentRaw()));
        }, 1, TimeUnit.MINUTES, () -> c.getChannel().sendMessage(getTimeOutMessage()).queue());
    }

    private void startRequestEvidenceWaiter(String username, Message evi_mess, CommandMessage context, String reward) {
        avaire.getWaiter().waitForEvent(PrivateMessageReceivedEvent.class, b -> context.getMember().getUser().equals(b.getAuthor()) && checkEvidenceAcceptance(context, b), o -> {
            if (o.getMessage().getContentRaw().equalsIgnoreCase("cancel")) {
                return;
            }
            o.getMessage().getChannel().sendMessage("If seems like I have got everything! Is this your reward request?").queue();
            o.getMessage().getChannel().sendMessage(context.makeEmbeddedMessage().setTitle(context.getMember().getEffectiveName()).setDescription("**User**: " + username + "\n" +
                "**Rank**: " + getUserIdRank(getRobloxId(username)) + "\n" +
                "**Suggested Reward**: " + reward + "\n" +
                "**Evidence/Reasoning**: \n" + o.getMessage().getContentRaw()).buildEmbed()).queue(l -> {
                l.addReaction("✅").queue();
                l.addReaction("❌").queue();
                isReportDoneWaiter(username, l, context, reward, o);
            });
        }, 1, TimeUnit.MINUTES, () -> evi_mess.getChannel().sendMessage(getTimeOutMessage()).queue());
    }

    private void isReportDoneWaiter(String username, Message l, CommandMessage context, String reward, PrivateMessageReceivedEvent o) {
        avaire.getWaiter().waitForEvent(PrivateMessageReactionAddEvent.class, k -> k.getUser().equals(context.getMember().getUser()) && checkValidEmoji(k), emote -> {
            if (emote.getReactionEmote().getName().equalsIgnoreCase("✅")) {
                sendRewardRequest(context, username, reward, o.getMessage().getContentRaw(), getUserIdRank(getRobloxId(username)));
            }
            if (emote.getReactionEmote().getName().equalsIgnoreCase("❌")) {
                return;
            }
        });
    }

    private void sendRewardRequest(CommandMessage context, String username, String reward, String contentRaw, String userIdRank) {
        TextChannel tc = context.getJDA().getGuildById("438134543837560832").getTextChannelById(Constants.REWARD_REQUESTS_CHANNEL_ID);
        if (tc != null) {
            tc.sendMessage(context.makeEmbeddedMessage()
                .setFooter(context.getMember().getEffectiveName(), context.member.getUser().getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .setTitle(username)
                .setDescription("**User**: " + username + "\n" +
                    "**Rank**: " + userIdRank + "\n" +
                    "**Suggested Reward**: " + reward + "\n" +
                    "**Evidence/Reasoning**: \n" + contentRaw).buildEmbed())
                .queue(msg -> {
                    context.getMember().getUser().openPrivateChannel().queue(l -> l.sendMessage(context.makeSuccess("[Your report has been sent in the correct report channel!](" + msg.getJumpUrl() + ")").buildEmbed()).queue());
                    msg.addReaction("\uD83D\uDC4D").queue();
                    msg.addReaction("\uD83D\uDC4E").queue();
                    msg.addReaction("✅").queue();
                    msg.addReaction("❌").queue();
                    msg.addReaction("trash:694314074179240027").queue();
                    msg.addReaction("\uD83D\uDD04").queue();
                });
        }
    }

    private boolean checkValidEmoji(PrivateMessageReactionAddEvent k) {
        if (k.getReactionEmote().getName().equalsIgnoreCase("✅") || k.getReactionEmote().getName().equalsIgnoreCase("❌"))
            return true;
        else
            k.getReaction().removeReaction(k.getUser()).queue();
        return false;
    }

    private boolean checkUserRanks(CommandMessage context) {
        return context.getMember().getRoles().contains(context.guild.getRoleById("438136063077384202")) || context.getMember().getRoles().contains(context.guild.getRoleById("438136063001886741"));
    }


    public int getRobloxId(String un) {

        try {
            JSONObject json = readJsonFromUrl("http://api.roblox.com/users/get-by-username?username=" + un);
            return json.getInt("Id");
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isValidRobloxUser(String un) {

        try {
            JSONObject json = readJsonFromUrl("http://api.roblox.com/users/get-by-username?username=" + un);
            String username = json.getString("Username");

            return username != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean getUserRankFromId(int id) {
        try {
            JSONObject json = readJsonFromUrl("https://groups.roblox.com/v1/users/" + id + "/groups/roles");
            JSONArray array = json.getJSONArray("data");

            for (int i = 0; i < array.length(); i++) {
                if (array.getJSONObject(i).getJSONObject("group").getInt("id") == 645836) {
                    return array.getJSONObject(i).getJSONObject("role").getInt("rank") < 30;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public String getUserIdRank(int id) {
        try {
            JSONObject json = readJsonFromUrl("https://groups.roblox.com/v1/users/" + id + "/groups/roles");
            JSONArray array = json.getJSONArray("data");

            for (int i = 0; i < array.length(); i++) {
                if (array.getJSONObject(i).getJSONObject("group").getInt("id") == 645836) {
                    return array.getJSONObject(i).getJSONObject("role").getString("name");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ":x: Rank not found :x:";
        }
        return ":x: Rank not found :x:";
    }


    private boolean getRequestedUserRank(PrivateMessageReceivedEvent context) {
        if (context.getMessage().getContentRaw().equalsIgnoreCase("cancel")) {
            return true;
        }
        if (!isValidRobloxUser(context.getMessage().getContentRaw())) {
            context.getMessage().getChannel().sendMessage("Please retype the username! This user does not exist").queue();
            return false;
        }

        if (!getUserRankFromId(getRobloxId(context.getMessage().getContentRaw()))) {
            context.getMessage().getChannel().sendMessage("Sorry, but you're not able to suggest a Tier 3 or higher for a reward!").queue();
            return false;
        }
        return true;
    }

    private static boolean checkEvidenceAcceptance(CommandMessage context, PrivateMessageReceivedEvent pm) {
        String message = pm.getMessage().getContentRaw();
        if (message.equalsIgnoreCase("cancel")) {
            return true;
        }
        if (!(message.contains("https://youtu.be") ||
            message.contains("http://youtu.be") ||
            message.contains("https://www.youtube.com/") ||
            message.contains("http://www.youtube.com/") ||
            message.contains("https://youtube.com/") ||
            message.contains("http://youtube.com/") ||
            message.contains("https://streamable.com/") ||
            message.contains("cdn.discordapp.com") ||
            message.contains("media.discordapp.com") ||
            message.contains("gyazo.com") ||
            message.contains("prntscr.com") || message.contains("imgur.com"))) {
            pm.getChannel().sendMessage(context.makeError("Sorry, but we are only accepting [YouTube links](https://www.youtube.com/upload), [Gyazo Links](https://gyazo.com), [LightShot Links](https://app.prntscr.com/), [Discord Image Links](https://cdn.discordapp.com/attachments/689520756891189371/733599719351123998/unknown.png) or [Imgur links](https://imgur.com/upload) as evidence. Try again").buildEmbed()).queue();
            return false;
        }
        return true;
    }


    private boolean checkAccountAge(CommandMessage context) {
        if (context.member != null) {
            return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - context.member.getUser().getTimeCreated().toInstant().toEpochMilli()) < 3;
        }
        return false;
    }

    private void tookToLong(CommandMessage event) {
        event.makeError("<a:alerta:729735220319748117> You've taken to long to react to the message <a:alerta:729735220319748117>").queue();
    }
}

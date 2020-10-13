package com.avairebot.pinewood.waiters;

import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.commands.pinewood.ReportUserCommand;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.avairebot.utils.JsonReader.readArrayJsonFromUrl;
import static com.avairebot.utils.JsonReader.readJsonFromUrl;

public class HandbookReportWaiters {
    private static AvaIre avaire;

    public HandbookReportWaiters (AvaIre avaire) {
        HandbookReportWaiters.avaire = avaire;
    }
    public static void getPBSTHandbookReportWaiter(CommandMessage context) {
        context.makeInfo("I will send you a message in your private messages, if you did not get a message, make sure you can get messages from non-friends in this server.").queue();
        context.getMember().getUser().openPrivateChannel().queue(pc -> pc.sendMessage(context.makeInfo("Welcome to the Pinewood Builders Security Team Report System!\n" +
            "This system is used for reporting any PBST member not following the [handbook](https://pbst.pinewood-builders.com/).\n" +
            "\n" +
            "Make sure you also have evidence supporting you claim for said person.\n" +
            "This has to be done with a screenshot, or a YouTube video\n" +
            "\n" +
            "**If you're reporting someone, they have to be ``on-duty``. \n" +
            "This ONLY applies when they have a __PBST Ranktag with uniform or PBST weapons in their hands__** \n" +
            "\n" +
            "Abusing this system will result in a blacklist from the bot\n" +
            "\n" +
            "Lets start with the Roblox name! Who do you want to report?").buildEmbed()).queue(v -> beginUsernameWaiter(context, "pbst")), t -> context.makeError("It seems like I cannot send you any messages :(").queue());
    }

    public static void getPETHandbookReportWaiter(CommandMessage context) {
        context.makeInfo("I will send you a message in your private messages, if you did not get a message, make sure you can get messages from non-friends in this server.").queue();
        context.getMember().getUser().openPrivateChannel().queue(pc -> pc.sendMessage(context.makeInfo("Welcome to the Pinewood Emergency Team Report System!\n" +
            "This system is used for reporting any PET  member not following the [handbook](https://pet.pinewood-builders.com/).\n" +
            "\n" +
            "Make sure you also have evidence supporting you claim for said person.\n" +
            "This has to be done with a screenshot, or a YouTube video\n" +
            "\n" +
            "**If you're reporting someone, they have to be ``on-duty``. \n" +
            "This ONLY applies when they have a __PET Ranktag with uniform or PBST weapons in their hands__.\n\n" +
            "This also applies when someone is using PET Tools in a different group (PET Tools in TMS or PBST)**\n" +
            "\n" +
            "Abusing this system will result in a blacklist from the bot\n" +
            "\n" +
            "Lets start with the Roblox name! Who do you want to report?").buildEmbed()).queue(v -> beginUsernameWaiter(context, "pet")), t -> context.makeError("It seems like I cannot send you any messages :(").queue());
    }

    public static void getDevelopmentHandbookReportWaiter(CommandMessage context) {
        context.makeInfo("I will send you a message in your private messages, if you did not get a message, make sure you can get messages from non-friends in this server.").queue();
        context.getMember().getUser().openPrivateChannel().queue(pc -> pc.sendMessage(context.makeInfo(
            "Lets start with the Roblox name! Who do you want to report?").buildEmbed()).queue(v -> beginUsernameWaiter(context, "development")), t -> context.makeError("It seems like I cannot send you any messages :(").queue());
    }

    public static void getTMSHandbookReportWaiter(CommandMessage context) {
        context.makeInfo("I will send you a message in your private messages, if you did not get a message, make sure you can get messages from non-friends in this server.").queue();
        context.getMember().getUser().openPrivateChannel().queue(pc -> pc.sendMessage(context.makeInfo("Welcome to The Mayhem Syndicate Report System!\n" +
            "This system is used for reporting any TMS member not following the [handbook](https://devforum.roblox.com/t/the-mayhem-syndicate-handbook/595758).\n" +
            "\n" +
            "Make sure you also have evidence supporting you claim for said person.\n" +
            "This has to be done with a screenshot, or a video (YouTube, MP4, etc)\n" +
            "\n" +
            "**If you're reporting someone, they have to be ``on-duty``. \n" +
            "This ONLY applies when they have a __TMS Ranktag with uniform or TMS weapons in their hands__** \n" +
            "\n" +
            "Abusing this system will result in a blacklist from the bot\n" +
            "\n" +
            "Lets start with the Roblox name! Who do you want to report?").buildEmbed()).queue(v -> beginUsernameWaiter(context, "tms")), t -> context.makeError("It seems like I cannot send you any messages :(").queue());
    }

    public static void getPinewoodExploiterHandbookReportWaiter(CommandMessage context) {
        context.makeInfo("I will send you a message in your private messages, if you did not get a message, make sure you can get messages from non-friends in this server.").queue();
        context.getMember().getUser().openPrivateChannel().queue(pc -> pc.sendMessage(context.makeInfo("***When reporting someone, make sure you have evidence about them exploiting or abusing, this can be with a video, but also with a picture***\n" +
            "\n" +
            "Use this system to report exploiters, spammers, mass random killers, weapon abusers, and spawnkillers (which are rare due to there being spawn protection)\n" +
            "*Make sure that you use the `!call` command ingame to send a report to the PIA before you send a report here*" +
            "\n\n" +
            "Now, who are you trying to report?").buildEmbed()).queue(v -> beginUsernameWaiter(context, "exploit-abuse")), t -> context.makeError("It seems like I cannot send you any messages :(").queue());
    }

    private static void beginUsernameWaiter(CommandMessage context, String group) {
        avaire.getWaiter().waitForEvent(PrivateMessageReceivedEvent.class, a -> isValidDiscordUserPM(context, a) && isValidUser(context, a, group),
            p -> startDescriptionWaiter(context, p, group), 90, TimeUnit.SECONDS, () -> context.makeError("You took to long to respond, please restart the report system!").queue());
    }

    private static void startDescriptionWaiter(CommandMessage context, PrivateMessageReceivedEvent p, String group) {
        p.getChannel().sendMessage(context.makeSuccess("Yes! I've found the user you'd like to report, could you tell me what he did?").buildEmbed()).queue(
            v -> avaire.getWaiter().waitForEvent(PrivateMessageReceivedEvent.class, a -> isValidDiscordUserPM(context, a) && checkDescriptionLength(context, a), r -> startEvidenceWaiter(context, p, r, group), 90, TimeUnit.SECONDS, () -> p.getChannel().sendMessage("You took to long to respond, please restart the report system!").queue())
        );
    }

    private static void startEvidenceWaiter(CommandMessage context, PrivateMessageReceivedEvent username, PrivateMessageReceivedEvent description, String group) {
        username.getChannel().sendMessage(context.makeSuccess("I've collected the violation you entered, but I need to be sure he actually did something bad.\n" +
            "Please enter a **LINK** to evidence.\n\n" +
            "**We're accepting**:\n" +
            "- [YouTube Links](https://www.youtube.com/upload)\n" +
            "- [Imgur Links](https://imgur.com/upload)\n" +
            "- [Discord Image Links](https://cdn.discordapp.com/attachments/689520756891189371/733599719351123998/unknown.png)\n" +
            "- [Gyazo Links](https://gyazo.com)\n" +
            "- [LightShot Links](https://app.prntscr.com/)\n" +
            "- [Streamable](https://streamable.com)\n" +
            "If you want a link/video/image service added, please ask ``Stefano#7366``").buildEmbed()).queue(evi -> avaire.getWaiter().waitForEvent(PrivateMessageReceivedEvent.class, pm -> isValidDiscordUserPM(context, pm) && checkEvidenceAcceptance(context, pm), evidence -> startConfirmationWaiter(context, username, description, evidence, group), 90, TimeUnit.SECONDS, () -> description.getChannel().sendMessage("You took to long to respond, please restart the report system!").queue()));
    }

    private static void startConfirmationWaiter(CommandMessage context, PrivateMessageReceivedEvent u, PrivateMessageReceivedEvent d, PrivateMessageReceivedEvent e, String g) {
        String username = u.getMessage().getContentRaw();
        String description = d.getMessage().getContentRaw();
        String evidence = e.getMessage().getContentRaw();
        String group = getGroupString(g);

        e.getChannel().sendMessage(context.makeInfo("Ok, so. I've collected everything you've told me. And this is the data I got:\n\n" +
            "**Username**: " + username + "\n" +
            "**Group**: " + group + "\n" +
            "**Description**: \n" + description + "\n\n" +
            "**Evidence**: \n" + evidence + "\n\nIs this correct?").buildEmbed()).queue(l -> {
            l.addReaction("✅").queue();
            l.addReaction("❌").queue();
            avaire.getWaiter().waitForEvent(PrivateMessageReactionAddEvent.class, r -> isValidMember(r, context, l), send -> {
                if (send.getReactionEmote().getName().equals("❌")) {
                    e.getChannel().sendMessage("Report has been canceled, if you want to restart the report. Do ``c!ru`` in any bot-commands channel.").queue();
                }
                if (send.getReactionEmote().getName().equals("✅")) {
                    ReportUserCommand.sendReport(context, username, description, evidence, g, l);
                } else {
                    e.getChannel().sendMessage("Invalid emoji given, report deleted!").queue();
                }
            }, 90, TimeUnit.SECONDS, () -> e.getChannel().sendMessage("You took to long to respond, please restart the report system!").queue());
        });

    }

    private static boolean isValidMember(PrivateMessageReactionAddEvent r, CommandMessage context, Message l) {
        System.out.println(context.getMember().getUser());
        System.out.println(r.getUser());
        System.out.println("----");
        System.out.println(l.getId());
        System.out.println(r.getReaction().getMessageId());
        return context.getMember().getUser().equals(r.getUser()) && r.getReaction().getMessageId().equals(l.getId());
    }

    private static String getGroupString(String g) {
        switch (g) {
            case "pbst":
                return "Pinewood Builders Security Team <:PBSTHandbook:690133745805819917>";
            case "tms":
                return "The Mayhem Syndicate <:TMSHandbook:690134424041422859>";
            case "pet":
                return "Pinewood Emergency Team <:PETHandbook:690134297465585704>";
            case "exploit-abuse":
                return "Pinewood Builders";
        }
        return ":gear: Development Guild :gear:";
    }

    private static boolean checkEvidenceAcceptance(CommandMessage context, PrivateMessageReceivedEvent pm) {
        String message = pm.getMessage().getContentRaw();
        if (!(message.startsWith("https://youtu.be") ||
                message.startsWith("http://youtu.be") ||
                message.startsWith("https://www.youtube.com/") ||
                message.startsWith("http://www.youtube.com/") ||
                message.startsWith("https://youtube.com/") ||
                message.startsWith("http://youtube.com/") ||
                message.startsWith("https://streamable.com/")||
                message.contains("cdn.discordapp.com") ||
                message.contains("media.discordapp.com") ||
                message.contains("gyazo.com") ||
                message.contains("prntscr.com") || message.contains("imgur.com"))) {
            pm.getChannel().sendMessage(context.makeError("Sorry, but we are only accepting [YouTube links](https://www.youtube.com/upload), [Gyazo Links](https://gyazo.com), [LightShot Links](https://app.prntscr.com/), [Discord Image Links](https://cdn.discordapp.com/attachments/689520756891189371/733599719351123998/unknown.png) or [Imgur links](https://imgur.com/upload) as evidence. Try again").buildEmbed()).queue();
            return false;
        }
        return true;
    }


    private static boolean checkDescriptionLength(CommandMessage context, PrivateMessageReceivedEvent a) {
        int length = a.getMessage().getContentRaw().length();
        if (length < 50 || length > 700) {
            a.getChannel().sendMessage(context.makeError("Sorry, but reports have to have a minimal of 50 characters and a maximum of 700 characters.\n" +
                "Your report currently has **``" + length + "``** characters").buildEmbed()).queue();
            return false;
        }
        return true;
    }

    private static boolean isValidUser(CommandMessage context, PrivateMessageReceivedEvent p, String group) {
        if (p.getAuthor().isBot()) {
            return false;
        }
        if (!isValidRobloxUser(p.getMessage().getContentRaw())) {
            p.getChannel().sendMessage(context.makeError("This is not a valid roblox user, please check and re-enter the name.").buildEmbed()).queue();
            return false;
        }
        return checkRobloxGroup(context, p, group);
    }
    private static boolean isValidDiscordUserPM(CommandMessage context, PrivateMessageReceivedEvent p) {
        return context.getMember().getUser().equals(p.getAuthor());
    }
    private static boolean checkRobloxGroup(CommandMessage context, PrivateMessageReceivedEvent p, String group) {
        if (group.equalsIgnoreCase("pbst")) {
            if (isValidUserInRobloxGroup(getRobloxId(p.getMessage().getContentRaw()), 645836)) {
                return true;
            }
            p.getChannel().sendMessage(context.makeError("This user is not in PBST, please check and re-enter the name.").buildEmbed()).queue();
        }
        if (group.equalsIgnoreCase("pet")) {
            if (isValidUserInRobloxGroup(getRobloxId(p.getMessage().getContentRaw()), 2593707)) {
                return true;
            }
            p.getChannel().sendMessage(context.makeError("This user is not in PET, please check and re-enter the name.").buildEmbed()).queue();
        }
        if (group.equalsIgnoreCase("tms")) {
            if (isValidUserInRobloxGroup(getRobloxId(p.getMessage().getContentRaw()), 4890641)) {
                return true;
            }
            p.getChannel().sendMessage(context.makeError("This user is not in TMS, please check and re-enter the name.").buildEmbed()).queue();
        }
        if (group.equalsIgnoreCase("exploit-abuse")) {
            return true;
        }
        return group.equalsIgnoreCase("development");
    }

    private static boolean isValidUserInRobloxGroup(int id, int groupId) {
        try {
            JSONArray json = readArrayJsonFromUrl("https://api.roblox.com/users/" + id + "/groups");
            for (int i = 0; i < json.length(); i++) {
                JSONObject object = json.getJSONObject(i);
                if (object.getInt("Id") != groupId) continue;
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isValidRobloxUser(String un) {

        try {
            JSONObject json = readJsonFromUrl("http://api.roblox.com/users/get-by-username?username=" + un);
            String username = json.getString("Username");

            return username != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static int getRobloxId(String un) {
        try {
            JSONObject json = readJsonFromUrl("http://api.roblox.com/users/get-by-username?username=" + un);
            return json.getInt("Id");
        } catch (Exception e) {
            return 0;
        }
    }

}

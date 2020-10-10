package com.avairebot.commands.pinewood;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.Environment;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

import javax.annotation.Nonnull;
import java.awt.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.avairebot.pinewood.waiters.HandbookReportWaiters.*;


public class ReportUserCommand extends Command {


    public ReportUserCommand(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "Report User Command";
    }

    @Override
    public String getDescription() {
        return "Report a user who is breaking PBST Handbook rules.";
    }

    @Override
    public List <String> getUsageInstructions() {
        return Collections.singletonList(
            "`:command` - Start the report system."
        );
    }

    @Override
    public List <String> getExampleUsage() {
        return Collections.singletonList(
            "`:command` - Start the report system."
        );
    }


    @Override
    public List <String> getTriggers() {
        return Arrays.asList("report-user", "ru");
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
        if (checkAccountAge(context)) {
            context.makeError("Sorry, but only discord accounts that are older then 3 days are allowed to make actual reports.\nIf this is an important violation, please contact a trainer.").queue();
        }
        context.makeInfo("For what division would you like to create a report?\n**Please remember when using this feature, that the people who get reported may not know about the handbook in the first place, make sure you've told them in the first place!**\n\nIf you want to report an exploiter, react with <:PB:757736074641408022>").setColor(new Color(0, 255, 0)).queue(
            p -> {
                p.addReaction("PBSTHandbook:690133745805819917").queue();
                p.addReaction("TMSHandbook:690134424041422859").queue();
                p.addReaction("PETHandbook:690134297465585704").queue();
                p.addReaction("PB:757736074641408022").queue();

                if (Environment.fromName(avaire.getConfig().getString("environment", Environment.PRODUCTION.getName())).equals(Environment.DEVELOPMENT)) {
                    p.addReaction(":gear:").queue();
                }
                waitEmoji(context);
            }
        );

        
        return false;
    }

    public static void sendReport(CommandMessage context, String username, String description, String evidence, String group, Message l) {
        TextChannel tc = getReportChannel(context, group);
        if (tc != null) {
            tc.sendMessage(context.makeEmbeddedMessage(returnColor(tc.getGuild().getIdLong()))
                .setFooter(context.getMember().getEffectiveName(), context.member.getUser().getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .setTitle(username)
                .setDescription("**Information**:\n" +
                    description + "\n\n" +
                    "**Evidence**: \n" +
                    evidence).buildEmbed())
                .queue(msg -> {
                    l.getChannel().sendMessage(context.makeSuccess("[Your report has been sent in the correct report channel!](" + msg.getJumpUrl() + ")").buildEmbed()).queue();
                    msg.addReaction("\uD83D\uDC4D").queue();
                    msg.addReaction("\uD83D\uDC4E").queue();
                    msg.addReaction("✅").queue();
                    msg.addReaction("❌").queue();
                    msg.addReaction("trash:694314074179240027").queue();
                    msg.addReaction("\uD83D\uDD04").queue();
                });
        }
    }

    private static TextChannel getReportChannel(CommandMessage context, String group) {
        if (group.equals("pbst")) {
            return context.getJDA().getGuildById("438134543837560832").getTextChannelById(Constants.PBST_REPORT_CHANNEL);
        }
        if (group.equals("tms")) {
            return context.getJDA().getGuildById("572104809973415943").getTextChannelById(Constants.TMS_REPORT_CHANNEL);
        }
        if (group.equals("pet")) {
            return context.getJDA().getGuildById("436670173777362944").getTextChannelById(Constants.PET_REPORT_CHANNEL);
        }
        if (group.equals("exploit-abuse")) {
            return context.getJDA().getGuildById("371062894315569173").getTextChannelById(Constants.PB_REPORT_CHANNEL);
        } else {
            return context.getGuild().getTextChannelsByName("handbook-violator-reports", true).get(0);
        }
    }

    private boolean checkAccountAge(CommandMessage context) {
        if (context.member != null) {
            return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - context.member.getUser().getTimeCreated().toInstant().toEpochMilli()) < 3;
        }
        return false;
    }

    private void waitEmoji(CommandMessage event) {
        avaire.getWaiter().waitForEvent(GuildMessageReactionAddEvent.class,
            e -> checkValidEmojiMessage(e, event) && e.getMember().equals(event.getMember()), v -> {
                if (v.getReactionEmote().getName().equals("PBSTHandbook")) {
                    getPBSTHandbookReportWaiter(event);
                }
                if (v.getReactionEmote().getName().equals("TMSHandbook")) {
                    getTMSHandbookReportWaiter(event);
                }
                if (v.getReactionEmote().getName().equals("PETHandbook")) {
                    getPETHandbookReportWaiter(event);
                }
                if (v.getReactionEmote().getName().equals("PB")) {
                    getPinewoodExploiterHandbookReportWaiter(event);
                }
                if (v.getReactionEmote().getName().equals("gear")) {
                    getDevelopmentHandbookReportWaiter(event);
                }
                v.getChannel().retrieveMessageById(v.getMessageId()).queue(l -> {l.delete().queue();});
            }, 30, TimeUnit.SECONDS, () -> tookToLong(event));
    }




    private boolean checkValidEmojiMessage(GuildMessageReactionAddEvent e, CommandMessage event) {
        if (e.getUser().isBot()) {
            return false;
        }
        if (!e.getMember().equals(event.getMember())) {
            return false;
        }
        if (!(e.getReactionEmote().getName().equals("PBSTHandbook") ||
            e.getReactionEmote().getName().equals("TMSHandbook") ||
            e.getReactionEmote().getName().equals("PB") ||
            e.getReactionEmote().getName().equals("PETHandbook") ||
            e.getReactionEmote().getName().equals("gear"))) {
            event.makeError("Invalid emoji given, please react with a correct emoji!").queue();
            e.getReaction().removeReaction(e.getUser()).queue();
            return false;
        }
        return true;
    }


    private static Color returnColor(long server_id) {
        if (server_id == 438134543837560832L) { //PBST
            return new Color(33, 78, 179);
        }
        if (server_id == 572104809973415943L) { //TMS
            return new Color(120, 120, 120);
        }
        if (server_id == 436670173777362944L) { //PET
            return new Color(170, 0, 0);
        }
        if (server_id == 371062894315569173L) { //PB
            return new Color(74, 124, 227);
        }
        return new Color(255, 0, 255);
    }


    private void tookToLong(CommandMessage event) {
        event.makeError("<a:alerta:729735220319748117> You've taken to long to react to the message <a:alerta:729735220319748117>").queue();
    }
}

package com.avairebot.commands.pinewood;

import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import com.avairebot.pinewood.waiters.FeedbackWaiters;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PBSTFeedbackCommand extends Command {

    public PBSTFeedbackCommand(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "PBST Feedback Command";
    }

    @Override
    public String getDescription() {
        return "Feedback about something in PBST.";
    }

    @Override
    public List <String> getUsageInstructions() {
        return Collections.singletonList(
            "`:command` - Give feedback about PBST."
        );
    }

    @Override
    public List <String> getExampleUsage() {
        return Collections.singletonList(
            "`:command raids seem to be short staffed bla bla bla` - "
        );
    }


    @Override
    public List <String> getTriggers() {
        return Arrays.asList("suggest", "feedback");
    }

    @Nonnull
    @Override
    public List <CommandGroup> getGroups() {
        return Collections.singletonList(
            CommandGroups.COMMAND_CUSTOMIZATION
        );
    }

    @Override
    public List <String> getMiddleware() {
        return Arrays.asList(
            "isOfficialPinewoodGuild",
            "throttle:user,1,120"
        );
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
            context.makeInfo("Hello! And welcome to the feedback/suggestion selection menu :smile:\nYou can select from 5 places to suggest on: \n" +
                "- <:xeus:737770956671418438> -> Suggestions for Xeus.\n" +
                "- <:PBSTHandbook:690133745805819917> -> Suggestions for PBST.\n" +
                "- <:PETHandbook:690134297465585704> -> Suggestions for PET.\n" +
                "- <:PB:757736074641408022> -> Anything involving PB.\n" +
                "- \uD83D\uDEE2 -> PBOP (Pinewood Builders Oil Platform)").queue( p -> {
                    p.addReaction("xeus:737770956671418438").queue();
                    p.addReaction("PBSTHandbook:690133745805819917").queue();
                    p.addReaction("PETHandbook:690134297465585704").queue();
                    p.addReaction("PB:757736074641408022").queue();
                    p.addReaction("\uD83D\uDEE2").queue();

                    FeedbackWaiters.getInstance().startFeedbackVersionListener(context, p);
            });

        return true;
    }





}

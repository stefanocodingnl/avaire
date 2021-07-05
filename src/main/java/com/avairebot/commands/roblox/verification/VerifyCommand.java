package com.avairebot.commands.roblox.verification;

import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VerifyCommand extends Command {
    public VerifyCommand(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "Verify Command";
    }

    @Override
    public String getDescription() {
        return "Verify yourself on the discord.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Arrays.asList(
            "`:command` - Verify yourself and update the cache."
        );
    }

    @Override
    public List<String> getExampleUsage() {
        return Arrays.asList(
            "`:command`"
        );
    }

    @Override
    public List <String> getMiddleware() {
        return Arrays.asList(
                "isOfficialPinewoodGuild"
        );
    }

    @Override
    public List<String> getTriggers() {
        return Collections.singletonList("verify");
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (!avaire.getBotAdmins().getUserById(context.getAuthor().getIdLong(), true).isAdmin()) {
            return false;
        }


        return avaire.getRobloxAPIManager().getVerification().verify(context, true);

        /*context.makeInfo("<a:loading:742658561414266890> Checking verification database <a:loading:742658561414266890>").queue(m -> {
            VerificationEntity verifiedRobloxUser = avaire.getRobloxAPIManager().getVerification().fetchVerification(context.getMember().getId(), true);
            if (verifiedRobloxUser == null) {
                m.editMessage(context.makeError("No account found on the RoVer API. Please verify yourself on: https://verify.eryn.io/").requestedBy(context).buildEmbed()).queue();
                return;
            }

            if (context.member == null) {
                m.editMessage(context.makeError("Member object not found (Discord)").buildEmbed()).queue();
                return;
            }

            context.getGuild().modifyNickname(context.member, context.getVerificationTransformer().getNicknameFormat()
                    .replace("%USERNAME%", verifiedRobloxUser.getRobloxUsername())).queue();

            m.editMessage(context.makeSuccess(context.getVerificationTransformer().getWelcomeMessage()
                .replace("%SERVER%", context.getGuild().getName())
                .replace("%USERNAME%", verifiedRobloxUser.getRobloxUsername())).buildEmbed()).queue();


        });
        return false;*/
    }
}

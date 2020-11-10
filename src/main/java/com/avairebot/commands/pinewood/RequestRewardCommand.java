package com.avairebot.commands.pinewood;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import com.avairebot.factories.RequestFactory;
import com.avairebot.requests.Request;
import com.avairebot.requests.Response;
import com.avairebot.requests.service.user.rank.RobloxUserGroupRankService;
import com.avairebot.utilities.RestActionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.avairebot.utils.JsonReader.readJsonFromUrl;

public class RequestRewardCommand extends Command {

    public RequestRewardCommand(AvaIre avaire) {
        super(avaire, false);
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
    public List<String> getUsageInstructions() {
        return Collections.singletonList(
            "`:command` - Start the reward system."
        );
    }

    @Override
    public List<String> getExampleUsage() {
        return Collections.singletonList(
            "`:command` - Start the reward system."
        );
    }


    @Override
    public List<String> getTriggers() {
        return Arrays.asList("rr", "request-reward");
    }

    @Nonnull
    @Override
    public List<CommandGroup> getGroups() {
        return Collections.singletonList(
            CommandGroups.REPORTS
        );
    }

    @Override
    public List<String> getMiddleware() {
        return Arrays.asList(
            "isOfficialPinewoodGuild",
            "throttle:guild,1,30"
        );
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (!context.getAuthor().getId().equals("173839105615069184")) {
            context.makeError("[This command is temporarly disabled due changed in the recent way rewards are done.](https://discordapp.com/channels/438134543837560832/459764670782504961/775451307150147605)").queue();
            return true;
        }


        int requesterId = getRobloxId(context.getMember().getEffectiveName());
        if (requesterId == 0) {
            context.makeError("Sorry, but your username seems to not exist inside roblox. Please make sure your nick is correct.").queue();
            return false;
        }

        if (args.length < 1) {
            context.makeError("Please add who you'd like to request a reward for in the command.\n\n**Example**: \n - ``" + generateCommandPrefix(context.getMessage()) + "rr <Roblox Username>``").queue();
            return false;
        }

        int requestedId = getRobloxId(args[0]);
        if (requestedId == 0) {
            context.makeError("Sorry, but your username you gave us, does not exist in.").queue();
            return false;
        }




        Request requesterRequest = RequestFactory.makeGET("https://groups.roblox.com/v1/users/" + requesterId + "/groups/roles");
        Request requestedRequest = RequestFactory.makeGET("https://groups.roblox.com/v1/users/" + requesterId + "/groups/roles");

        requesterRequest.send((Consumer<Response>) response -> {
            int statusCode = response.getResponse().code();

            if (statusCode == 429) {
                context.makeWarning("To many attempts")
                    .queue(message -> message.delete().queueAfter(45, TimeUnit.SECONDS, null, RestActionUtil.ignore));
                return;
            }

            if (statusCode == 200) {
                RobloxUserGroupRankService service = (RobloxUserGroupRankService) response.toService(RobloxUserGroupRankService.class);
                continueNextCheckForPossibleRanksToVote(context, 1, service);
                return;
            }

            context.makeError("Something went wrong...").queue(message -> message.delete().queueAfter(45, TimeUnit.SECONDS, null, RestActionUtil.ignore));
        });


        return false;
    }

    private void continueNextCheckForPossibleRanksToVote(CommandMessage context, int userId, RobloxUserGroupRankService requester) {
        avaire.getWaiter().waitForEvent(GuildMessageReceivedEvent.class, o -> o.getMessage().getChannel().equals(context.getChannel()) && o.getMember().equals(context.getMember()) && isValidRobloxian(context, getRobloxId(o.getMessage().getContentRaw())),
            p -> {
                Request request = RequestFactory.makeGET("https://groups.roblox.com/v1/users/" + getRobloxId(p.getMessage().getContentRaw()) + "/groups/roles");

                request.send((Consumer<Response>) response -> {
                    int statusCode = response.getResponse().code();

                    if (statusCode == 429) {
                        context.makeWarning("To many attempts")
                            .queue(message -> message.delete().queueAfter(45, TimeUnit.SECONDS, null, RestActionUtil.ignore));
                        return;
                    }

                    if (statusCode == 200) {
                        RobloxUserGroupRankService service = (RobloxUserGroupRankService) response.toService(RobloxUserGroupRankService.class);
                        sendSpecialRanksAldRolesMessage(context, userId, requester, service);
                        return;
                    }

                    context.makeError("Something went wrong...").queue(message -> message.delete().queueAfter(45, TimeUnit.SECONDS, null, RestActionUtil.ignore));
                });
            });
    }

    private void sendSpecialRanksAldRolesMessage(CommandMessage context, int userId, RobloxUserGroupRankService requester, RobloxUserGroupRankService requested) {
        RobloxUserGroupRankService.Data requestedUser = requested.getData().stream().filter(l -> l.getGroup().getId() == 645836).findFirst().get();
        RobloxUserGroupRankService.Data requesterUser = requester.getData().stream().filter(l -> l.getGroup().getId() == 645836).findFirst().get();


        context.makeWarning("The user you requested has the rank: " + requestedUser.getRole().getName() + "\n" +
            "You have the rank: " + requesterUser.getRole().getName() + "\n\n" +
            "You can " + (requestedUser.getRole().getId() < requesterUser.getRole().getId() ? "NOT" : "ACTUALLY")).queue();
    }

    private boolean isValidRobloxian(CommandMessage context, int userId) {
        if (userId == 0) {
            context.makeError("Invalid username.").queue();
            return false;
        } else {
            return true;
        }
    }

    public int getRobloxId(String un) {

        try {
            JSONObject json = readJsonFromUrl("http://api.roblox.com/users/get-by-username?username=" + un);
            return json.getInt("Id");
        } catch (Exception e) {
            return 0;
        }
    }
}

package com.avairebot.commands.roblox;

import com.avairebot.AppInfo;
import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.avairebot.utils.JsonReader.readJsonFromUrl;

public class GroupRankCommand extends Command {
    public GroupRankCommand(AvaIre avaire) {
        super(avaire);
    }

    private static final MediaType json = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public String getName() {
        return "Group Rank Command";
    }

    @Override
    public String getDescription() {
        return "Rank someone in the Roblox Group.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Collections.singletonList(
            "`:command` - Ranking in the roblox Groups."
        );
    }

    @Override
    public List<String> getExampleUsage() {
        return Collections.singletonList(
            "`:command` - Ranking in the roblox Groups."
        );
    }


    @Override
    public List<String> getTriggers() {
        return Arrays.asList("group-rank", "gr");
    }

    @Nonnull
    @Override
    public List<CommandGroup> getGroups() {
        return Collections.singletonList(
            CommandGroups.MISCELLANEOUS
        );
    }

    @Override
    public List<String> getMiddleware() {
        return Arrays.asList(
            "throttle:user,1,10",
            "isManagerOrHigher"
        );
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {

        if (avaire.getConfig().getString("apiKeys.nobloxServerAPIKey") == null | avaire.getConfig().getString("apiKeys.nobloxServerAPIKey").length() < 1) {
            context.makeError("An noblox api key could not be found. Please enter it in the config.yml").queue();
            return false;
        }

        if (avaire.getConfig().getString("URL.noblox") == null | avaire.getConfig().getString("URL.noblox").length() < 1) {
            context.makeError("An noblox webserver could not be found. Please enter it in the config.yml").queue();
            return false;
        }

        if (context.getGuildTransformer() == null) {
            context.makeError("Transformer could not be loaded!").queue();
            return false;
        }

        if (context.getGuildTransformer().getRobloxGroupId() == 0) {
            context.makeError("The roblox ID of this group has not been set, please request a Facilitator or above to set this for you with `;rmanage`.").queue();
            return false;
        }

        context.makeWarning("This command will allow you to rank someone in the configured group ID (``"+context.getGuildTransformer().getRobloxGroupId()+"``).").queue();

        if (context.getGuildTransformer().getRobloxGroupId() == 0) {
            context.makeError("No group ID has been set for this guild!").queue();
            return false;
        }

        Long botAccount = getRobloxId("PB_Xbot");

        Request.Builder request = new Request.Builder()
            .addHeader("User-Agent", "Xeus v" + AppInfo.getAppInfo().version)
            .url("");


        context.makeInfo("What Roblox user would you like to edit?").queue(v -> {
            avaire.getWaiter().waitForEvent(GuildMessageReceivedEvent.class, l -> {
                return (l.getChannel().equals(context.getChannel()) &&
                    l.getMember() != null && l.getMember().equals(context.getMember()) &&
                    getRobloxId(l.getMessage().getContentRaw()) != null) ||
                    l.getMessage().getContentRaw().equalsIgnoreCase("cancel");
            }, k -> {
                if (k.getMessage().getContentRaw().equalsIgnoreCase("cancel")) {
                    context.makeInfo("Cancelled").queue();
                    return;
                }




                //sendMessage(context, k);
            });
        });
        return false;
    }

    private void sendMessage(CommandMessage context, GuildMessageReceivedEvent k) {
        String message = k.getMessage().getContentRaw();


        Long id = getRobloxId(context.getMember().getEffectiveName());
        if (id == null) {
            context.makeError("I coudn't find ");
            return;
        }

        Request.Builder request = new Request.Builder()
            .addHeader("User-Agent", "Xeus v" + AppInfo.getAppInfo().version)
            .url(avaire.getConfig().getString("URL.noblox").replace("%location%", "GroupShout"));
            //.post(RequestBody.create(json, buildPayload(message, context.getGuildTransformer().getRobloxGroupId())));

        try (Response response = client.newCall(request.build()).execute()) {
            context.makeSuccess("Message sent to the [group wall](https://www.roblox.com/groups/:RobloxID): \n```" + response.message() + "```").set("RobloxID", context.getGuildTransformer().getRobloxGroupId()).queue();
        } catch (IOException e) {
            AvaIre.getLogger().error("Failed sending sync with beacon request: " + e.getMessage());
        }
    }

    private String buildPayload(long userId, long robloxId, int rankId) {
        JSONObject main = new JSONObject();

        main.put("auth_key", avaire.getConfig().getString("apiKeys.nobloxServerAPIKey"));
        main.put("Group", robloxId);
        main.put("Target", userId);
        main.put("Rank", rankId);

        return main.toString();
    }

    public Long getRobloxId(String un) {
        try {
            JSONObject json = readJsonFromUrl("http://api.roblox.com/users/get-by-username?username=" + un);
            return json.getLong("Id");
        } catch (Exception e) {
            return null;
        }
    }

}

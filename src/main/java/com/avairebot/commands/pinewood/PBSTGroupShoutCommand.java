package com.avairebot.commands.pinewood;

import com.avairebot.AppInfo;
import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import okhttp3.*;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PBSTGroupShoutCommand extends Command {
    public PBSTGroupShoutCommand(AvaIre avaire) {
        super(avaire);
    }

    private static final MediaType json = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public String getName() {
        return "Group Shout Command";
    }

    @Override
    public String getDescription() {
        return "Shouts in the Roblox Group.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Collections.singletonList(
            "`:command` - Shouting in the roblox Groups."
        );
    }

    @Override
    public List<String> getExampleUsage() {
        return Collections.singletonList(
            "`:command` - Shouting in the roblox Groups."
        );
    }


    @Override
    public List<String> getTriggers() {
        return Arrays.asList("group-shout", "gs");
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
            "isBotAdmin"
        );
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (!context.member.getId().equals("173839105615069184")) {
            context.makeWarning("PLEASE BE WARNED, THIS WILL SEND A GROUP SHOUT AS **superstefano4** TO THE GROUP CONNECTED TO THE GUILD.").queue();
        }
        if (context.getGuildTransformer() == null) {
            context.makeError("Transformer could not be loaded!").queue();
            return false;
        }
        if (context.getGuildTransformer().getRobloxGroupId() == 0) {
            context.makeError("No group ID has been set for this guild!").queue();
            return false;
        }

        context.makeInfo("What would you like to shout? (Be warned, this sends **INSTANTLY** after you send the message). Say ``cancel`` to cancel sending the message.").queue(v -> {
            avaire.getWaiter().waitForEvent(GuildMessageReceivedEvent.class, l -> {
                return l.getChannel().equals(context.getChannel()) && l.getMember().equals(context.getMember());
            }, k -> {
                if (k.getMessage().getContentRaw().equalsIgnoreCase("cancel")) {
                    context.makeInfo("Cancelled").queue();
                    return;
                }
                Request.Builder request = new Request.Builder()
                    .addHeader("User-Agent", "Xeus v" + AppInfo.getAppInfo().version)
                    .url(avaire.getConfig().getString("URL.noblox"))
                    .post(RequestBody.create(json, buildPayload(k.getMessage().getContentRaw(), context.getGuildTransformer().getRobloxGroupId())));

                try (Response response = client.newCall(request.build()).execute()) {
                    context.makeSuccess("Message sent to the [group wall](https://www.roblox.com/groups/:RobloxID): \n```" + response.message() + "```").set("RobloxID", context.getGuildTransformer().getRobloxGroupId()).queue();
                } catch (IOException e) {
                    AvaIre.getLogger().error("Failed sending sync with beacon request: " + e.getMessage());
                }
            });
        });
        return false;
    }

    private String buildPayload(String contentRaw, int robloxId) {
        JSONObject main = new JSONObject();

        main.put("auth_key", avaire.getConfig().getString("apiKeys.nobloxServerAPIKey"));
        main.put("Group", robloxId);
        main.put("Message", contentRaw);

        return main.toString();
    }

}

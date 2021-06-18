package com.avairebot.commands.pinewood;

import com.avairebot.AppInfo;
import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import com.avairebot.utilities.CheckPermissionUtil;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import okhttp3.*;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GroupShoutCommand extends Command {
    public GroupShoutCommand(AvaIre avaire) {
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
            "throttle:user,1,10",
            "isGroupShoutOrHigher"
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

        context.makeWarning("PLEASE BE WARNED, THIS WILL SEND A GROUP SHOUT AS **PB_XBot** (If PB_XBot has permission to shout on ``"+context.getGuildTransformer().getRobloxGroupId()+"``) TO THE GROUP CONNECTED TO THE GUILD.").queue();

        if (context.getGuildTransformer().getRobloxGroupId() == 0) {
            context.makeError("No group ID has been set for this guild!").queue();
            return false;
        }

        context.makeInfo("What would you like to shout? (Be warned, if you're authorised to use this, then this will send **INSTANTLY** after you send the message). Say ``cancel`` to cancel sending the message.").queue(v -> {
            avaire.getWaiter().waitForEvent(GuildMessageReceivedEvent.class, l -> {
                return l.getChannel().equals(context.getChannel()) && l.getMember().equals(context.getMember());
            }, k -> {
                if (k.getMessage().getContentRaw().equalsIgnoreCase("cancel")) {
                    context.makeInfo("Cancelled").queue();
                    return;
                }


                sendMessage(context, k);
            });
        });
        return false;
    }

    private void sendMessage(CommandMessage context, GuildMessageReceivedEvent k) {
        String message = k.getMessage().getContentRaw();
        int charLimit = 255;
        if ((context.getGuild().getId().equals("438134543837560832") && !context.getAuthor().getId().equals("173839105615069184")) ||
            CheckPermissionUtil.getPermissionLevel(context).getLevel() >= CheckPermissionUtil.GuildPermissionCheckType.MOD.getLevel()) {
            if ((context.getMember().getRoles().contains(context.getGuild().getRoleById("771360204696059904")) || context.getMember().getRoles().contains(context.getGuild().getRoleById("771360069036146708"))) || CheckPermissionUtil.getPermissionLevel(context).getLevel() >= CheckPermissionUtil.GuildPermissionCheckType.MOD.getLevel()) {
                charLimit = charLimit - context.getMember().getEffectiveName().length() + 2;
                message = context.getMember().getEffectiveName() + ": " + message;
            } else {
                context.makeError("You're not authorised to use this feature.").queue();
                return;
            }
        } else {
            context.makeError("This guild is not authorised to use this feature.").queue();
            return;
        }

        if (message.length() >= charLimit) {
            context.makeError("Sorry, but this message is to long to be sent to the group shout. (Roblox has a limit of 255 characters)").queue();
            return;
        }

        Request.Builder request = new Request.Builder()
            .addHeader("User-Agent", "Xeus v" + AppInfo.getAppInfo().version)
            .url(avaire.getConfig().getString("URL.noblox"))
            .post(RequestBody.create(json, buildPayload(message, context.getGuildTransformer().getRobloxGroupId())));

        try (Response response = client.newCall(request.build()).execute()) {
            context.makeSuccess("Message sent to the [group wall](https://www.roblox.com/groups/:RobloxID): \n```" + response.message() + "```").set("RobloxID", context.getGuildTransformer().getRobloxGroupId()).queue();
        } catch (IOException e) {
            AvaIre.getLogger().error("Failed sending sync with beacon request: " + e.getMessage());
        }
    }

    private String buildPayload(String contentRaw, int robloxId) {
        JSONObject main = new JSONObject();

        main.put("auth_key", avaire.getConfig().getString("apiKeys.nobloxServerAPIKey"));
        main.put("Group", robloxId);
        main.put("Message", contentRaw);

        return main.toString();
    }

}

package com.avairebot.commands.globalmod;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.utilities.NumberUtil;
import net.dv8tion.jda.api.entities.Guild;

import javax.annotation.Nonnull;
import java.sql.SQLException;
import java.util.*;

public class GlobalModCommand extends Command {

    public GlobalModCommand(AvaIre avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "Global Mod Command";
    }

    @Override
    public String getDescription() {
        return "Manage the global moderation settings (Filter for emotes, mentions etc).";
    }

    @Override
    public List <String> getUsageInstructions() {
        return Collections.singletonList(
            "`:command` - Manage the moderation across all PB Guilds.");
    }

    @Override
    public List <String> getTriggers() {
        return Arrays.asList("global-mod");
    }

    @Override
    public List <String> getMiddleware() {
        return Arrays.asList(
            "isOfficialPinewoodGuild",
            "isValidPIAMember"
        );
    }

    @Nonnull
    @Override
    public List <CommandGroup> getGroups() {
        return Collections.singletonList(CommandGroups.MODERATION);
    }

    public final ArrayList <String> guilds = new ArrayList <String>() {{
        add("495673170565791754"); // Aerospace
        add("438134543837560832"); // PBST
        add("791168471093870622"); // Kronos Dev
        add("371062894315569173"); // Official PB Server
        add("514595433176236078"); // PBQA
        add("436670173777362944"); // PET
        add("505828893576527892"); // MMFA
        add("498476405160673286"); // PBM
        add("572104809973415943"); // TMS
        add("758057400635883580"); // PBOP
        add("669672893730258964"); // PB Dev
    }};


    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (args.length == 0) {
            context.makeInfo("Please select what setting you'd like to modify (0 = Disabled)\n" +
                " - ``mass-mention``\n" +
                " - ``emoji-spam``\n" +
                " - ``link-spam``\n" +
                " - ``message-spam``\n" +
                " - ``image-spam``\n" +
                " - ``character-spam``\n"
            ).queue();
            return false;
        }

        GuildTransformer transformer = context.getGuildTransformer();
        if (transformer == null) {
            context.makeError("Server settings cannot be loaded.").queue();
            return false;
        }

        switch (args[0]) {
            case "mass-mention":
                return runMentionUpdateCommand(context, args, transformer);
            case "emoji-spam":
                return runEmojiSpamUpdateCommand(context, args, transformer);
            case "link-spam":
                return runLinkSpamUpdateCommand(context, args, transformer);
            case "message-spam":
                return runMessageSpamUpdateCommand(context, args, transformer);
            case "image-spam":
                return runImageSpamUpdateCommand(context, args, transformer);
            case "character-spam":
                return runCharacterSpamUpdateCommand(context, args, transformer);
        }

        return true;
    }

    private boolean runCharacterSpamUpdateCommand(CommandMessage context, String[] args, GuildTransformer transformer) {
        if (NumberUtil.isNumeric(args[1])) {
            context.makeInfo("Update character spam to " + args[1]).queue();
            transformer.setCharacterSpam(Integer.parseInt(args[1]));
            return updateRecordInDatabase(context, "automod_character_spam", transformer.getCharacterSpam());
        } else {
            context.makeError("Please enter a number.").queue();
            return false;
        }

    }

    private boolean runImageSpamUpdateCommand(CommandMessage context, String[] args, GuildTransformer transformer) {
        context.makeInfo("Update image spam to " + args[1]).queue();
        if (NumberUtil.isNumeric(args[1])) {
            context.makeInfo("Update link spam to " + args[1]).queue();
            transformer.setImageSpam(Integer.parseInt(args[1]));
            return updateRecordInDatabase(context, "automod_image_spam", transformer.getImageSpam());
        } else {
            context.makeError("Please enter a number.").queue();
            return false;
        }
    }

    private boolean runMessageSpamUpdateCommand(CommandMessage context, String[] args, GuildTransformer transformer) {
        if (NumberUtil.isNumeric(args[1])) {
            context.makeInfo("Update message spam to " + args[1]).queue();
            transformer.setMessageSpam(Integer.parseInt(args[1]));
            return updateRecordInDatabase(context, "automod_message_spam", transformer.getMessageSpam());
        } else {
            context.makeError("Please enter a number.").queue();
            return false;
        }
    }

    private boolean runLinkSpamUpdateCommand(CommandMessage context, String[] args, GuildTransformer transformer) {
        if (NumberUtil.isNumeric(args[1])) {
            context.makeInfo("Update link spam to " + args[1]).queue();
            transformer.setLinkSpam(Integer.parseInt(args[1]));
            return updateRecordInDatabase(context, "automod_link_spam", transformer.getLinkSpam());
        } else {
            context.makeError("Please enter a number.").queue();
            return false;
        }
    }

    private boolean runEmojiSpamUpdateCommand(CommandMessage context, String[] args, GuildTransformer transformer) {
        if (NumberUtil.isNumeric(args[1])) {
            context.makeInfo("Update emoji spam to " + args[1]).queue();
            transformer.setEmojiSpam(Integer.parseInt(args[1]));
            return updateRecordInDatabase(context, "automod_emoji_spam", transformer.getEmojiSpam());
        } else {
            context.makeError("Please enter a number.").queue();
            return false;
        }
    }

    private boolean runMentionUpdateCommand(CommandMessage context, String[] args, GuildTransformer transformer) {
        if (NumberUtil.isNumeric(args[1])) {
            context.makeInfo("Update mention spam to " + args[1]).queue();
            transformer.setMassMentionSpam(Integer.parseInt(args[1]));
            return updateRecordInDatabase(context, "automod_mass_mention", transformer.getMassMentionSpam());
        } else {
            context.makeError("Please enter a number.").queue();
            return false;
        }
    }

    private boolean updateRecordInDatabase(CommandMessage context, String table, int setTo) {
        try {
            ArrayList<Guild> guild = new ArrayList <>();

            for (String id : guilds) {
                Guild g = avaire.getShardManager().getGuildById(id);
                if (g !=null) {
                    guild.add(g);
                }
            }

            for (Guild g : guild) {
                avaire.getDatabase().newQueryBuilder(Constants.GUILD_TABLE_NAME).where("id", g.getId()).update(p -> {
                    p.set(table, setTo);
                });
            }
            context.makeSuccess("Updated!").queue();
            return true;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            context.makeError("Database error!").queue();
            return false;
        }
    }

}


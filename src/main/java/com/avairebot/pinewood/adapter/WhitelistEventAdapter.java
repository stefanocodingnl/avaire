package com.avairebot.pinewood.adapter;

import com.avairebot.AvaIre;
import com.avairebot.database.controllers.GuildController;
import com.avairebot.pinewood.VoiceWhitelistManager;
import com.avairebot.utilities.CheckPermissionUtil;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;

public class WhitelistEventAdapter {
    private final AvaIre avaire;
    private final VoiceWhitelistManager whitelistManager;

    public WhitelistEventAdapter(AvaIre avaire, VoiceWhitelistManager whitelist) {
        this.avaire = avaire;
        this.whitelistManager = whitelist;
    }

    public void whitelistCheckEvent(GenericGuildVoiceEvent event) {
        VoiceChannel newVc = getNewVC(event);

        if (!whitelistManager.hasWhitelist(newVc)) {
            return;
        }

        int lvl = CheckPermissionUtil.getPermissionLevel(GuildController.fetchGuild(avaire, event.getGuild()), event.getGuild(), event.getMember()).getLevel();
        if (!(lvl >= CheckPermissionUtil.GuildPermissionCheckType.MOD.getLevel())) {
            if (!whitelistManager.isInWhitelist(newVc, event.getMember())) {
                event.getGuild().kickVoiceMember(event.getMember()).queue();
                event.getMember().getUser().openPrivateChannel().queue(l -> {
                    l.sendMessage("Sorry, but you're not on the whitelist for " + newVc.getName() + ". Please ask the mod who asked you to join to whitelist you. (Unless you passed the window to join)").queue();
                });

            }
        }
    }

    private VoiceChannel getNewVC(GenericGuildVoiceEvent event) {
        if (event instanceof GuildVoiceJoinEvent) {
            return ((GuildVoiceJoinEvent) event).getChannelJoined();
        } else if (event instanceof GuildVoiceMoveEvent) {
            return ((GuildVoiceMoveEvent) event).getChannelJoined();
        } else {
            return event.getVoiceState().getChannel();
        }
    }
}

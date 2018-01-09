package com.avairebot.commands.music;

import com.avairebot.AvaIre;
import com.avairebot.audio.AudioHandler;
import com.avairebot.audio.GuildMusicManager;
import com.avairebot.contracts.commands.Command;
import com.avairebot.factories.MessageFactory;
import net.dv8tion.jda.core.entities.Message;

import java.util.Arrays;
import java.util.List;

public class RepeatMusicQueueCommand extends Command {

    public RepeatMusicQueueCommand(AvaIre avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "Repeat Music Command";
    }

    @Override
    public String getDescription() {
        return "Repeats all the songs in the music queue.";
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("repeatsongs", "repeat", "loop");
    }

    @Override
    public List<String> getMiddleware() {
        return Arrays.asList("has-role:DJ", "throttle:guild,2,4");
    }

    @Override
    public boolean onCommand(Message message, String[] args) {
        GuildMusicManager musicManager = AudioHandler.getGuildAudioPlayer(message.getGuild());

        if (musicManager.getPlayer().getPlayingTrack() == null) {
            return sendErrorMessage(message, "There is nothing to repeat, request music first with `!play`");
        }

        musicManager.setRepeatQueue(!musicManager.isRepeatQueue());

        MessageFactory.makeSuccess(message, "Music queue looping has been turned `:status`.")
            .set("status", musicManager.isRepeatQueue() ? "ON" : "OFF")
            .queue();

        return true;
    }
}
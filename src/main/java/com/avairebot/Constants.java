/*
 * Copyright (c) 2018.
 *
 * This file is part of AvaIre.
 *
 * AvaIre is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AvaIre is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AvaIre.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 */

package com.avairebot;

import java.io.File;
import java.util.ArrayList;

@SuppressWarnings("WeakerAccess")
public class Constants {

    public static final File STORAGE_PATH = new File("storage");

    // Database Tables
    public static final String GUILD_TABLE_NAME = "guilds";
    public static final String GUILD_TYPES_TABLE_NAME = "guild_types";
    public static final String STATISTICS_TABLE_NAME = "statistics";
    public static final String BLACKLIST_TABLE_NAME = "blacklists";
    public static final String PLAYER_EXPERIENCE_TABLE_NAME = "experiences";
    public static final String VOTES_TABLE_NAME = "votes";
    public static final String FEEDBACK_TABLE_NAME = "feedback";
    public static final String MUSIC_PLAYLIST_TABLE_NAME = "playlists";
    public static final String SHARDS_TABLE_NAME = "shards";
    public static final String LOG_TABLE_NAME = "logs";
    public static final String LOG_TYPES_TABLE_NAME = "log_types";
    public static final String REACTION_ROLES_TABLE_NAME = "reaction_roles";
    public static final String PURCHASES_TABLE_NAME = "purchases";
    public static final String MUTE_TABLE_NAME = "mutes";

    public static final String MUSIC_SEARCH_PROVIDERS_TABLE_NAME = "music_search_providers";
    public static final String MUSIC_SEARCH_CACHE_TABLE_NAME = "music_search_cache";
    public static final String INSTALLED_PLUGINS_TABLE_NAME = "installed_plugins";

    // Pinewood Specific Tables
    public static final String MOTS_VOTE_TABLE_NAME = "xeus_vote";
    public static final String MOTS_VOTES_TABLE_NAME = "xeus_votes";
    public static final String MOTS_VOTABLE_TABLE_NAME = "xeus_votable";

    public static final String PB_SUGGESTIONS_TABLE_NAME = "pinewood_suggestions";
    public static final String REPORTS_DATABASE_TABLE_NAME = "pinewood_reports";
    public static final String REMITTANCE_DATABASE_TABLE_NAME = "pinewood_patrol_remittance";
    public static final String EVENT_SCHEDULE_REQUESTS_TABLE = "pinewood_event_requests";

    public static final String ON_WATCH_TABLE_NAME = "on_watch";
    public static final String ON_WATCH_LOG_TABLE_NAME = "on_watch_logs";
    public static final String ON_WATCH_TYPES_TABLE_NAME = "on_watch_types";

    public static final String ROLE_PERSISTENCE_TABLE_NAME = "role_persistence";

    public static final String ANTI_UNBAN_TABLE_NAME = "pia_antiban";
    public static final String FEATURE_BLACKLIST_TABLE_NAME = "feature_blacklist";

    public static final String EVALS_DATABASE_TABLE_NAME = "pinewood_evaluations";
    public static final String EVALS_LOG_DATABASE_TABLE_NAME = "pinewood_evaluations_log";

    // Package Specific Information
    public static final String PACKAGE_MIGRATION_PATH = "com.avairebot.database.migrate";
    public static final String PACKAGE_SEEDER_PATH = "com.avairebot.database.seeder";
    public static final String PACKAGE_COMMAND_PATH = "com.avairebot.commands";
    public static final String PACKAGE_INTENTS_PATH = "com.avairebot.ai.dialogflow.intents";
    public static final String PACKAGE_JOB_PATH = "com.avairebot.scheduler";

    // Emojis
    public static final String EMOTE_ONLINE = "<:green_circle:679666667672174592>";
    public static final String EMOTE_AWAY = "<:yellow_circle:679666871368417290>";
    public static final String EMOTE_DND = "<:red_circle:6796668916982088550>";

    // Purchase Types
    public static final String RANK_BACKGROUND_PURCHASE_TYPE = "rank-background";

    // Audio Metadata
    public static final String AUDIO_HAS_SENT_NOW_PLAYING_METADATA = "has-sent-now-playing";

    // Command source link
    public static final String SOURCE_URI = "https://gitlab.com/pinewood-builders/discord/xeus/-/blob/master/src/main/java/com/avairebot/commands/%s/%s.java";

    public static final String REWARD_REQUESTS_CHANNEL_ID = "722606219319181392";
    public static final String PIA_LOG_CHANNEL = "788316320747094046";

    // Official Pinewood Guilds
    public static final ArrayList <String> guilds = new ArrayList<String>() {{
        add("495673170565791754"); // Aerospace
        add("438134543837560832"); // PBST
        add("791168471093870622"); // Kronos Dev
        add("371062894315569173"); // Official PB Server
        add("514595433176236078"); // PBQA
        add("436670173777362944"); // PET
        add("505828893576527892"); // MMFA
        add("498476405160673286"); // PBM
        add("572104809973415943"); // TMS
        add("697546632040022186"); // PWA (Wiki Administration)
        add("669672893730258964"); // PB Dev Lair
        add("699379074505637908"); // PTE (PBST Tier Evals)
        add("750471488095780966"); // PBA (Pinewood Builders Appeals)
        add("758057400635883580"); // PBOP
    }};

    // BYPASS USERS
    public static final ArrayList <String> piaMembers = new ArrayList<String>() {{
        add("251818929226383361"); // CombatSwift
        add("194517256389132288"); // Coasterteam
        add("131917628800237570"); // Soppo
        add("610591577961791500"); // C3N0
        add("202926188522504192"); // Sparked
        add("142083279309373440"); // Wickey
        add("723151849640820759"); // Csdi
        add("315231688290730005"); // LENEMAR
        add("257193596074065921"); // Omni
        add("137235914924490761"); // ood
        add("235086178309636096"); // RogueVader
        add("252228224904331268"); // Supremo
        add("329668217515540482"); // TenX
        add("148420768324124672"); // Diddleshot
        add("314505421870333952"); // AnuCat
        add("446775463441072130"); // Oak
    }};
    ;
}

package com.avairebot.database.migrate.migrations;

import com.avairebot.Constants;
import com.avairebot.contracts.database.migrations.Migration;
import com.avairebot.database.schema.Schema;

import java.sql.SQLException;

public class CreatePBVotesTableMigration implements Migration {

    @Override
    public String created_at() {
        return "Thu, Oct 15, 2020 10:38 PM";
    }

    @Override
    public boolean up(Schema schema) throws SQLException {
        return schema.createIfNotExists(Constants.MOTS_VOTES_TABLE_NAME, table -> {
            table.Increments("id");
            table.String("question");
            table.Integer("total_votes");
            table.String("vote_id");
            table.Long("guild_id");
            table.Boolean("active").defaultValue(true);
            //table.DateTime("end_date");
            table.Timestamps();
        });

    }

    @Override
    public boolean down(Schema schema) throws SQLException {
        return schema.dropIfExists(Constants.MOTS_VOTES_TABLE_NAME);
    }
}

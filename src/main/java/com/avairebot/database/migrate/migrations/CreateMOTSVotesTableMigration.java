package com.avairebot.database.migrate.migrations;

import com.avairebot.Constants;
import com.avairebot.contracts.database.migrations.Migration;
import com.avairebot.database.schema.Schema;

import java.sql.SQLException;

public class CreateMOTSVoteTableMigration implements Migration {

    @Override
    public String created_at() {
        return "Thursday, April 2nd, 2020 11:38 PM";
    }

    @Override
    public boolean up(Schema schema) throws SQLException {
        return schema.createIfNotExists(Constants.MOTS_VOTES_TABLE_NAME, table -> {
            table.Increments("id");
            table.String("voted_for");
            table.Long("vote_message_id");
            table.Long("voter_user_id");
            table.String("description");
            table.Boolean("accepted");

            table.Timestamps();
        });
    }

    @Override
    public boolean down(Schema schema) throws SQLException {
        return schema.dropIfExists(Constants.MOTS_VOTES_TABLE_NAME);
    }
}

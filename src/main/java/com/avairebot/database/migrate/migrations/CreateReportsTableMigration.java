package com.avairebot.database.migrate.migrations;

import com.avairebot.Constants;
import com.avairebot.contracts.database.migrations.Migration;
import com.avairebot.database.schema.Schema;

import java.sql.SQLException;

public class CreateReportsTableMigration implements Migration {

    @Override
    public String created_at() {
        return "Thursday, April 2nd, 2020 11:38 PM";
    }

    @Override
    public boolean up(Schema schema) throws SQLException {
        return schema.createIfNotExists(Constants.REPORTS_DATABASE_TABLE_NAME, table -> {
            table.Increments("id");
            table.Long("pb_server_id");
            table.Long("report_message_id");
            table.Long("reporter_discord_id");
            table.String("reporter_discord_name");
            table.Long("reported_roblox_id").unsigned();
            table.String("reporter_discord_name");
            table.String("reported_roblox_name");
            table.String("report_punishment").nullable();
            table.String("report_evidence");
            table.String("report_date");
            table.String("report_reason");
            table.Timestamps();
        });
    }

    @Override
    public boolean down(Schema schema) throws SQLException {
        return schema.dropIfExists(Constants.REPORTS_DATABASE_TABLE_NAME);
    }
}

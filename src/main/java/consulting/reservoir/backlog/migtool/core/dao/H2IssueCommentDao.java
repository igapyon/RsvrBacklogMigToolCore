/*
 * Copyright 2022 Reservoir Consulting - Toshiki Iga
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulting.reservoir.backlog.migtool.core.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

import com.nulabinc.backlog4j.ChangeLog;
import com.nulabinc.backlog4j.IssueComment;
import com.nulabinc.backlog4j.Notification;

import consulting.reservoir.backlog.migtool.core.RsvrBacklogMigToolProcessInfo;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;

/**
 * h2 database に対する 課題の `Comment` に関する DAO クラス。
 */
public class H2IssueCommentDao {
    /**
     * このDaoが対象とするテーブルを作成。
     * 
     * @param conn データベース接続。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " //
                + "BacklogIssueComment (" //
                + "IssueCommentId BIGINT NOT NULL" //
                + ",IssueId BIGINT" //
                + ",Content VARCHAR(65535)" //
                + ",CreatedUser BIGINT" //
                + ",Created TIMESTAMP" //
                + ",Updated TIMESTAMP" //
                + ",Notification VARCHAR(65535)" //
                + ",PRIMARY KEY(IssueCommentId)" //
                + ")" //
        ))) {
            stmt.executeUpdate();
        }
    }

    /**
     * 与えられた情報を Dao 経由でデータベースに格納します。
     * 
     * Starsは対象から除外しています。
     * 
     * @param conn    データベース接続
     * @param issueId 課題Id。
     * @param source  格納したいデータ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void store2Local(Connection conn, IssueComment source, RsvrBacklogMigToolProcessInfo processInfo,
            long issueId) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("SELECT IssueCommentId" //
                + " FROM BacklogIssueComment" //
                + " WHERE IssueCommentId =  " + source.getId() //
        ))) {
            boolean isNew = false;
            try (RsvrResultSet rset = stmt.executeQuery()) {
                isNew = (rset.next() == false);
            }

            if (isNew) {
                try (RsvrPreparedStatement stmtMod = RsvrJdbc
                        .wrap(conn.prepareStatement("INSERT INTO BacklogIssueComment (IssueCommentId) VALUES (?)" //
                        ))) {
                    stmtMod.setLong(source.getId());
                    stmtMod.executeUpdateSingleRow();
                }
                processInfo.incrementIns("IssueComment");
            } else {
                processInfo.incrementUpd("IssueComment");
            }

            // 他の項目は全てUPDATEで処理する。
            try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement("UPDATE BacklogIssueComment SET " //
                    + "IssueId=?, Content=?" //
                    + " , CreatedUser=?, Created=?, Updated=?, Notification=?" //
                    + " WHERE IssueCommentId = ?"))) {
                stmtMod.setLong(issueId);
                stmtMod.setString(source.getContent());
                if (source.getCreatedUser() == null) {
                    stmtMod.setNull(Types.BIGINT);
                } else {
                    stmtMod.setLong(source.getCreatedUser().getId());
                    H2UserDao.store2Local(conn, source.getCreatedUser(), processInfo);
                }
                stmtMod.setJavaUtilDate(source.getCreated());
                stmtMod.setJavaUtilDate(source.getUpdated());

                {
                    String buildString = "";
                    for (Notification look : source.getNotifications()) {
                        if (buildString.length() != 0) {
                            buildString += ",";
                        }

                        buildString += look.getUser().getId();
                        H2UserDao.store2Local(conn, look.getUser(), processInfo);
                    }
                    stmtMod.setString(buildString);

                }

                // ChangeLogのAPI結果にIdがないため、ここで人為的に1オリジンのキー項目を作成。
                int changeLogSec = 1;
                for (ChangeLog look : source.getChangeLog()) {
                    H2IssueCommentChangeLogDao.store2Local(conn, look, processInfo,
                            source.getId() + "-" + (changeLogSec++), issueId, source.getId());
                }

                stmtMod.setLong(source.getId());
                stmtMod.executeUpdateSingleRow();
            }
        }
    }
}

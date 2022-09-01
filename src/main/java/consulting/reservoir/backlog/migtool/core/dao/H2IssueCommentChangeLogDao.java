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

import com.nulabinc.backlog4j.ChangeLog;

import consulting.reservoir.backlog.migtool.core.RsvrBacklogMigToolProcessInfo;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;

/**
 * h2 database に対する 課題の `Comment` の変更ログ に関する DAO クラス。
 */
public class H2IssueCommentChangeLogDao {
    /**
     * このDaoが対象とするテーブルを作成。
     * 
     * @param conn データベース接続。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " //
                + "BacklogIssueCommentChangeLog (" //
                + "IssueCommentChangeLogId VARCHAR(80) NOT NULL" //
                + ",IssueCommentId BIGINT" //
                + ",Field VARCHAR(8192)" //
                + ",OriginalValue VARCHAR(65535)" //
                + ",NewValue VARCHAR(65535)" //
                + ",IssueAttachmentId BIGINT" //
                + ",AttributeInfo VARCHAR(8192)" //
                + ",NotificationInfo VARCHAR(8192)" //
                + ",PRIMARY KEY(IssueCommentChangeLogId)" //
                + ")" //
        ))) {
            stmt.executeUpdate();
        }
    }

    /**
     * 与えられた情報を Dao 経由でデータベースに格納します。
     * 
     * @param conn                    データベース接続
     * @param issueCommentChangeLogId 課題コメント変更ログのId。
     * @param issueId                 課題Id。
     * @param issueCommentId          課題コメントId。
     * @param source                  格納したいデータ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void store2Local(Connection conn, ChangeLog source, RsvrBacklogMigToolProcessInfo processInfo,
            String issueCommentChangeLogId, long issueId, long issueCommentId) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("SELECT IssueCommentChangeLogId" //
                + " FROM BacklogIssueCommentChangeLog" //
                + " WHERE IssueCommentChangeLogId = ?"))) {
            stmt.setString(issueCommentChangeLogId);
            boolean isNew = false;
            try (RsvrResultSet rset = stmt.executeQuery()) {
                isNew = (rset.next() == false);
            }

            if (isNew) {
                try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement(
                        "INSERT INTO BacklogIssueCommentChangeLog (IssueCommentChangeLogId) VALUES (?)" //
                ))) {
                    stmtMod.setString(issueCommentChangeLogId);
                    stmtMod.executeUpdateSingleRow();
                }
                processInfo.incrementIns("IssueCommentChangeLog");
            } else {
                processInfo.incrementUpd("IssueCommentChangeLog");
            }

            // 他の項目は全てUPDATEで処理する。
            try (RsvrPreparedStatement stmtMod = RsvrJdbc
                    .wrap(conn.prepareStatement("UPDATE BacklogIssueCommentChangeLog SET " //
                            + "IssueCommentId=?, Field=?" //
                            + " , OriginalValue=?, NewValue=?, IssueAttachmentId=?, AttributeInfo=?, NotificationInfo=?" //
                            + " WHERE IssueCommentChangeLogId = ?"))) {
                stmtMod.setLong(issueCommentId);
                stmtMod.setString(source.getField());
                stmtMod.setString(source.getOriginalValue());
                stmtMod.setString(source.getNewValue());
                if (source.getAttachmentInfo() == null) {
                    stmtMod.setNull(java.sql.Types.BIGINT);
                } else {
                    stmtMod.setLong(source.getAttachmentInfo().getId());
                }

                if (source.getAttributeInfo() == null) {
                    stmtMod.setNull(java.sql.Types.NVARCHAR);
                } else {
                    stmtMod.setString("Id:" + source.getAttributeInfo().getId() + ", type:"
                            + source.getAttributeInfo().getTypeId());

                }

                stmtMod.setString((source.getNotificationInfo() == null ? "" : source.getNotificationInfo().getType()));

                stmtMod.setString(issueCommentChangeLogId);
                stmtMod.executeUpdateSingleRow();
            }

            if (source.getAttributeInfo() != null) {
                // カスタム項目を更新
                H2IssueCustomFieldDao.store2Local(conn, processInfo, issueId, source.getAttributeInfo().getId(),
                        source.getNewValue(), source.getAttributeInfo().getTypeId());
            }
        }
    }
}

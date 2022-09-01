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

import com.nulabinc.backlog4j.CustomField;

import consulting.reservoir.backlog.migtool.core.RsvrBacklogMigToolProcessInfo;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;

/**
 * h2 database に対する 課題の `CustomField` に関する DAO クラス。
 */
public class H2IssueCustomFieldDao {
    /**
     * このDaoが対象とするテーブルを作成。
     * 
     * @param conn データベース接続。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " //
                + "BacklogIssueCustomField (" //
                + "IssueId BIGINT NOT NULL" // システム都合で追加。
                + ",IssueCustomFieldId BIGINT NOT NULL" //
                + ",Name VARCHAR(8192)" //
                + ",CustomValue VARCHAR(65535)" // システム都合で追加
                + ",FieldTypeId VARCHAR(80)" // APIがStringを戻す場合があるため。
                + ",PRIMARY KEY(IssueId, IssueCustomFieldId)" //
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
    public static void store2Local(Connection conn, CustomField source, RsvrBacklogMigToolProcessInfo processInfo,
            long issueId) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement( //
                "SELECT IssueCustomFieldId" //
                        + " FROM BacklogIssueCustomField" //
                        + " WHERE IssueId = " + issueId + " AND IssueCustomFieldId = " + source.getId() //
        ))) {
            boolean isNew = false;
            try (RsvrResultSet rset = stmt.executeQuery()) {
                isNew = (rset.next() == false);
            }

            if (isNew) {
                try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement(
                        "INSERT INTO BacklogIssueCustomField (IssueId, IssueCustomFieldId) VALUES (?,?)" //
                ))) {
                    stmtMod.setLong(issueId);
                    stmtMod.setLong(source.getId());
                    stmtMod.executeUpdateSingleRow();
                }
                processInfo.incrementIns("IssueCustomField");
            } else {
                processInfo.incrementUpd("IssueCustomField");
            }

            // 他の項目は全てUPDATEで処理する。
            try (RsvrPreparedStatement stmtMod = RsvrJdbc
                    .wrap(conn.prepareStatement("UPDATE BacklogIssueCustomField SET " //
                            + "Name=?, FieldTypeId=?" //
                            + " WHERE IssueCustomFieldId = ? AND IssueId = ?"))) {
                stmtMod.setString(source.getName());
                stmtMod.setInt(source.getFieldTypeId());
                stmtMod.setLong(source.getId());
                stmtMod.setLong(issueId);
                stmtMod.executeUpdateSingleRow();
            }
        }
    }

    public static void store2Local(Connection conn, RsvrBacklogMigToolProcessInfo processInfo, long issueId,
            long issueCustomFieldId, String value, String fieldTypeId) throws SQLException {
//        System.err.println("TRACE: H2IssueCustomFieldDao.store2Local(conn, processInfo, " + issueId + ", "
//                + issueCustomFieldId + ", value:" + value);

        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("SELECT IssueCustomFieldId" //
                + " FROM BacklogIssueCustomField" //
                + " WHERE IssueId = " + issueId + " AND IssueCustomFieldId = " + issueCustomFieldId //
        ))) {
            boolean isNew = false;
            try (RsvrResultSet rset = stmt.executeQuery()) {
                isNew = (rset.next() == false);
            }

            if (isNew) {
                // 想定外
                System.err.println("Unexpected case occured. カスタム項目の値更新において、想定しないルート。 issueId:" + issueId
                        + ", issueCustomFieldId:" + issueCustomFieldId + ", value:" + value);
                processInfo.incrementIns("IssueCustomField");
            } else {
                processInfo.incrementUpd("IssueCustomField");
            }

            // 他の項目は全てUPDATEで処理する。
            try (RsvrPreparedStatement stmtMod = RsvrJdbc
                    .wrap(conn.prepareStatement("UPDATE BacklogIssueCustomField SET " //
                            + "CustomValue=?, FieldTypeId=?" //
                            + " WHERE IssueId = ? AND IssueCustomFieldId = ?"))) {
                stmtMod.setString(value);
                stmtMod.setString(fieldTypeId);
                stmtMod.setLong(issueId);
                stmtMod.setLong(issueCustomFieldId);
                stmtMod.executeUpdateSingleRow();
            }
        }
    }
}

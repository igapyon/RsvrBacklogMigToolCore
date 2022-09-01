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

import com.nulabinc.backlog4j.CustomFieldListItemSetting;
import com.nulabinc.backlog4j.CustomFieldSetting;

import consulting.reservoir.backlog.migtool.core.RsvrBacklogMigToolProcessInfo;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;

/**
 * h2 database に対する `CustomFieldSetting` (エクスポート後) に関する DAO クラス。
 */
public class H2CustomFieldSettingDao {
    /**
     * このDaoが対象とするテーブルを作成。
     * 
     * @param conn データベース接続。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement( //
                "CREATE TABLE IF NOT EXISTS " //
                        + "BacklogCustomFieldSetting (" //
                        + "CustomFieldSettingId BIGINT NOT NULL" //
                        + ",Name VARCHAR(8192)" // 親から継承
                        + ",FieldTypeId VARCHAR(80)" // APIがStringを戻す場合があるため。親から継承。
                        + ",Description VARCHAR(8192)" // 親から継承
                        + ",IsRequired BOOL" //
                        + ",ApplicableIssueType VARCHAR(65535)" //
                        + ",Items VARCHAR(65535)" //
                        + ",PRIMARY KEY(CustomFieldSettingId)" //
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
     * @param conn   データベース接続
     * @param source 格納したいデータ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void store2Local(Connection conn, CustomFieldSetting source,
            RsvrBacklogMigToolProcessInfo processInfo) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("SELECT CustomFieldSettingId" //
                + " FROM BacklogCustomFieldSetting" //
                + " WHERE CustomFieldSettingId = " + source.getId() //
        ))) {
            boolean isNew = false;
            try (RsvrResultSet rset = stmt.executeQuery()) {
                isNew = (rset.next() == false);
            }

            if (isNew) {
                try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(
                        conn.prepareStatement("INSERT INTO BacklogCustomFieldSetting (CustomFieldSettingId) VALUES (?)" //
                        ))) {
                    stmtMod.setLong(source.getId());
                    stmtMod.executeUpdateSingleRow();
                }
                processInfo.incrementIns("CustomFieldSetting");
            } else {
                processInfo.incrementUpd("CustomFieldSetting");
            }

            // 他の項目は全てUPDATEで処理する。
            try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement( //
                    "UPDATE BacklogCustomFieldSetting SET " //
                            + "Name=?, FieldTypeId=?" //
                            + ", Description=?, IsRequired=?, ApplicableIssueType=?, Items=?" //
                            + " WHERE CustomFieldSettingId = ?"))) {
                stmtMod.setString(source.getName());
                stmtMod.setInt(source.getFieldTypeId());
                stmtMod.setString(source.getDescription());
                stmtMod.setBoolean(source.isRequired());

                // 一覧をカンマ区切り文字列化します。
                if (source.getApplicableIssueTypes() == null) {
                    stmtMod.setNull(Types.NVARCHAR);
                } else {
                    String applicableIssueTypeString = "";
                    for (long look : source.getApplicableIssueTypes()) {
                        if (applicableIssueTypeString.length() != 0) {
                            applicableIssueTypeString += ",";
                        }
                        applicableIssueTypeString += look;
                    }
                    stmtMod.setString(applicableIssueTypeString);
                }

                // 一覧をカンマ区切り文字列化します。
                if (source.getItems() == null) {
                    stmtMod.setNull(Types.NVARCHAR);
                } else {
                    String itemString = "";
                    for (CustomFieldListItemSetting look : source.getItems()) {
                        if (itemString.length() != 0) {
                            itemString += ",";
                        }
                        itemString += look.getName();
                    }
                    stmtMod.setString(itemString);
                }

                stmtMod.setLong(source.getId());
                stmtMod.executeUpdateSingleRow();
            }
        }
    }
}

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

import com.nulabinc.backlog4j.SharedFile;

import consulting.reservoir.backlog.migtool.core.RsvrBacklogMigToolProcessInfo;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;

/**
 * h2 database に対する `File` (エクスポート後) に関する DAO クラス。
 */
public class H2FileDao {
    /**
     * このDaoが対象とするテーブルを作成。
     * 
     * @param conn データベース接続。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " //
                + "BacklogFile (" //
                + "FileId BIGINT NOT NULL" //
                + ",Type VARCHAR(80)" //
                + ",Dir VARCHAR(65535)" //
                + ",Name VARCHAR(8192)" //
                + ",Size BIGINT" //
                + ",CreatedUser BIGINT" //
                + ",Created TIMESTAMP" //
                + ",Updated TIMESTAMP" //
                + ",IsImage BOOL" //
                + ",PRIMARY KEY(FileId)" //
                + ")" //
        ))) {
            stmt.executeUpdate();
        }
    }

    /**
     * 与えられた情報を Dao 経由でデータベースに格納します。
     * 
     * @param conn   データベース接続
     * @param source 格納したいデータ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void store2Local(Connection conn, SharedFile source, RsvrBacklogMigToolProcessInfo processInfo)
            throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT FileId FROM BacklogFile WHERE FileId =  " + source.getId() //
                ))) {
            boolean isNew = false;
            try (RsvrResultSet rset = stmt.executeQuery()) {
                isNew = (rset.next() == false);
            }

            if (isNew) {
                // 新規レコード
                try (RsvrPreparedStatement stmtMod = RsvrJdbc
                        .wrap(conn.prepareStatement("INSERT INTO BacklogFile (FileId) VALUES (?)" //
                        ))) {
                    stmtMod.setLong(source.getId());
                    stmtMod.executeUpdateSingleRow();
                }
                processInfo.incrementIns("File");
            } else {
                processInfo.incrementUpd("File");
            }

            // 他の項目は全てUPDATEで処理する。
            try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement("UPDATE BacklogFile SET " //
                    + "Type=?, Name=?, Dir=?, Size=?" //
                    + ", CreatedUser=?, Created=?, Updated=?, IsImage=?" //
                    + " WHERE FileId = ? " //
            ))) {
                stmtMod.setString(source.getType());
                stmtMod.setString(source.getName());
                stmtMod.setString(source.getDir());
                stmtMod.setLong(source.getSize());

                if (source.getCreatedUser() == null) {
                    stmtMod.setNull(Types.BIGINT);
                } else {
                    stmtMod.setLong(source.getCreatedUser().getId());
                    H2UserDao.store2Local(conn, source.getCreatedUser(), processInfo);
                }

                stmtMod.setJavaUtilDate(source.getCreated());
                stmtMod.setJavaUtilDate(source.getUpdated());
                stmtMod.setBoolean(source.isImage());
                stmtMod.setLong(source.getId());
                stmtMod.executeUpdateSingleRow();

                H2UserDao.store2Local(conn, source.getCreatedUser(), processInfo);
            }
        }
    }
}

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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.nulabinc.backlog4j.Attachment;
import com.nulabinc.backlog4j.SharedFile;
import com.nulabinc.backlog4j.Wiki;
import com.nulabinc.backlog4j.WikiTag;

import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.backlog.migtool.core.util.RsvrBacklogMigToolUtil;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;

/**
 * h2 database に対する `Wiki` (エクスポート後) に関する DAO クラス。
 */
public class H2WikiDao {
    /**
     * このDaoが対象とするテーブルを作成。
     * 
     * @param conn データベース接続。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " //
                + "BacklogWiki (" //
                + " WikiId BIGINT NOT NULL" //
                + ",ProjectId BIGINT" //
                + ",Name VARCHAR(8192)" //
                + ",Content VARCHAR(65535)" //
                + ",Tags VARCHAR(8192)" // 文字列でカンマ区切り列挙
                + ",SharedFile BIGINT" // 文字列でカンマ区切り列挙
                + ",CreatedUser BIGINT" //
                + ",Created TIMESTAMP" //
                + ",UpdatedUser BIGINT" //
                + ",Updated TIMESTAMP"//
                + ",PRIMARY KEY(WikiId)" //
                + ")"))) {
            stmt.executeUpdate();
        }
    }

    /**
     * 与えられた情報を Dao 経由でデータベースに格納します。
     * 
     * @param conn        データベース接続
     * @param source      格納したいデータ。
     * @param processInfo 処理情報。
     * @param bklConn     Backlog接続情報。
     * @throws SQLException SQL例外が発生した場合。
     * @throws IOException
     */
    public static void store2Local(Connection conn, Wiki source, RsvrBacklogApiConn bklConn, File baseDir)
            throws SQLException, IOException {
        boolean isNew = false;
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT WikiId FROM BacklogWiki WHERE WikiId=?"))) {
            stmt.setLong(source.getId());
            try (RsvrResultSet rset = stmt.executeQuery()) {
                isNew = (rset.next() == false);
            }
        }
        if (isNew) {
            try (RsvrPreparedStatement stmtMod = RsvrJdbc
                    .wrap(conn.prepareStatement("INSERT INTO BacklogWiki (WikiId) VALUES (?)"))) {
                stmtMod.setLong(source.getId());
                stmtMod.executeUpdateSingleRow();
            }
            bklConn.getProcessInfo().incrementIns("Wiki");
        } else {
            bklConn.getProcessInfo().incrementUpd("Wiki");
        }

        // 他の項目は全てUPDATEで処理する。
        try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement("UPDATE BacklogWiki SET " //
                + " ProjectId=?" //
                + ",Name=?" //
                + ",Content=?" //
                + ",Tags=?" //
                + ",SharedFile=?" //
                + ",CreatedUser=?" //
                + ",Created=?" //
                + ",UpdatedUser=?" //
                + ",Updated=?" //
                + " WHERE WikiId=?"))) {
            stmtMod.setLong(source.getProjectId());
            stmtMod.setString(source.getName());
            stmtMod.setString(source.getContent());

            // 一覧をカンマ区切り文字列化します。
            {
                String buildString = "";
                for (WikiTag look : source.getTags()) {
                    if (buildString.length() != 0) {
                        buildString += ",";
                    }
                    buildString += look.getId();
                }
                if (buildString.length() == 0) {
                    buildString = null;
                }
                stmtMod.setString(buildString);
            }

            // 一覧をカンマ区切り文字列化します。
            {
                for (Attachment look : source.getAttachments()) {
                    H2WikiAttachmentDao.store2Local(conn, look, bklConn.getProcessInfo(), source.getId(), bklConn,
                            baseDir);

                    // API呼び出しインターバルをsleepします。
                    RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
                }
            }

            // 一覧をカンマ区切り文字列化します。
            {
                String buildString = "";
                for (SharedFile look : source.getSharedFiles()) {
                    if (buildString.length() != 0) {
                        buildString += ",";
                    }
                    buildString += look.getId();

                    // ここでダウンロードかな？
                }
                if (buildString.length() == 0) {
                    buildString = null;
                }
                stmtMod.setString(buildString);
            }

            stmtMod.setLong(source.getCreatedUser() == null ? null : source.getCreatedUser().getId());
            if (source.getCreatedUser() != null) {
                H2UserDao.store2Local(conn, source.getCreatedUser(), bklConn.getProcessInfo());
            }
            stmtMod.setJavaUtilDate(source.getCreated());

            stmtMod.setLong(source.getUpdatedUser() == null ? null : source.getUpdatedUser().getId());
            if (source.getUpdatedUser() != null) {
                H2UserDao.store2Local(conn, source.getUpdatedUser(), bklConn.getProcessInfo());
            }
            stmtMod.setJavaUtilDate(source.getUpdated());

            stmtMod.setLong(source.getId());
            stmtMod.executeUpdateSingleRow();
        }
    }
}

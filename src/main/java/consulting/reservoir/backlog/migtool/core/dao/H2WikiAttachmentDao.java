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
import java.sql.Types;

import org.apache.commons.io.FileUtils;

import com.nulabinc.backlog4j.Attachment;
import com.nulabinc.backlog4j.AttachmentData;

import consulting.reservoir.backlog.migtool.core.RsvrBacklogMigToolProcessInfo;
import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.backlog.migtool.core.util.RsvrBacklogMigToolUtil;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;

/**
 * h2 database に対する `Wiki` の添付ファイル (エクスポート後) に関する DAO クラス。
 */
public class H2WikiAttachmentDao {
    /**
     * このDaoが対象とするテーブルを作成。
     * 
     * @param conn データベース接続。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " //
                + "BacklogWikiAttachment (" //
                + "WikiAttachmentId BIGINT NOT NULL" //
                + ",WikiId BIGINT" //
                + ",Name VARCHAR(8192)" //
                + ",Size BIGINT" //
                + ",CreatedUser BIGINT" //
                + ",Created TIMESTAMP" //
                + ",IsImage BOOL" //
                + ",LocalFilename VARCHAR(8192)" // BacklogMigTool が独自に追加した項目。
                + ",PRIMARY KEY(WikiAttachmentId)" //
                + ")" //
        ))) {
            stmt.executeUpdate();
        }
    }

    /**
     * 与えられた情報を Dao 経由でデータベースに格納します。
     * 
     * @param conn    データベース接続
     * @param source  格納したいデータ。
     * @param bklConn Backlog API 呼び出しのための接続情報をまとめたもの。
     * @param baseDir 格納するデータのうち物理ファイルを格納するフォルダ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void store2Local(Connection conn, Attachment source, RsvrBacklogMigToolProcessInfo processInfo,
            long issueId, RsvrBacklogApiConn bklConn, File baseDir) throws SQLException, IOException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement(
                "SELECT WikiAttachmentId FROM BacklogWikiAttachment WHERE WikiAttachmentId =  " + source.getId() //
        ))) {
            boolean isNew = false;
            try (RsvrResultSet rset = stmt.executeQuery()) {
                isNew = (rset.next() == false);
            }

            if (isNew) {
                try (RsvrPreparedStatement stmtMod = RsvrJdbc
                        .wrap(conn.prepareStatement("INSERT INTO BacklogWikiAttachment (WikiAttachmentId) VALUES (?)" //
                        ))) {
                    stmtMod.setLong(source.getId());
                    stmtMod.executeUpdateSingleRow();
                }
                processInfo.incrementIns("WikiAttachment");
            } else {
                processInfo.incrementUpd("WikiAttachment");
            }

            // 他の項目は全てUPDATEで処理する。
            try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement("UPDATE BacklogWikiAttachment SET " //
                    + "WikiId=?, Name=?, Size=?" //
                    + " , CreatedUser=?, Created=?, IsImage=?, LocalFilename=?" //
                    + " WHERE WikiAttachmentId = ?"))) {
                stmtMod.setLong(issueId);
                stmtMod.setString(source.getName());
                stmtMod.setLong(source.getSize());
                if (source.getCreatedUser() == null) {
                    stmtMod.setNull(Types.BIGINT);
                } else {
                    stmtMod.setLong(source.getCreatedUser().getId());
                    H2UserDao.store2Local(conn, source.getCreatedUser(), processInfo);
                }
                stmtMod.setJavaUtilDate(source.getCreated());
                stmtMod.setBoolean(source.isImage());

                String localFilename = null;
                {
                    // 拡張子は調整のうえ物理ファイルの拡張子として設定。
                    localFilename = "" + source.getId();
                    if (source.getName().lastIndexOf('.') > 0) {
                        localFilename += source.getName().substring(source.getName().lastIndexOf('.'));
                    } else {
                        localFilename += ".bin";
                    }
                    stmtMod.setString(localFilename);
                }

                stmtMod.setLong(source.getId());
                stmtMod.executeUpdateSingleRow();

                /////////////////////////////////
                // 課題の添付ファイル本体を Backlog から取得してローカル格納。

                AttachmentData file = bklConn.getClient().downloadWikiAttachment(issueId, source.getId());

                FileUtils.copyToFile(file.getContent(), new File(baseDir, localFilename));

                // API呼び出しインターバルをsleepします。
                RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
            }
        }
    }
}

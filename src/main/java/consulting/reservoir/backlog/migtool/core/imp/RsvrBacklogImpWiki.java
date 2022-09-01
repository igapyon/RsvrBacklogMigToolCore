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
package consulting.reservoir.backlog.migtool.core.imp;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.nulabinc.backlog4j.Attachment;
import com.nulabinc.backlog4j.BacklogException;
import com.nulabinc.backlog4j.ResponseList;
import com.nulabinc.backlog4j.Wiki;
import com.nulabinc.backlog4j.api.option.AddWikiAttachmentParams;
import com.nulabinc.backlog4j.api.option.CreateWikiParams;

import consulting.reservoir.backlog.migtool.core.BMCMessages;
import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.backlog.migtool.core.apicall.retryable.RetryableAddWikiAttachment;
import consulting.reservoir.backlog.migtool.core.apicall.retryable.RetryableCreateWiki;
import consulting.reservoir.backlog.migtool.core.apicall.retryable.RetryablePostAttachment;
import consulting.reservoir.backlog.migtool.core.dao.H2TargetWikiDao;
import consulting.reservoir.backlog.migtool.core.util.RsvrBacklogMigToolUtil;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;
import consulting.reservoir.log.RsvrLog;

/**
 * ローカルの h2 database の `Wiki` 情報をもとに、Backlog API を呼び出してターゲット Backlog
 * プロジェクトにインポートします。
 */
public class RsvrBacklogImpWiki {
    private Connection conn = null;
    private RsvrBacklogApiConn bklConn = null;
    private boolean forceProduction = false;

    public RsvrBacklogImpWiki(Connection conn, RsvrBacklogApiConn bklConn) {
        this.conn = conn;
        this.bklConn = bklConn;
    }

    /**
     * インポート対象を処理します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     * @throws IOException  IO例外が発生した場合。
     */
    public void process(boolean forceProduction) throws SQLException, IOException {
        this.forceProduction = forceProduction;
        if (bklConn.getClient() == null) {
            throw new IllegalArgumentException("Not connected to Backlog. Please login() before process().");
        }

        // 非本番モードの場合に、プロジェクト名が MIGTEST になっているかどうかチェック。
        if (RsvrBacklogMigToolUtil.checkTargetProjectNameForNonProductionModeMIGTEST(conn, bklConn,
                forceProduction) == false) {
            return;
        }

        if (forceProduction) {
            // [BMC5901] -forceproduction と -forceimport とを同時に指定することはできません。
            RsvrLog.error(BMCMessages.BMC5901);
            throw new IOException(BMCMessages.BMC5901);
        }

        // ローカルからインポートします。
        impFromLocal();
    }

    /**
     * Wiki情報をローカルからインポートします。
     * 
     * @param conn    入力となるデータベース接続。
     * @param bklConn Backlog 接続。
     * @throws SQLException
     */
    private void impFromLocal() throws SQLException, IOException {
        // API呼び出しインターバルをsleepします。
        RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());

        // target
        H2TargetWikiDao.createTable(conn);

        // そもそもの現状のWiki状態を取得。
        {
            ResponseList<Wiki> respList = bklConn.getClient().getWikis(bklConn.getProjectId());
            for (Wiki look : respList) {
                H2TargetWikiDao.store2Local(conn, look, bklConn);
            }

            RsvrLog.info("Target Export (Prepare): " //
                    + bklConn.getProcessInfo().getDisplayString("TargetWiki"));

            // API呼び出しインターバルをsleepします。
            RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
        }

        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement(
                "SELECT WikiId, Name, Content, Tags, SharedFile, CreatedUser, Created, UpdatedUser, Updated" //
                        + " FROM BacklogWiki" //
                        + " ORDER BY WikiId"))) {
            try (RsvrResultSet rset = stmt.executeQuery()) {
                for (; rset.next();) {
                    Long origWikiId = rset.getLong();
                    String name = rset.getString();
                    String content = rset.getString();
                    String tags = rset.getString();
                    String sharedFile = rset.getString();
                    Long origCreatedUser = rset.getLong();
                    Date created = rset.getTime();
                    Long origUpdatedUser = rset.getLong();
                    Date updated = rset.getTime();

                    // 該当するWikiが既に存在するかどうかチェック。
                    try (RsvrPreparedStatement stmt2 = RsvrJdbc.wrap(conn.prepareStatement("SELECT WikiId" //
                            + " FROM BacklogTargetWiki" //
                            + " WHERE Name=?"))) {
                        stmt2.setString(name);
                        try (RsvrResultSet rset2 = stmt2.executeQuery()) {
                            if (rset2.next()) {
                                RsvrLog.trace("Wiki: すでにあるWikiです。スキップします: " + name);
                            } else {
                                RsvrLog.trace("Wiki: 新しいWikiを追加します: " + name);
                                CreateWikiParams params = new CreateWikiParams(bklConn.getProjectId(), name, content);

                                RetryableCreateWiki apicallout = new RetryableCreateWiki(params);
                                apicallout.execute(bklConn);
                                final Wiki newWiki = apicallout.getResult();

                                bklConn.getProcessInfo().incrementIns("TargetWiki");
                                RsvrLog.trace("Wiki: 新規Wiki: " + newWiki.getId());

                                // API呼び出しインターバルをsleepします。
                                RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());

                                // 添付ファイルもここで処理。
                                processAttachment(origWikiId, newWiki.getId());

                                // [BMC5801] Import: Wiki: created.
                                RsvrLog.info(BMCMessages.BMC5801 + ": " + newWiki.getName());

                                // API呼び出しインターバルをsleepします。
                                RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
                            }
                        }
                    }
                }
            }
        }

    }

    private void processAttachment(long origWikiId, long newWikiId) throws SQLException, IOException {
        final List<Long> attachmentIds = new ArrayList<Long>();
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("SELECT" //
                + " Name, LocalFilename" //
                + " FROM BacklogWikiAttachment" //
                + " WHERE WikiId=?"))) {
            stmt.setLong(origWikiId);
            try (RsvrResultSet rset = stmt.executeQuery()) {
                for (; rset.next();) {
                    String name = rset.getString();
                    String localFilename = rset.getString();
                    File localFile = new File(bklConn.getToolConf().getDirExpWikiAttachment(), localFilename);
                    if (localFile.exists() == false) {
                        throw new IOException(
                                "Unexpected Local attachment file not exist: " + localFile.getCanonicalPath());
                    }

                    RetryablePostAttachment apicallout = new RetryablePostAttachment(name, localFile);
                    apicallout.execute(bklConn);
                    Attachment attachment = apicallout.getResult();

                    // 新規のWiki添付ファイル。
                    attachmentIds.add(attachment.getId());

                    // API呼び出しインターバルをsleepします。
                    RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
                }
            }
        }

        if (attachmentIds.size() > 0) {
            RsvrLog.trace("Wiki: addWikiAttachmentWithRetry");
            AddWikiAttachmentParams params = new AddWikiAttachmentParams(newWikiId, attachmentIds);

            System.err.println("attachmentId[]: " + String.valueOf(attachmentIds));

            try {
                RetryableAddWikiAttachment apicallout = new RetryableAddWikiAttachment(params);
                apicallout.execute(bklConn);
            } catch (BacklogException ex) {
                if (ex.getMessage().contains("err.wiki.attachment.operation")) {
                    System.err.println("Wikiへのファイル添付に失敗. フリープランの場合これが発生する. ファイルを添付せずに次に進む。:" + ex.getMessage());
                }
            }

            // API呼び出しインターバルをsleepします。
            RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
        }
    }
}

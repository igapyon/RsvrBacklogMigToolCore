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
package consulting.reservoir.backlog.migtool.core.exp;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.nulabinc.backlog4j.Attachment;
import com.nulabinc.backlog4j.ResponseList;

import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.backlog.migtool.core.dao.H2IssueAttachmentDao;
import consulting.reservoir.backlog.migtool.core.util.RsvrBacklogMigToolUtil;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;

/**
 * Backlog API を呼び出して `Issue` 添付ファイル情報を取得して、ローカルの h2 database のテーブルにエクスポートします。
 */
public class RsvrBacklogExpIssueAttachment {
    private Connection conn = null;
    private RsvrBacklogApiConn bklConn = null;

    public RsvrBacklogExpIssueAttachment(Connection conn, RsvrBacklogApiConn bklConn) {
        this.conn = conn;
        this.bklConn = bklConn;
    }

    /**
     * エクスポート対象を処理します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     * @throws IOException  IO例外が発生した場合。
     */
    public void process(File baseDir) throws SQLException, IOException {
        if (bklConn.getClient() == null) {
            throw new IllegalArgumentException("Not connected to Backlog. Please login() before process().");
        }

        // h2 に Issue添付ファイルテーブルを作成します。
        H2IssueAttachmentDao.createTable(conn);

        // Issueコメントをローカルに保管します。
        toLocal(baseDir);
    }

    /**
     * Issueコメントをローカルに格納します。
     * 
     * @param bklConn Backlog 接続。
     * @throws SQLException
     * @throws IOException
     */
    private void toLocal(File baseDir) throws SQLException, IOException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("SELECT" //
                + " IssueId, IssueKey, KeyId" //
                + " FROM BacklogIssue" //
                + " ORDER BY KeyId"))) { //

            try (RsvrResultSet rset = stmt.executeQuery()) {
                for (; rset.next();) {
                    long issueId = rset.getLong();
                    String issueKey = rset.getString();
                    long keyId = rset.getLong();
                    ResponseList<Attachment> respList = bklConn.getClient().getIssueAttachments(issueId);
                    for (Attachment look : respList) {
                        H2IssueAttachmentDao.store2Local(conn, look, bklConn.getProcessInfo(), issueId, bklConn,
                                baseDir);
                    }

                    // API呼び出しインターバルをsleepします。
                    RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
                }
            }
        }
    }
}

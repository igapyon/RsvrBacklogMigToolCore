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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.nulabinc.backlog4j.IssueComment;
import com.nulabinc.backlog4j.ResponseList;
import com.nulabinc.backlog4j.api.option.QueryParams;
import com.nulabinc.backlog4j.api.option.QueryParams.Order;

import consulting.reservoir.backlog.migtool.core.BMCMessages;
import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.backlog.migtool.core.dao.H2IssueCommentChangeLogDao;
import consulting.reservoir.backlog.migtool.core.dao.H2IssueCommentDao;
import consulting.reservoir.backlog.migtool.core.util.RsvrBacklogMigToolUtil;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;
import consulting.reservoir.log.RsvrLog;

/**
 * Backlog API を呼び出して `Issue` Comment 情報を取得して、ローカルの h2 database のテーブルにエクスポートします。
 */
public class RsvrBacklogExpIssueComment {
    private Connection conn = null;
    private RsvrBacklogApiConn bklConn = null;

    public RsvrBacklogExpIssueComment(Connection conn, RsvrBacklogApiConn bklConn) {
        this.conn = conn;
        this.bklConn = bklConn;
    }

    /**
     * エクスポート対象を処理します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     * @throws IOException  IO例外が発生した場合。
     */
    public void process() throws SQLException, IOException {
        if (bklConn.getClient() == null) {
            throw new IllegalArgumentException("Not connected to Backlog. Please login() before process().");
        }

        // h2 に IssueCommentテーブルを作成します。
        H2IssueCommentDao.createTable(conn);

        H2IssueCommentChangeLogDao.createTable(conn);

        // Issueコメントをローカルに保管します。
        toLocal();
    }

    /**
     * Issueコメントをローカルに格納します。
     * 
     * @param bklConn Backlog 接続。
     * @throws SQLException
     */
    private void toLocal() throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("SELECT" //
                + " IssueId, IssueKey, KeyId" //
                + " FROM BacklogIssue" //
                + " ORDER BY KeyId"))) { //

            try (RsvrResultSet rset = stmt.executeQuery()) {
                for (; rset.next();) {
                    long issueId = rset.getLong();
                    String issueKey = rset.getString();
                    long keyId = rset.getLong();

                    final int commentCount = bklConn.getClient().getIssueCommentCount(issueId);
                    // API呼び出しインターバルをsleepします。
                    RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());

                    int commentCountActual = 0;

                    // ウィンドウを動かしながらデータを取得するための仕組み。
                    long pastMaxId = 0;
                    for (;;) {
                        QueryParams qp = new QueryParams();
                        qp.count(100); // 100にしたい。
                        qp.order(Order.Asc);
                        qp.minId(pastMaxId + 1);

                        ResponseList<IssueComment> respList = bklConn.getClient().getIssueComments(issueId, qp);
                        // API呼び出しインターバルをsleepします。
                        RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());

                        if (respList.size() == 0) {
                            // データの終端に到達。終わります。
                            break;
                        }
                        for (IssueComment look : respList) {
                            H2IssueCommentDao.store2Local(conn, look, bklConn.getProcessInfo(), issueId);
                            // 検索ウィンドウを次に進める。
                            pastMaxId = look.getId();
                        }
                        // 実際のコメント数をカウント。
                        commentCountActual += respList.size();
                    }
                    if (commentCount != commentCountActual) {
                        // [BMC1101] 期待したコメント数と、実際に取得できたコメント数とが異なる
                        RsvrLog.error(BMCMessages.BMC1101 + ": 期待:" + commentCount + ", 実際:" + commentCountActual);
                    }
                }
            }
        }
    }
}

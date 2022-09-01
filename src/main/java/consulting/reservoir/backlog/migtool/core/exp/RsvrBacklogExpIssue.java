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
import java.util.ArrayList;
import java.util.List;

import com.nulabinc.backlog4j.Issue;
import com.nulabinc.backlog4j.ResponseList;
import com.nulabinc.backlog4j.api.option.GetIssuesParams;
import com.nulabinc.backlog4j.api.option.GetIssuesParams.Order;

import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.backlog.migtool.core.dao.H2IssueCustomFieldDao;
import consulting.reservoir.backlog.migtool.core.dao.H2IssueDao;
import consulting.reservoir.backlog.migtool.core.util.RsvrBacklogMigToolUtil;

/**
 * Backlog API を呼び出して `Issue` 情報を取得して、ローカルの h2 database のテーブルにエクスポートします。
 */
public class RsvrBacklogExpIssue {
    private Connection conn = null;
    private RsvrBacklogApiConn bklConn = null;

    public RsvrBacklogExpIssue(Connection conn, RsvrBacklogApiConn bklConn) {
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

        // h2 にテーブルを作成します。
        H2IssueDao.createTable(conn);
        H2IssueCustomFieldDao.createTable(conn);

        // Issueをローカルに保管します。
        toLocal();
    }

    /**
     * Issue情報をローカルに格納します。
     * 
     * @param bklConn Backlog 接続。
     * @throws SQLException
     */
    private void toLocal() throws SQLException {
        List<Long> projectIds = new ArrayList<Long>();
        projectIds.add(bklConn.getProjectId());

        int retryCount = 2;
        long offset = 0;
        for (;;) {
            // System.err.println("trace: offset: " + offset);
            GetIssuesParams params = new GetIssuesParams(projectIds);
            params.offset(offset);
            params.order(Order.Asc);
            params.count(100);
            ResponseList<Issue> issueList = bklConn.getClient().getIssues(params);
            for (Issue lookup : issueList) {
                H2IssueDao.store2Local(conn, lookup, bklConn.getProcessInfo(), bklConn);
            }
            if (issueList.size() == 0) {
                // System.err.println("trace: 取得結果が0件.");
                retryCount--;
                if (retryCount == 0) {
                    // 失敗が続くので、終端に到達したと判断。
                    break;
                }
            }
            offset += issueList.size();

            // API呼び出しインターバルをsleepします。
            RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
        }
    }
}

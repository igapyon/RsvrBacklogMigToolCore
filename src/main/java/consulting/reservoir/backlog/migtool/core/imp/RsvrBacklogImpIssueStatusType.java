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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.nulabinc.backlog4j.Project;
import com.nulabinc.backlog4j.ResponseList;
import com.nulabinc.backlog4j.Status;
import com.nulabinc.backlog4j.api.option.AddStatusParams;

import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.backlog.migtool.core.apicall.retryable.RetryableAddStatus;
import consulting.reservoir.backlog.migtool.core.dao.H2TargetIssueStatusTypeDao;
import consulting.reservoir.backlog.migtool.core.util.RsvrBacklogMigToolUtil;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;
import consulting.reservoir.log.RsvrLog;

/**
 * ローカルの h2 database の `IssueStatusType` 情報をもとに、Backlog API を呼び出してターゲット Backlog
 * プロジェクトにインポートします。
 */
public class RsvrBacklogImpIssueStatusType {
    private Connection conn = null;
    private RsvrBacklogApiConn bklConn = null;

    public RsvrBacklogImpIssueStatusType(Connection conn, RsvrBacklogApiConn bklConn) {
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
        if (bklConn.getClient() == null) {
            throw new IllegalArgumentException("Not connected to Backlog. Please login() before process().");
        }

        // 非本番モードの場合に、プロジェクト名が MIGTEST になっているかどうかチェック。
        if (RsvrBacklogMigToolUtil.checkTargetProjectNameForNonProductionModeMIGTEST(conn, bklConn,
                forceProduction) == false) {
            return;
        }

        H2TargetIssueStatusTypeDao.createTable(conn);

        // ターゲットの最新情報をゲット。
        expFromTargetToLocal();

        // ローカルからインポートします。
        impFromLocal();

        // ターゲットの最新情報をゲット。
        expFromTargetToLocal();
    }

    /**
     * 情報をターゲットBacklogに格納します。
     * 
     * @param conn    入力となるデータベース接続。
     * @param bklConn Backlog 接続。
     * @throws SQLException
     */
    private void impFromLocal() throws SQLException, IOException {
        // API呼び出しインターバルをsleepします。
        RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());

        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("SELECT" //
                + " IssueStatusTypeId, Name" //
                + " FROM BacklogIssueStatusType" //
                + " ORDER BY IssueStatusTypeId"))) {
            try (RsvrResultSet rset = stmt.executeQuery()) {
                for (; rset.next();) {
                    Long issueStatusTypeId = rset.getLong();
                    String name = rset.getString();

                    if (issueStatusTypeId < 10) {
                        // Backlog天然物
                        continue;
                    }

                    // 今から登録しようとしているものがすでに登録済みかどうか確認。
                    try (RsvrPreparedStatement stmt2 = RsvrJdbc.wrap(conn.prepareStatement(
                            "SELECT IssueStatusTypeId FROM BacklogTargetIssueStatusType WHERE Name=?"))) {
                        stmt2.setString(name);
                        try (RsvrResultSet rset2 = stmt2.executeQuery()) {
                            if (rset2.next()) {
                                // 既に登録済み。
                                RsvrLog.trace("すでに登録済みのStatus: " + name);
                            } else {
                                // 新規もの。作成します。
                                RsvrLog.trace("新規にStatusを追加: " + name);
                                AddStatusParams params = new AddStatusParams(bklConn.getProjectId(), name,
                                        Project.CustomStatusColor.Color1);
                                RetryableAddStatus apicallout = new RetryableAddStatus(params);
                                apicallout.execute(bklConn);
                                apicallout.getResult();
                            }
                        }
                    }
                }
            }
        }
    }

    private void expFromTargetToLocal() throws SQLException, IOException {
        // API呼び出しインターバルをsleepします。
        RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());

        ResponseList<Status> respList = bklConn.getClient().getStatuses(bklConn.getProjectId());
        for (Status look : respList) {
            H2TargetIssueStatusTypeDao.store2Local(conn, look, bklConn);
        }

        // API呼び出しインターバルをsleepします。
        RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
    }
}

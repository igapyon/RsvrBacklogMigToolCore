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

import com.nulabinc.backlog4j.api.option.UpdateIssueParams;

import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.backlog.migtool.core.apicall.retryable.RetryableUpdateIssue;
import consulting.reservoir.backlog.migtool.core.util.RsvrBacklogMigToolUtil;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;
import consulting.reservoir.log.RsvrLog;

/**
 * ローカルの h2 database の `TargetIssue` 情報をもとに、Backlog API を呼び出して親子関係を設定します。。
 */
public class RsvrBacklogImpIssueParent {
    private Connection conn = null;
    private RsvrBacklogApiConn bklConn = null;
    private boolean forceProduction = false;

    public RsvrBacklogImpIssueParent(Connection conn, RsvrBacklogApiConn bklConn) {
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

        // ローカルからインポートします。
        impFromLocal();
    }

    /**
     * Issue情報をローカルに格納します。
     * 
     * @param conn    入力となるデータベース接続。
     * @param bklConn Backlog 接続。
     * @throws SQLException
     */
    private void impFromLocal() throws SQLException, IOException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT bti.TargetIssueId, btpi.TargetIssueId, bti.KeyId, btpi.KeyId"//
                        + " FROM BacklogIssue m" //
                        + " LEFT OUTER JOIN BacklogTargetIssue bti ON m.IssueId = bti.SourceIssueId" //
                        + " LEFT OUTER JOIN BacklogTargetIssue btpi ON m.ParentIssueId = btpi.SourceIssueId" //
                        + " WHERE m.ParentIssueId<>0" //
                        + " ORDER BY m.IssueId"))) {
            try (RsvrResultSet rset = stmt.executeQuery()) {
                for (; rset.next();) {
                    Long targetChildIssueId = rset.getLong();
                    Long targetParentIssueId = rset.getLong();
                    Long targetChildKeyId = rset.getLong();
                    Long targetParentKeyId = rset.getLong();

                    UpdateIssueParams updateIssueParams = new UpdateIssueParams(targetChildIssueId);
                    updateIssueParams.parentIssueId(targetParentIssueId);
                    updateIssueParams.comment("子課題:" + targetChildKeyId + " に親課題:" + targetParentKeyId + " を設定。");
                    RsvrLog.trace("子プロジェクト:" + targetChildKeyId + " に親プロジェクト:" + targetParentKeyId + " をセット。");

                    RetryableUpdateIssue apicallout = new RetryableUpdateIssue(updateIssueParams);
                    apicallout.execute(bklConn);
                    apicallout.getResult();

                    // API呼び出しインターバルをsleepします。
                    RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
                }
            }
        }
    }
}

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

import com.nulabinc.backlog4j.BacklogAPIException;
import com.nulabinc.backlog4j.Priority;
import com.nulabinc.backlog4j.Project;
import com.nulabinc.backlog4j.Resolution;
import com.nulabinc.backlog4j.ResponseList;
import com.nulabinc.backlog4j.Status;
import com.nulabinc.backlog4j.User;

import consulting.reservoir.backlog.migtool.core.BMCMessages;
import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.backlog.migtool.core.dao.H2TargetIssuePriorityTypeDao;
import consulting.reservoir.backlog.migtool.core.dao.H2TargetIssueResolutionTypeDao;
import consulting.reservoir.backlog.migtool.core.dao.H2TargetIssueStatusTypeDao;
import consulting.reservoir.backlog.migtool.core.dao.H2TargetIssueTypeDao;
import consulting.reservoir.backlog.migtool.core.dao.H2TargetProjectDao;
import consulting.reservoir.backlog.migtool.core.dao.H2TargetUserDao;
import consulting.reservoir.backlog.migtool.core.util.RsvrBacklogMigToolUtil;
import consulting.reservoir.log.RsvrLog;

/**
 * Backlog インポートに先立ち、接続先Backlogから Project, User, IssueType, IssuePriority,
 * IssueResolution, IssueStatus 情報を収集します。
 */
public class RsvrBacklogImpPrepare {
    private Connection conn = null;
    private RsvrBacklogApiConn bklConn = null;

    public RsvrBacklogImpPrepare(Connection conn, RsvrBacklogApiConn bklConn) {
        this.conn = conn;
        this.bklConn = bklConn;
    }

    /**
     * 対象を処理します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     * @throws IOException  IO例外が発生した場合。
     */
    public void process(boolean forceProduction) throws SQLException, IOException {
        if (bklConn.getClient() == null) {
            throw new IllegalArgumentException("Not connected to Backlog. Please login() before process().");
        }

        try {
            // Phase1 におけるチェックはDBに到達せずに確認したい。個別のコードで実現する。
            System.err.println("処理に先立ち、ターゲット Backlog の Project情報を取得.");
            Project proj = bklConn.getClient().getProject(bklConn.getProjectId());
            RsvrLog.info("Target Project Name: " + proj.getName() + " (" + proj.getId() + ")");

            if (forceProduction == false && proj.getProjectKey().startsWith("MIGTEST") == false) {
                // [BMC5204] Import: Warn: 非本番モードであるのに、ProjectKey が MIGTEST
                // から始まる名前ではありません。非本番モードでは ProjectKey は MIGTEST で開始するようにしてください. 処理スキップします.";
                RsvrLog.error(BMCMessages.BMC5204 + ": [" + proj.getProjectKey() + "] " + proj.getName());
                throw new IOException(BMCMessages.BMC5204);
            }

            // API呼び出しインターバルをsleepします。
            RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
        } catch (BacklogAPIException ex) {
            throw new IOException("Login Failed:" + ex.toString(), ex);
        }

        // MIGTEST のチェックが終わって初めて h2 database テーブルを作成する。

        // h2 にテーブルを作成します。
        H2TargetProjectDao.createTable(conn);
        H2TargetUserDao.createTable(conn);
        H2TargetIssueTypeDao.createTable(conn);
        H2TargetIssuePriorityTypeDao.createTable(conn);
        H2TargetIssueResolutionTypeDao.createTable(conn);
        H2TargetIssueStatusTypeDao.createTable(conn);

        // 情報を取得します。
        toLocal(forceProduction);
    }

    /**
     * 情報をローカルに格納します。
     * 
     * @param conn    入力となるデータベース接続。
     * @param bklConn Backlog 接続。
     * @throws SQLException
     */
    private void toLocal(boolean forceProduction) throws SQLException, IOException {
        try {
            Project proj = bklConn.getClient().getProject(bklConn.getProjectId());
            H2TargetProjectDao.store2Local(conn, proj, bklConn.getProcessInfo());
            RsvrLog.info("Target Project Name: " + proj.getName() + " (" + proj.getId() + ")");

            RsvrLog.info("Target Export (Prepare): " //
                    + bklConn.getProcessInfo().getDisplayString("TargetProject"));

            // API呼び出しインターバルをsleepします。
            RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
        } catch (BacklogAPIException ex) {
            throw new IOException("Login Failed:" + ex.toString(), ex);
        }

        {
            ResponseList<User> userList = bklConn.getClient().getProjectUsers(bklConn.getProjectId());
            for (User look : userList) {
                H2TargetUserDao.store2Local(conn, look, bklConn.getProcessInfo());
            }

            RsvrLog.info("Target Export (Prepare): " //
                    + bklConn.getProcessInfo().getDisplayString("TargetUser"));

            // API呼び出しインターバルをsleepします。
            RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
        }

        {
            ResponseList<Priority> priorities = bklConn.getClient().getPriorities();
            for (Priority look : priorities) {
                H2TargetIssuePriorityTypeDao.store2Local(conn, look, bklConn.getProcessInfo(), bklConn);
            }

            RsvrLog.info("Target Export (Prepare): " //
                    + bklConn.getProcessInfo().getDisplayString("TargetIssuePriorityType"));

            // API呼び出しインターバルをsleepします。
            RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
        }

        {
            ResponseList<Resolution> resolutionList = bklConn.getClient().getResolutions();
            for (Resolution look : resolutionList) {
                H2TargetIssueResolutionTypeDao.store2Local(conn, look, bklConn.getProcessInfo(), bklConn);
            }

            RsvrLog.info("Target Export (Prepare): " //
                    + bklConn.getProcessInfo().getDisplayString("TargetIssueResolutionType"));

            // API呼び出しインターバルをsleepします。
            RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
        }

        {
            ResponseList<Status> statusList = bklConn.getClient().getStatuses(bklConn.getProjectId());
            for (Status look : statusList) {
                H2TargetIssueStatusTypeDao.store2Local(conn, look, bklConn);
            }

            RsvrLog.info("Target Export (Prepare): " //
                    + bklConn.getProcessInfo().getDisplayString("TargetIssueStatusType"));

            // API呼び出しインターバルをsleepします。
            RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
        }
    }
}

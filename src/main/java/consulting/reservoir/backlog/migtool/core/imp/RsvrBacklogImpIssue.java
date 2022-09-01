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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.nulabinc.backlog4j.Attachment;
import com.nulabinc.backlog4j.BacklogAPIException;
import com.nulabinc.backlog4j.Issue;
import com.nulabinc.backlog4j.Issue.PriorityType;
import com.nulabinc.backlog4j.Issue.ResolutionType;
import com.nulabinc.backlog4j.ResponseList;
import com.nulabinc.backlog4j.api.option.CreateIssueParams;
import com.nulabinc.backlog4j.api.option.GetIssuesParams;
import com.nulabinc.backlog4j.api.option.GetIssuesParams.Order;

import consulting.reservoir.backlog.migtool.core.BMCMessages;
import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.backlog.migtool.core.apicall.retryable.RetryableAddIssueComment;
import consulting.reservoir.backlog.migtool.core.apicall.retryable.RetryableCreateIssue;
import consulting.reservoir.backlog.migtool.core.apicall.retryable.RetryablePostAttachment;
import consulting.reservoir.backlog.migtool.core.apicall.retryable.RetryableUpdateIssue;
import consulting.reservoir.backlog.migtool.core.dao.H2MappingUserDao;
import consulting.reservoir.backlog.migtool.core.dao.H2TargetCategoryDao;
import consulting.reservoir.backlog.migtool.core.dao.H2TargetIssueDao;
import consulting.reservoir.backlog.migtool.core.dao.H2TargetIssuePriorityTypeDao;
import consulting.reservoir.backlog.migtool.core.dao.H2TargetIssueStatusTypeDao;
import consulting.reservoir.backlog.migtool.core.dao.H2TargetIssueTypeDao;
import consulting.reservoir.backlog.migtool.core.dao.H2TargetMilestoneDao;
import consulting.reservoir.backlog.migtool.core.dao.H2TargetVersionDao;
import consulting.reservoir.backlog.migtool.core.util.RsvrBacklogMigToolUtil;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;
import consulting.reservoir.log.RsvrLog;

/**
 * ローカルの h2 database の `Issue` 情報をもとに、Backlog API を呼び出してターゲット Backlog
 * プロジェクトにインポートします。
 */
public class RsvrBacklogImpIssue {
    private Connection conn = null;
    private RsvrBacklogApiConn bklConn = null;
    private boolean forceProduction = false;
    private boolean forceImport = false;
    private int skipImportIssueCount = 0;

    public RsvrBacklogImpIssue(Connection conn, RsvrBacklogApiConn bklConn) {
        this.conn = conn;
        this.bklConn = bklConn;
    }

    /**
     * インポート対象を処理します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     * @throws IOException  IO例外が発生した場合。
     */
    public void process(boolean forceProduction, boolean forceImport, int skipImportIssueCount)
            throws SQLException, IOException {
        this.forceProduction = forceProduction;
        this.forceImport = forceImport;
        this.skipImportIssueCount = skipImportIssueCount;
        if (bklConn.getClient() == null) {
            throw new IllegalArgumentException("Not connected to Backlog. Please login() before process().");
        }

        // 非本番モードの場合に、プロジェクト名が MIGTEST になっているかどうかチェック。
        if (RsvrBacklogMigToolUtil.checkTargetProjectNameForNonProductionModeMIGTEST(conn, bklConn,
                forceProduction) == false) {
            return;
        }

        if (forceProduction && forceImport) {
            // [BMC5901] -forceproduction と -forceimport とを同時に指定することはできません。
            RsvrLog.error(BMCMessages.BMC5901);
            throw new IOException(BMCMessages.BMC5901);
        }

        H2TargetIssueDao.createTable(conn);

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

        {
            RsvrLog.trace("Import: Import先のIssueが空であることを確認.");

            List<Long> projectIds = new ArrayList<Long>();
            projectIds.add(bklConn.getProjectId());
            GetIssuesParams params = new GetIssuesParams(projectIds);
            params.order(Order.Asc);
            params.count(100);
            ResponseList<Issue> issueList = bklConn.getClient().getIssues(params);
            if (issueList.size() > 0) {
                if (forceImport == false) {
                    // [BMC5102] Import: Issue: Import cannot proceed because issue(s) already
                    // exists in the project. Processing will be aborted. issue count:
                    RsvrLog.error(BMCMessages.BMC5102 + issueList.size());
                    throw new IOException(BMCMessages.BMC5102 + issueList.size());
                } else {
                    // [BMC5103] Import: Issue: Warn: Import warn because issue(s) already exists in
                    // the project. Processing will be continued (-forceimp). issue count:
                    RsvrLog.warn(BMCMessages.BMC5103 + issueList.size());
                }
            }
        }

        // API呼び出しインターバルをsleepします。
        RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());

        int skipImportIssueCountLeft = skipImportIssueCount;
        // 前回の KeyId
        long lastKeyId = 0;

        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement(
                "SELECT IssueId, KeyId, Summary, IssueType, Priority, Description, Resolution, Status, Assignee" //
                        + ", Category, Version, Milestone, StartDate, DueDate, EstimatedHours, ActualHours" //
                        + ", ParentIssueId, CreatedUser, Created, UpdatedUser, Updated" //
                        + " FROM BacklogIssue" //
                        + " ORDER BY KeyId" //
        ))) {
            try (RsvrResultSet rset = stmt.executeQuery()) {
                for (; rset.next();) {
                    Long origIssueId = rset.getLong();
                    Long keyId = rset.getLong();
                    String summary = rset.getString();
                    String issueType = rset.getString();
                    String priority = rset.getString();
                    String description = rset.getString();
                    String resolution = rset.getString();
                    String status = rset.getString();
                    Long assignee = rset.getLong(); // FIXME おりじなるのアサイン
                    String category = rset.getString();
                    String version = rset.getString();
                    String milestone = rset.getString();
                    String startDate = rset.getString();
                    String dueDate = rset.getString();
                    BigDecimal estimatedHours = rset.getBigDecimal();
                    BigDecimal actualHours = rset.getBigDecimal();
                    Long origParentIssueId = rset.getLong();
                    Long origCreatedUser = rset.getLong();
                    Date created = rset.getTime();
                    Long origUpdatedUser = rset.getLong();
                    Date updated = rset.getTime();

                    // 与えられた文字列をもとに IssueTypeId を取得。
                    long issueTypeId = H2TargetIssueTypeDao.getIssueTypeIdByName(conn, issueType);

                    int issuePriorityTypeId = (int) H2TargetIssuePriorityTypeDao.getIssuePriorityTypeIdByName(conn,
                            priority);

                    if (keyId - lastKeyId == 1) {
                        // 順番どおりだった。
                    } else {
                        // 飛んだ数だけ「削除」で埋めます。
                        for (long fillingCount = keyId - lastKeyId - 1; fillingCount > 0; fillingCount--) {
                            final String deletedMessage = BMCMessages.MBC5107 + ": " + (keyId - fillingCount);
                            final CreateIssueParams param = new CreateIssueParams(bklConn.getProjectId(),
                                    deletedMessage, issueTypeId, Issue.PriorityType.Low);
                            param.description(deletedMessage);

                            RetryableCreateIssue apicallout = new RetryableCreateIssue(param);
                            apicallout.execute(bklConn);
                            apicallout.getResult();
                        }
                    }
                    // 前回のIDとして記憶
                    lastKeyId = keyId;

                    if (skipImportIssueCountLeft-- > 0) {
                        RsvrLog.info("[-skipimportissuecount] Skipping issues: (" + keyId + ") " + summary);
                        continue;
                    }

                    CreateIssueParams param = new CreateIssueParams(bklConn.getProjectId(),
                            (summary == null ? "" : summary), issueTypeId,
                            Issue.PriorityType.valueOf((int) issuePriorityTypeId));

                    // parentIssueId
                    if (origParentIssueId != null && origParentIssueId != 0) {
                        RsvrLog.trace("Issue作成のparentIssueId指定があるがこれは次の処理パスで対応");
                    }

                    // description
                    if (description != null) {
                        param.description(description);
                    }

                    // startDate
                    if (startDate != null) {
                        param.startDate(startDate);
                    }

                    // dueDate
                    if (dueDate != null) {
                        param.dueDate(dueDate);
                    }

                    // estimatedHours
                    if (estimatedHours != null) {
                        param.estimatedHours(estimatedHours);
                    }

                    // actualHours
                    if (actualHours != null) {
                        param.actualHours(actualHours);
                    }

                    // categoryIds
                    if (category != null && category.length() > 0) {
                        param.categoryIds(H2TargetCategoryDao.getCategoryIdListByNames(conn, category));
                    }

                    // versionIds
                    if (version != null && version.length() > 0) {
                        param.versionIds(H2TargetVersionDao.getVersionIdListByNames(conn, version));
                    }

                    // milestoneIds
                    if (milestone != null && milestone.length() > 0) {
                        param.milestoneIds(H2TargetMilestoneDao.getMilestoneIdListByNames(conn, milestone));
                    }

                    // assigneeId
                    if (assignee != null) {
                        param.assigneeId(H2MappingUserDao.getTargetUserIdBySourceUserId(conn, assignee));
                    }

                    // notifiedUserIds
                    // IssueCommentにて設定される。

                    // attachmentIds
                    // IssueCommentにて設定される。

                    // textCustomField など、以降はカスタムフィールド関連
                    // TODO カスタム系は手付かず。TBD

                    RetryableCreateIssue apicallout = new RetryableCreateIssue(param);
                    apicallout.execute(bklConn);
                    final Issue newIssue = apicallout.getResult();

                    bklConn.getProcessInfo().incrementIns("TargetIssue");

                    // 新旧の IssueIdの対応を記録。
                    H2TargetIssueDao.store2Local(conn, newIssue, origIssueId, bklConn);

                    // [BMC5101] Import: Issue: created.
                    RsvrLog.trace(BMCMessages.BMC5101 + ": [" + newIssue.getIssueKey() + "] " + newIssue.getSummary());

                    // API呼び出しインターバルをsleepします。
                    RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());

                    // IssueCommentを反映
                    processIssueComment(origIssueId, newIssue);

                }
            }
        }

    }

    private void processIssueComment(long origIssueId, Issue newIssue) throws SQLException, IOException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("SELECT" //
                + " IssueCommentId, Content, uc.Name AS CreatedUserName, Created, Updated" //
                + " FROM BacklogIssueComment" //
                + " LEFT OUTER JOIN BacklogUser AS uc ON BacklogIssueComment.CreatedUser = uc.UserId" // TODO FIXME
                                                                                                      // ターゲットUserに変更
                + " WHERE IssueId = ?" //
                + " ORDER BY IssueCommentId"))) { //
            stmt.setLong(origIssueId);
            try (RsvrResultSet rset = stmt.executeQuery()) {
                for (; rset.next();) {
                    final WrappedAddIssueCommentParams addIssueCommentParams = new WrappedAddIssueCommentParams(
                            newIssue.getId());
                    long origIssueCommentId = rset.getLong();
                    String content = rset.getString();
                    String createdUserName = rset.getString();
                    Date created = rset.getJavaUtilDate();
                    Date updated = rset.getJavaUtilDate();

                    if (content != null) {
                        addIssueCommentParams.setContent(content);
                    }

                    final WrappedUpdateIssueParams updateIssueParams = new WrappedUpdateIssueParams(newIssue.getId());

                    // CommentChangeLogにも行く。むしろこれが本命。
                    processCommentChangeLog(origIssueCommentId, newIssue, updateIssueParams, addIssueCommentParams);

                    if (updateIssueParams.isPostDataExists()) {
                        try {
                            RetryableUpdateIssue apicallout = new RetryableUpdateIssue(updateIssueParams.getPostData());
                            apicallout.execute(bklConn);
                            apicallout.getResult();
                        } catch (BacklogAPIException ex) {
                            if (ex.getStatusCode() == 400 && ex.getMessage().contains("No comment content")) {
                                // [MBC5105] No comment content occured: 変更コメントに ((移行の結果差分なし)) と加えてリトライ。
                                RsvrLog.trace(BMCMessages.MBC5105);
                                // "((移行の結果差分なし))" をコメントとしてセット。
                                updateIssueParams.setComment(BMCMessages.MBC5106);

                                RetryableUpdateIssue apicallout = new RetryableUpdateIssue(
                                        updateIssueParams.getPostData());
                                apicallout.execute(bklConn);
                                apicallout.getResult();
                            }
                        }
                        // API呼び出しインターバルをsleepします。
                        RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
                    }

                    if (addIssueCommentParams.isPostDataExists()) {
                        RetryableAddIssueComment apicallout = new RetryableAddIssueComment(
                                addIssueCommentParams.getPostData());
                        apicallout.execute(bklConn);
                        apicallout.getResult();

                        // API呼び出しインターバルをsleepします。
                        RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
                    }
                }
            }
        }
    }

    private void processCommentChangeLog(long origIssueCommentId, Issue newIssue,
            WrappedUpdateIssueParams updateIssueParams, WrappedAddIssueCommentParams addIssueCommentParams)
            throws SQLException, IOException {

        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("SELECT" //
                + " Field, OriginalValue, NewValue, IssueAttachmentId, AttributeInfo, NotificationInfo" //
                + " FROM BacklogIssueCommentChangeLog" //
                + " WHERE IssueCommentId = ?" //
                + " ORDER BY IssueCommentChangeLogId"))) { //
            stmt.setLong(origIssueCommentId);
            try (RsvrResultSet rset = stmt.executeQuery()) {
                for (; rset.next();) {
                    String field = rset.getString();
                    String originalValue = rset.getString();
                    String newValue = rset.getString();
                    Long issueAttachmentId = rset.getLong();
                    String attributeInfo = rset.getString();
                    String notificationInfo = rset.getString();
                    if (notificationInfo != null) {
                        // TODO 通知を処理
                    }

                    // これはなにだ component

                    if ("parentIssue".equals(field)) {
                        // これは次のパスで処理 parentIssue
                        // ここでは実施せず、別のフェーズにて対応します。
                    } else if ("summary".equals(field)) {
                        updateIssueParams.setSummary(newValue);
                    } else if ("description".equals(field)) {
                        updateIssueParams.setDescription(newValue);
                    } else if ("notification".equals(field) && newValue != null) {
                        final String[] notiTarget = newValue.split(",");
                        List<Long> notiList = new ArrayList<Long>();
                        for (String look : notiTarget) {
                            notiList.add(H2MappingUserDao.getTargetUserIdBySourceUserId(conn, Long.valueOf(look)));

                            // 本文コンテンツがない場合は無視する。
                            addIssueCommentParams.setNotifiedUserIds(notiList);

                            updateIssueParams.setNotifiedUserIds(notiList);
                        }
                    } else if ("priority".equals(field)) {
                        try (RsvrPreparedStatement stmt2 = RsvrJdbc.wrap(conn.prepareStatement("SELECT" //
                                + " IssuePriorityTypeId FROM BacklogTargetIssuePriorityType WHERE Name=?"))) {
                            stmt2.setString(newValue);
                            try (RsvrResultSet rset2 = stmt2.executeQuery()) {
                                if (rset2.next() == false) {
                                    RsvrLog.warn("想定外: 指定の名称のPriorityが見つかりません: " + newValue);
                                } else {
                                    updateIssueParams.setPriority(PriorityType.valueOf(rset2.getInt()));
                                }
                            }
                        }
                    } else if ("status".equals(field)) {
                        try {
                            long issueStatusTypeId = H2TargetIssueStatusTypeDao.getIssueStatusTypeIdByName(conn,
                                    newValue);
                            updateIssueParams.setStatusId((int) issueStatusTypeId);
                        } catch (IOException ex) {
                            RsvrLog.warn(
                                    "過去のある時点では存在した IssueType のセットを試みた模様: スキップ: " + newValue + ": " + ex.getMessage());
                        }
                    } else if ("assigner".equals(field)) {
                        updateIssueParams
                                .setAssigneeId(H2MappingUserDao.getTargetUserIdBySourceUserName(conn, newValue));
                    } else if ("startDate".equals(field)) {
                        updateIssueParams.setStartDate(newValue);
                    } else if ("limitDate".equals(field)) {
                        updateIssueParams.setDueDate(newValue);
                    } else if ("estimatedHours".equals(field)) {
                        updateIssueParams.setEstimatedHours(newValue == null ? null : new BigDecimal(newValue));
                    } else if ("actualHours".equals(field)) {
                        updateIssueParams.setActualHours(newValue == null ? null : new BigDecimal(newValue));
                    } else if ("attachment".equals(field)) {
                        // 添付ファイル
                        processAttachment(issueAttachmentId, updateIssueParams);
                    } else if ("resolution".equals(field)) {
                        if (newValue == null || newValue.length() == 0) {
                            // 設定の解除
                            updateIssueParams.setResolution(null);
                        } else {
                            try (RsvrPreparedStatement stmt2 = RsvrJdbc.wrap(conn.prepareStatement("SELECT" //
                                    + " IssueResolutionTypeId" //
                                    + " FROM BacklogTargetIssueResolutionType" //
                                    + " WHERE Name=?"))) {
                                stmt2.setString(newValue);
                                try (RsvrResultSet rset2 = stmt2.executeQuery()) {
                                    if (rset2.next() == false) {
                                        RsvrLog.warn("想定外: 指定の名称のResolutionが見つかりません: " + newValue);
                                    } else {
                                        updateIssueParams.setResolution(ResolutionType.valueOf(rset2.getInt()));
                                    }
                                }
                            }
                        }
                    } else if ("component".equals(field) || "category".equals(field)) {
                        // categoryはcomponentとして飛んでくるようだ。
                        try {
                            updateIssueParams
                                    .setCategoryIds((H2TargetCategoryDao.getCategoryIdListByNames(conn, newValue)));
                        } catch (IOException ex) {
                            RsvrLog.warn(
                                    "過去のある時点では存在した Category のセットなどを試みた模様: スキップ: " + newValue + ": " + ex.getMessage());
                        }
                    } else if ("milestone".equals(field)) {
                        try {
                            updateIssueParams
                                    .setMilestoneIds((H2TargetMilestoneDao.getMilestoneIdListByNames(conn, newValue)));
                        } catch (IOException ex) {
                            RsvrLog.warn(
                                    "過去のある時点では存在した Milestone のセットなどを試みた模様: スキップ: " + newValue + ": " + ex.getMessage());
                        }
                    } else if ("version".equals(field)) {
                        try {
                            updateIssueParams
                                    .setVersionIds((H2TargetVersionDao.getVersionIdListByNames(conn, newValue)));
                        } catch (IOException ex) {
                            RsvrLog.warn(
                                    "過去のある時点では存在した Version のセットなどを試みた模様: スキップ: " + newValue + ": " + ex.getMessage());
                        }
                    } else if ("issueType".equals(field)) {
                        try {
                            long issueTypeId = H2TargetIssueTypeDao.getIssueTypeIdByName(conn, newValue);
                            updateIssueParams.setIssueTypeId(issueTypeId);
                        } catch (IOException ex) {
                            RsvrLog.warn(
                                    "過去のある時点では存在した issueType のセットなどを試みた模様: スキップ: " + newValue + ": " + ex.getMessage());
                        }
                    } else {
                        RsvrLog.warn("警告: updateIssueParams 未実装: " + field + ": [" + newValue + "]");
                    }
                }
            }

        }
    }

    private void processAttachment(long issueAttachmentId, final WrappedUpdateIssueParams updateIssueParams)
            throws SQLException, IOException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("SELECT" //
                + " Name, LocalFilename" //
                + " FROM BacklogIssueAttachment" //
                + " WHERE IssueAttachmentId = " + issueAttachmentId))) { //
            try (RsvrResultSet rset = stmt.executeQuery()) {
                if (rset.next() == false) {
                    RsvrLog.warn("課題の添付ファイル処理: 与えられた issueAttachmentId:" + issueAttachmentId
                            + " が BacklogIssueAttachment テーブル中に見つからず。これは過去に存在した課題添付ファイルが最新状態では存在しないという可能性が考えられる。");
                    // 処理継続はできないのでここで離脱する。
                    return;
                }

                String name = rset.getString();
                String localFilename = rset.getString();
                File localFile = new File(bklConn.getToolConf().getDirExpAttachment(), localFilename);
                if (localFile.exists() == false) {
                    throw new IOException(
                            "Unexpected Local attachment file not exist: " + localFile.getCanonicalPath());
                }

                RetryablePostAttachment apicallout = new RetryablePostAttachment(name, localFile);
                apicallout.execute(bklConn);
                Attachment attachment = apicallout.getResult();

                updateIssueParams.addAttachmentIds(attachment.getId());
            }
        }
    }
}

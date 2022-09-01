package consulting.reservoir.backlog.migtool.core.imp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.nulabinc.backlog4j.Issue;
import com.nulabinc.backlog4j.Issue.ResolutionType;
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;

/**
 * UpdateIssueParams に適切に値を設定するためのラッパークラス。
 */
class WrappedUpdateIssueParams {
    private static final boolean IS_DEBUG = false;

    private boolean isUpdated = false;

    private UpdateIssueParams updateIssueParams = null;

    private List<Long> internalAttachmentIds = new ArrayList<Long>();

    // summary
    // parentIssueId
    // description
    // statusId
    // resolution
    // startDate
    // dueDate
    // estimatedHours
    // actualHours
    // issueTypeId
    // categoryIds
    // versionIds
    // milestoneIds
    // priority
    // assigneeId
    // notifiedUserIds
    // attachmentIds
    // comment
    // その他カスタム項目群
    //
    // オリジナルの日時は保存できない。（方法がわからない）

    public WrappedUpdateIssueParams(long newIssueId) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#Constructor: " + newIssueId);
        updateIssueParams = new UpdateIssueParams(newIssueId);
    }

    public void setSummary(String arg) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#setSummary: " + arg);
        updateIssueParams.summary(arg);
        isUpdated = true;
    }

    public void setParentIssueId(long arg) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#setParentIssueId: " + arg);
        updateIssueParams.parentIssueId(arg);
        isUpdated = true;
    }

    public void setDescription(String arg) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#setDescription: " + arg);
        updateIssueParams.description(arg);
        isUpdated = true;
    }

    public void setStatusId(int arg) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#setStatusId: " + arg);
        updateIssueParams.statusId(arg);
        isUpdated = true;
    }

    public void setResolution(ResolutionType arg) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#setResolution: " + arg);
        updateIssueParams.resolution(arg);
        isUpdated = true;
    }

    public void setStartDate(String arg) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#setStartDate: " + arg);
        updateIssueParams.startDate(arg);
        isUpdated = true;
    }

    public void setDueDate(String arg) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#setDueDate: " + arg);
        updateIssueParams.dueDate(arg);
        isUpdated = true;
    }

    public void setEstimatedHours(BigDecimal arg) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#setEstimatedHours: " + arg);
        updateIssueParams.estimatedHours(arg);
        isUpdated = true;
    }

    public void setActualHours(BigDecimal arg) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#setActualHours: " + arg);
        updateIssueParams.actualHours(arg);
        isUpdated = true;
    }

    public void setIssueTypeId(long arg) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#setIssueTypeId: " + arg);
        updateIssueParams.issueTypeId(arg);
        isUpdated = true;
    }

    public void setCategoryIds(List<Long> argList) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#setCategoryIds: " + argList);
        updateIssueParams.categoryIds(argList);
        isUpdated = true;
    }

    public void setVersionIds(List<Long> argList) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#setVersionIds: " + argList);
        updateIssueParams.versionIds(argList);
        isUpdated = true;
    }

    public void setMilestoneIds(List<Long> argList) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#setMilestoneIds: " + argList);
        updateIssueParams.milestoneIds(argList);
        isUpdated = true;
    }

    public void setPriority(Issue.PriorityType arg) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#setIssueTypeId: " + arg);
        updateIssueParams.priority(arg);
        isUpdated = true;
    }

    public void setAssigneeId(long arg) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#setAssigneeId: " + arg);
        updateIssueParams.assigneeId(arg);
        isUpdated = true;
    }

    public void setNotifiedUserIds(List<Long> argList) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#setNotifiedUserIds: " + argList);
        updateIssueParams.notifiedUserIds(argList);
        // (通知については、主体的には更新フラグセットしない) isUpdated = true;
    }

    public void addAttachmentIds(long arg) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#addAttachmentIds: " + arg);
        internalAttachmentIds.add(arg);
        isUpdated = true;
    }

    public void setComment(String arg) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#setComment: " + arg);
        updateIssueParams.comment(arg);
        isUpdated = true;
    }

    public boolean isPostDataExists() {
        return isUpdated;
    }

    public UpdateIssueParams getPostData() {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedUpdateIssueParams#getPostData");

        if (internalAttachmentIds.size() > 0) {
            updateIssueParams.attachmentIds(internalAttachmentIds);
        }

        return updateIssueParams;
    }
}

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

import java.util.List;

import com.nulabinc.backlog4j.api.option.AddIssueCommentParams;

/**
 * AddIssueCommentParams に適切に値を設定するためのラッパークラス。
 */
class WrappedAddIssueCommentParams {
    private static final boolean IS_DEBUG = false;

    private long newIssueId;

    private AddIssueCommentParams addIssueCommentParam = null;

    private boolean isUpdated = false;

    public WrappedAddIssueCommentParams(long newIssueId) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedAddIssueCommentParams#Constructor: " + newIssueId);
        this.newIssueId = newIssueId;
    }

    public void setContent(String content) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedAddIssueCommentParams#setContent: " + content);
        addIssueCommentParam = new AddIssueCommentParams(newIssueId, content);
        isUpdated = true;
    }

    public void setNotifiedUserIds(List<Long> notiList) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedAddIssueCommentParams#setNotifiedUserIds: " + notiList);
        if (addIssueCommentParam == null) {
            System.err.println("警告: addIssueCommentParam が null なのに notifiedUserIds がセットされました。もう一方の処理で使用されることを期待する");
        } else {
            // 本文コンテンツがない場合は無視する。
            addIssueCommentParam.notifiedUserIds(notiList);
            isUpdated = true;
        }
    }

    public void setAttachmentIds(List<Long> argList) {
        if (IS_DEBUG)
            System.err.println("TRACE: WrappedAddIssueCommentParams#setAttachmentIds: " + argList);
        if (addIssueCommentParam == null) {
            System.err.println("想定外: addIssueCommentParam が null なのに attachmentIds がセットされました。");
        } else {
            // 本文コンテンツがない場合は無視する。
            addIssueCommentParam.attachmentIds(argList);
            isUpdated = true;
        }
    }

    public boolean isPostDataExists() {
        return isUpdated;
    }

    public AddIssueCommentParams getPostData() {
        return addIssueCommentParam;
    }
}

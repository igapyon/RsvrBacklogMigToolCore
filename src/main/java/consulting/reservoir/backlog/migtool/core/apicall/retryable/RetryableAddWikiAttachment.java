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
package consulting.reservoir.backlog.migtool.core.apicall.retryable;

import com.nulabinc.backlog4j.Attachment;
import com.nulabinc.backlog4j.BacklogException;
import com.nulabinc.backlog4j.ResponseList;
import com.nulabinc.backlog4j.api.option.AddWikiAttachmentParams;

import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;

/**
 * Backlog API の `addWikiAttachment` をリトライ付きで呼び出すことを可能にするクラス。
 */
public class RetryableAddWikiAttachment extends AbstractRetryableApiCallout {
    private AddWikiAttachmentParams params;
    private ResponseList<Attachment> result;

    /**
     * コンストラクタ.
     */
    public RetryableAddWikiAttachment(AddWikiAttachmentParams params) {
        this.params = params;
    }

    /**
     * API呼び出しを処理します。
     */
    @Override
    void processApiCallout(RsvrBacklogApiConn bklConn) throws BacklogException {
        result = bklConn.getClient().addWikiAttachment(params);
    }

    /**
     * API呼び出し結果を取得します。
     * 
     * @return API呼び出し結果。
     */
    public ResponseList<Attachment> getResult() {
        return result;
    }
}

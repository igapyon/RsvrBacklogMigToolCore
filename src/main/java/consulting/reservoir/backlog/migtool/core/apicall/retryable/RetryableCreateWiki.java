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

import com.nulabinc.backlog4j.BacklogException;
import com.nulabinc.backlog4j.Wiki;
import com.nulabinc.backlog4j.api.option.CreateWikiParams;

import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;

/**
 * Backlog API の `createWiki` をリトライ付きで呼び出すことを可能にするクラス。
 */
public class RetryableCreateWiki extends AbstractRetryableApiCallout {
    private CreateWikiParams params;
    private Wiki result;

    /**
     * コンストラクタ.
     */
    public RetryableCreateWiki(CreateWikiParams params) {
        this.params = params;
    }

    /**
     * API呼び出しを処理します。
     */
    @Override
    void processApiCallout(RsvrBacklogApiConn bklConn) throws BacklogException {
        result = bklConn.getClient().createWiki(params);
    }

    /**
     * API呼び出し結果を取得します。
     * 
     * @return API呼び出し結果。
     */
    public Wiki getResult() {
        return result;
    }
}

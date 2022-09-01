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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;

import com.nulabinc.backlog4j.Attachment;
import com.nulabinc.backlog4j.AttachmentData;
import com.nulabinc.backlog4j.BacklogAPIException;
import com.nulabinc.backlog4j.BacklogException;
import com.nulabinc.backlog4j.internal.file.AttachmentDataImpl;

import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;

/**
 * Backlog API の `postAttachment` をリトライ付きで呼び出すことを可能にするクラス。
 * 
 * InputStream の再オープンを実現するために、このクラスはたのリトライクラスに比べて記述が少し複雑です。
 */
public class RetryablePostAttachment extends AbstractRetryableApiCallout {
    private String name;
    private File localFile;
    private Attachment result;

    /**
     * コンストラクタ.
     */
    public RetryablePostAttachment(String name, File localFile) {
        this.name = name;
        this.localFile = localFile;
    }

    /**
     * API呼び出しを処理します。
     */
    @Override
    void processApiCallout(RsvrBacklogApiConn bklConn) throws BacklogException {
        // リトライを想定し、これらパラメータはこの場所でインスタンス作成する必要があります。
        // この inStreamはクローズしちゃダメ。クローズするのは Backlog APIのほうだよ。
        InputStream inStream = null;
        try {
            inStream = FileUtils.openInputStream(localFile);
        } catch (IOException ex) {
            throw new BacklogAPIException("想定しないIOエラーが発生: " + ex.toString(), ex);
        }

        AttachmentData attachmentData = new AttachmentDataImpl(name, inStream);
        result = bklConn.getClient().postAttachment(attachmentData);
    }

    /**
     * API呼び出し結果を取得します。
     * 
     * @return API呼び出し結果。
     */
    public Attachment getResult() {
        return result;
    }
}

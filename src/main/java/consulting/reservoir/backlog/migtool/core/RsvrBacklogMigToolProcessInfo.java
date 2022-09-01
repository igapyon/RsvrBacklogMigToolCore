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
package consulting.reservoir.backlog.migtool.core;

import java.util.HashMap;
import java.util.Map;

/**
 * BacklogMigToolの動作結果情報を蓄えるクラス。何件レコード追加/更新したのかを保持します。
 */
public class RsvrBacklogMigToolProcessInfo {
    public static final String[] COUNTER_TYPE = { "User", "Project", "IssueType", "IssueStatusType", "Category",
            "Milestone", "Version", "CustomFieldSetting", //
            "File", "Wiki", "WikiAttachment", //
            "Issue", "IssueComment", "IssueCustomField", "IssueAttachment", "IssueCommentChangeLog",
            // TARGET
            "TargetUser", "TargetProject", "TargetIssueType", "TargetCategory", "TargetMilestone", "TargetVersion", //
            "TargetFile", "TargetWiki", "TargetWikiAttachment", //
            "TargetIssue", "TargetIssuePriorityType", "TargetIssueResolutionType", "TargetIssueStatusType", //
            // MAPPING
            "MappingUser", //
    };

    /**
     * 処理件数を記憶するマップ
     */
    private final Map<String, Integer> counter = new HashMap<String, Integer>();

    /**
     * 処理件数の状況を文字列形式で取得。
     * 
     * @param counterType
     * @return
     */
    public String getDisplayString(String counterType) {
        validateCounterType(counterType);
        return "`" + counterType + "`: ins:" + getIns(counterType) + ", upd:" + getUpd(counterType);
    }

    /**
     * すべての処理件数を標準エラー出力にダンプ。
     */
    public void dumpAllCounter() {
        System.err.println("All counter:");
        for (String look : COUNTER_TYPE) {
            System.err.println("  " + getDisplayString(look));
        }
    }

    /**
     * 追加件数を取得。
     * 
     * @param counterType
     * @return
     */
    public int getIns(String counterType) {
        validateCounterType(counterType);
        Integer lookup = counter.get(counterType + ":" + "Ins");
        if (lookup == null) {
            return 0;
        }
        return lookup;
    }

    /**
     * 更新件数を取得。
     * 
     * @param counterType
     * @return
     */
    public int getUpd(String counterType) {
        validateCounterType(counterType);
        Integer lookup = counter.get(counterType + ":" + "Upd");
        if (lookup == null) {
            return 0;
        }
        return lookup;
    }

    /**
     * 追加件数をインクリメント。
     * 
     * @param counterType
     */
    public void incrementIns(String counterType) {
        validateCounterType(counterType);
        int current = getIns(counterType);
        counter.put(counterType + ":" + "Ins", ++current);
    }

    /**
     * 更新件数をインクリメント。
     * 
     * @param counterType
     */
    public void incrementUpd(String counterType) {
        validateCounterType(counterType);
        int current = getUpd(counterType);
        counter.put(counterType + ":" + "Upd", ++current);
    }

    /**
     * 与えられたカウンタータイプ名が正しいものかどうか判定。
     * 
     * @param counterType
     */
    private static void validateCounterType(String counterType) {
        for (String look : COUNTER_TYPE) {
            if (look.equals(counterType)) {
                return;
            }
        }
        throw new IllegalArgumentException("Unexpected: Unknown counterType: " + counterType);
    }
}

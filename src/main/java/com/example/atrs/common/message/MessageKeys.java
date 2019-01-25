/*
 * Copyright 2014-2018 NTT Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.example.atrs.common.message;

/**
 * メッセージキーを表す列挙型。
 * 
 * @author NTT 電電太郎
 */
public enum MessageKeys {

    /**
     * 会員情報の更新が成功したことを通知するためのメッセージキー。
     */
    I_AR_C2_2001("i.ar.c2.2001");

    /**
     * メッセージキー。
     */
    private final String key;

    /**
     * コンストラクタ。
     * 
     * @param key メッセージキー。
     */
    private MessageKeys(String key) {
        this.key = key;
    }

    /**
     * メッセージキーを取得する。
     * 
     * @return メッセージキー
     */
    public String key() {
        return key;
    }

}
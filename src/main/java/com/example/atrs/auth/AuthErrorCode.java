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
package com.example.atrs.auth;

import com.example.atrs.common.exception.AtrsErrorCode;

public enum AuthErrorCode implements AtrsErrorCode {
	/**
	 * 会員番号またはパスワードが確認できませんでした。入力情報をご確認ください。
	 */
	E_AR_A1_2001("e.ar.a1.2001");

	/**
	 * エラーコード。
	 */
	private final String code;

	/**
	 * コンストラクタ。
	 *
	 * @param code エラーコード。
	 */
	AuthErrorCode(String code) {
		this.code = code;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String code() {
		return code;
	}

}

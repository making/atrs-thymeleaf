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
package com.example.atrs.auth.security;

import com.example.atrs.auth.AuthLoginService;
import com.example.atrs.auth.AuthLoginUserDetails;

import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * ログイン成功イベントハンドリングクラス。
 * 
 * @author NTT 電電太郎
 */
@Component
public class AtrsAuthenticationSuccessListener
		implements ApplicationListener<AuthenticationSuccessEvent> {

	/**
	 * 会員ログインサービス。
	 */
	private final AuthLoginService authLoginService;

	public AtrsAuthenticationSuccessListener(AuthLoginService authLoginService) {
		this.authLoginService = authLoginService;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * ログイン処理成功後に会員ログインステータスを更新する。
	 * </p>
	 */
	@Override
	public void onApplicationEvent(AuthenticationSuccessEvent event) {
		Authentication authentication = event.getAuthentication();

		AuthLoginUserDetails userDetails = (AuthLoginUserDetails) authentication.getPrincipal();
		authLoginService.login(userDetails.getAuthLogin());

	}
}

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
package com.example.atrs.member.web;

import java.security.Principal;

import com.example.atrs.common.message.MessageKeys;
import com.example.atrs.member.Member;
import com.example.atrs.member.MemberSession;
import com.example.atrs.member.MemberUpdateService;
import com.example.atrs.member.MembershipSharedService;
import org.terasoluna.gfw.common.message.ResultMessages;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 会員情報変更コントローラ。
 * 
 * @author NTT 電電花子
 */
@Controller
@RequestMapping("member/update")
public class MemberUpdateController {
	private final MemberHelper memberHelper;
	private final MembershipSharedService membershipSharedService;
	private final MemberUpdateService memberUpdateService;
	private final MemberUpdateValidator memberUpdateValidator;

	private final MemberSession memberSession;

	public MemberUpdateController(MemberUpdateService memberUpdateService,
			MemberHelper memberHelper, MembershipSharedService membershipSharedService,
			MemberUpdateValidator memberUpdateValidator, MemberSession memberSession) {
		this.memberUpdateService = memberUpdateService;
		this.memberHelper = memberHelper;
		this.membershipSharedService = membershipSharedService;
		this.memberUpdateValidator = memberUpdateValidator;
		this.memberSession = memberSession;
	}

	/**
	 * 会員情報変更フォームのバリデータをバインダに追加する。
	 * 
	 * @param binder バインダ
	 */
	@InitBinder("memberUpdateForm")
	public void initBinder(WebDataBinder binder) {
		binder.addValidators(memberUpdateValidator);
	}

	/**
	 * 会員情報変更フォームを初期化する。
	 * 
	 * @return 会員情報変更フォーム
	 */
	@ModelAttribute("memberUpdateForm")
	public MemberUpdateForm setUpForm() {
		MemberUpdateForm memberUpdateForm = new MemberUpdateForm();
		return memberUpdateForm;
	}

	/**
	 * 会員情報の変更を行う。
	 * <ul>
	 * <li>チェックエラーがある場合、会員情報変更画面を再表示する。</li>
	 * <li>更新成功の場合、更新完了メッセージを設定して会員情報変更画面を表示する。</li>
	 * </ul>
	 *
	 * @param memberUpdateForm 会員情報変更フォーム
	 * @param model 出力情報を保持するクラス
	 * @param redirectAttributes フラッシュスコープ格納用オブジェクト
	 * @param result チェック結果を保持するクラス
	 * @param principal ログイン情報を持つオブジェクト
	 * @return View論理名
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String update(@Validated MemberUpdateForm memberUpdateForm,
			@CookieValue("atrs-auth") String jwt, BindingResult result, Model model,
			RedirectAttributes redirectAttributes, Principal principal) {

		if (result.hasErrors()) {
			// 検証エラーがある場合は画面再表示
			return updateRedo(memberUpdateForm, model);
		}
		// ログインユーザ情報から会員番号を取得
		String membershipNumber = principal.getName();
		// 会員情報更新
		Member member = memberHelper.toMember(memberUpdateForm);
		member.setMembershipNumber(membershipNumber);
		this.memberUpdateService.updateMember(member, jwt);

		this.memberSession.updateMember(member);

		// 更新完了メッセージ設定
		ResultMessages messages = ResultMessages.success()
				.add(MessageKeys.I_AR_C2_2001.key());
		redirectAttributes.addFlashAttribute(messages);

		// リダイレクトで会員情報変更画面を表示
		return "redirect:/member/update?complete";
	}

	/**
	 * 会員情報変更完了の会員情報変更画面を表示する。
	 *
	 * @param model 出力情報を保持するクラス
	 * @param principal ログイン情報を持つオブジェクト
	 * @return View論理名
	 */
	@RequestMapping(method = RequestMethod.GET, params = "complete")
	public String updateComplete(Model model, Principal principal,
			@CookieValue("atrs-auth") String jwt) {

		// 再検索して会員情報変更画面を表示
		return updateForm(model, principal, jwt);
	}

	/**
	 * 会員情報変更画面を表示する。
	 *
	 * @param model 出力情報を保持するクラス
	 * @param principal ログイン情報を持つオブジェクト
	 * @return View論理名
	 */
	@RequestMapping(method = RequestMethod.GET, params = "form")
	public String updateForm(Model model, Principal principal,
			@CookieValue("atrs-auth") String jwt) {

		// 会員情報から会員情報変更フォームを生成し、設定
		Member member = this.membershipSharedService.findMe(jwt);
		MemberUpdateForm memberUpdateForm = memberHelper.toMemberUpdateForm(member);
		model.addAttribute(memberUpdateForm);

		// カレンダー表示制御のため、生年月日入力可能日付を設定
		model.addAttribute("dateOfBirthMinDate", memberHelper.getDateOfBirthMinDate());
		model.addAttribute("dateOfBirthMaxDate", memberHelper.getDateOfBirthMaxDate());

		return "member/memberUpdateForm";
	}

	/**
	 * 会員情報変更画面を再表示する。
	 * 
	 * @param memberUpdateForm 会員情報変更フォーム
	 * @param model 出力情報を保持するクラス
	 * @return View論理名
	 */
	@RequestMapping(method = RequestMethod.POST, params = "redo")
	public String updateRedo(MemberUpdateForm memberUpdateForm, Model model) {

		// カレンダー表示制御のため、生年月日入力可能日付を設定
		model.addAttribute("dateOfBirthMinDate", memberHelper.getDateOfBirthMinDate());
		model.addAttribute("dateOfBirthMaxDate", memberHelper.getDateOfBirthMaxDate());

		return "member/memberUpdateForm";
	}

}

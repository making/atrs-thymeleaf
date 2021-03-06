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
package com.example.atrs.ticket;

import java.util.Date;
import java.util.List;

import com.example.atrs.common.exception.AtrsBusinessException;
import com.example.atrs.common.logging.LogMessages;
import com.example.atrs.common.util.FareUtil;
import com.example.atrs.member.Gender;
import com.example.atrs.member.Member;
import com.example.atrs.member.MemberMapper;
import org.terasoluna.gfw.common.exception.BusinessException;
import org.terasoluna.gfw.common.exception.SystemException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static com.example.atrs.ticket.TicketErrorCode.E_AR_B2_2002;
import static com.example.atrs.ticket.TicketErrorCode.E_AR_B2_2003;
import static com.example.atrs.ticket.TicketErrorCode.E_AR_B2_2004;
import static com.example.atrs.ticket.TicketErrorCode.E_AR_B2_2005;
import static com.example.atrs.ticket.TicketErrorCode.E_AR_B2_2006;
import static com.example.atrs.ticket.TicketErrorCode.E_AR_B2_2007;
import static com.example.atrs.ticket.TicketErrorCode.E_AR_B2_2008;
import static com.example.atrs.ticket.TicketErrorCode.E_AR_B2_2009;
import static com.example.atrs.ticket.TicketErrorCode.E_AR_B2_2010;

/**
 * チケット予約のサービス実装クラス。
 * 
 * @author NTT 電電三郎
 */
@Service
@Transactional
public class TicketReserveService {

	/**
	 * 大人運賃が適用される最小年齢。
	 */
	private final int adultPassengerMinAge;

	/**
	 * 大人運賃に対する小児運賃の比率(%)。
	 */
	private final int childFareRate;

	/**
	 * フライト情報リポジトリ。
	 */
	private final FlightMapper flightMapper;

	/**
	 * カード会員情報リポジトリ。
	 */
	private final MemberMapper memberMapper;

	/**
	 * 予約代表者に必要な最小年齢。
	 */
	private final int representativeMinAge;

	/**
	 * 予約情報リポジトリ。
	 */
	private final ReservationMapper reservationMapper;

	/**
	 * チケット共通サービス。
	 */
	private final TicketSharedService ticketSharedService;

	public TicketReserveService(TicketProperties props, FlightMapper flightMapper,
			MemberMapper memberMapper,
			ReservationMapper reservationMapper,
			TicketSharedService ticketSharedService) {
		this.representativeMinAge = props.getRepresentativeMinAge();
		this.adultPassengerMinAge = props.getAdultPassengerMinAge();
		this.childFareRate = props.getChildFareRate();
		this.flightMapper = flightMapper;
		this.memberMapper = memberMapper;
		this.reservationMapper = reservationMapper;
		this.ticketSharedService = ticketSharedService;
	}

	/**
	 * 予約チケットの合計金額を計算する。
	 *
	 * @param flightList 予約するフライトのリスト
	 * @param passengerList 搭乗者リスト
	 * @return 予約チケットの合計金額
	 */
	public int calculateTotalFare(List<Flight> flightList,
			List<Passenger> passengerList) {

		Assert.notEmpty(flightList);
		Assert.notEmpty(passengerList);

		// 小児搭乗者数(12歳未満の搭乗者数)
		int childNum = 0;
		// 小児搭乗者数をカウント
		for (Passenger passenger : passengerList) {
			// リスト要素の null チェック
			Assert.notNull(passenger);
			if (passenger.getAge() < adultPassengerMinAge) {
				childNum++;
			}
		}

		// 12歳以上搭乗者数
		int adultNum = passengerList.size() - childNum;

		// 運賃から合計金額を算出
		// 合計金額 = 往路の合計金額 + 復路の合計金額
		// フライト単位の合計金額 = 運賃 × 搭乗者数(12歳以上) +
		// (基本運賃 × 小児運賃の比率 - 運賃種別ごとの割引額) × 搭乗者数（12歳未満)
		// 運賃種別ごとの割引額 = 基本運賃 × 割引率

		// 合計金額にフライト単位の合計金額を加算
		int totalFare = 0;
		for (Flight flight : flightList) {
			// リスト要素の null チェック
			Assert.notNull(flight);

			Route route = flight.getFlightMaster().getRoute();
			int baseFare = ticketSharedService.calculateBasicFare(route.getBasicFare(),
					flight.getBoardingClass().getBoardingClassCd(),
					flight.getDepartureDate());

			int discountRate = flight.getFareType().getDiscountRate();
			int boardingFare = ticketSharedService.calculateFare(baseFare, discountRate);

			int fare = boardingFare * adultNum
					+ baseFare * (childFareRate - discountRate) / 100 * childNum;

			// 合計金額
			totalFare += fare;
		}

		// 合計金額の100円未満を切り上げ
		totalFare = FareUtil.ceilFare(totalFare);

		return totalFare;
	}

	/**
	 * 会員番号に該当するカード会員情報を検索する。
	 *
	 * @param membershipNumber 会員番号
	 * @return カード会員情報
	 */
	public Member findMember(String membershipNumber) {

		Assert.hasText(membershipNumber);

		return memberMapper.findOne(membershipNumber);
	}

	/**
	 * 予約情報を登録し、予約したチケット料金の支払期限を決定する。
	 *
	 * @param reservation 予約情報
	 * @return 予約番号と予約したチケット料金の支払期限
	 * @throws BusinessException 空席数が搭乗者数未満の場合にスローする例外
	 */
	public TicketReserveDto registerReservation(Reservation reservation)
			throws BusinessException {

		Assert.notNull(reservation);

		// 予約フライト情報一覧
		List<ReserveFlight> reserveFlightList = reservation.getReserveFlightList();
		Assert.notEmpty(reserveFlightList);

		// 予約フライト情報に対して空席数の確認および更新を実施
		for (ReserveFlight reserveFlight : reserveFlightList) {
			Assert.notNull(reserveFlight);

			Flight flight = reserveFlight.getFlight();
			Assert.notNull(flight);

			// 搭乗日が運賃種別予約可能時期範囲内かチェック
			if (!ticketSharedService.isAvailableFareType(flight.getFareType(),
					flight.getDepartureDate())) {
				throw new AtrsBusinessException(E_AR_B2_2008);
			}

			// 空席数を更新するために、フライト情報を取得する(排他)
			flight = flightMapper.findOneForUpdate(flight.getDepartureDate(),
					flight.getFlightMaster().getFlightName(), flight.getBoardingClass(),
					flight.getFareType());
			int vacantNum = flight.getVacantNum();

			// 搭乗者数
			int passengerNum = reserveFlight.getPassengerList().size();

			// 取得した空席数が搭乗者数以上であることを確認
			if (vacantNum < passengerNum) {
				// 空席数が搭乗者数未満の場合、業務例外をスロー
				throw new AtrsBusinessException(E_AR_B2_2009);
			}

			// 取得した空席数から搭乗者数を引いた数を、フライト情報の空席数に設定
			flight.setVacantNum(vacantNum - passengerNum);

			// 空席数を更新
			int flightUpdateCount = flightMapper.update(flight);
			if (flightUpdateCount != 1) {
				throw new SystemException(LogMessages.E_AR_A0_L9002.getCode(),
						LogMessages.E_AR_A0_L9002.getMessage(flightUpdateCount, 1));
			}
		}

		// 予約情報を登録
		// (パラメータの予約情報に予約番号が格納される)
		int reservationInsertCount = reservationMapper.insert(reservation);
		if (reservationInsertCount != 1) {
			throw new SystemException(LogMessages.E_AR_A0_L9002.getCode(),
					LogMessages.E_AR_A0_L9002.getMessage(reservationInsertCount, 1));
		}
		// 予約番号を取得
		String reserveNo = reservation.getReserveNo();

		// 予約フライト情報、搭乗者情報の登録
		for (ReserveFlight reserveFlight : reserveFlightList) {

			// 予約フライト情報に予約番号を設定
			reserveFlight.setReserveNo(reserveNo);

			// 予約フライト情報を登録
			int reserveFlightInsertCount = reservationMapper
					.insertReserveFlight(reserveFlight);
			if (reserveFlightInsertCount != 1) {
				throw new SystemException(LogMessages.E_AR_A0_L9002.getCode(),
						LogMessages.E_AR_A0_L9002.getMessage(reserveFlightInsertCount,
								1));
			}

			// 全搭乗者情報を登録
			for (Passenger passenger : reserveFlight.getPassengerList()) {
				passenger.setReserveFlightNo(reserveFlight.getReserveFlightNo());
				int passengerInsertCount = reservationMapper
						.insertPassenger(passenger);
				if (passengerInsertCount != 1) {
					throw new SystemException(LogMessages.E_AR_A0_L9002.getCode(),
							LogMessages.E_AR_A0_L9002.getMessage(passengerInsertCount,
									1));
				}
			}
		}

		// 往路搭乗日を支払期限とする
		Date paymentDate = reserveFlightList.get(0).getFlight().getDepartureDate();

		return new TicketReserveDto(reserveNo, paymentDate);

	}

	/**
	 * 予約情報の業務ロジックチェックを行う。
	 *
	 * @param reservation 予約情報
	 * @throws BusinessException 業務例外
	 */
	public void validateReservation(Reservation reservation) throws BusinessException {

		Assert.notNull(reservation);
		Assert.notEmpty(reservation.getReserveFlightList());

		// 予約代表者の年齢が18歳以上であることを検証
		validateRepresentativeAge(reservation.getRepAge());

		// 予約フライト情報一覧を取得
		List<ReserveFlight> reserveFlightList = reservation.getReserveFlightList();

		// 運賃種別の適用可否を検証
		validateFareType(reserveFlightList);

		// 予約者代表者の検証
		validateRepresentativeMemberInfo(reservation);

		// 搭乗者情報と登録されている会員情報を照合
		validatePassengerMemberInfo(reserveFlightList);
	}

	/**
	 * 運賃種別の適用可否を確認する。
	 * 
	 * @param reserveFlightList 予約フライト情報一覧
	 * @throws AtrsBusinessException チェック失敗例外
	 */
	private void validateFareType(List<ReserveFlight> reserveFlightList)
			throws AtrsBusinessException {

		for (ReserveFlight reserveFlight : reserveFlightList) {

			Assert.notNull(reserveFlight);

			// 運賃種別
			FareType fareType = reserveFlight.getFlight().getFareType();

			// 運賃種別コード
			FareTypeCd fareTypeCd = fareType.getFareTypeCd();

			// 搭乗者情報一覧
			List<Passenger> passengerList = reserveFlight.getPassengerList();
			Assert.notEmpty(passengerList);

			if (fareTypeCd == FareTypeCd.LD) {
				// 運賃種別がレディース割の場合

				for (Passenger passenger : passengerList) {
					Assert.notNull(passenger);
					if (passenger.getGender() == Gender.M) {
						// 男性の搭乗者がいる場合、業務例外をスロー
						throw new AtrsBusinessException(E_AR_B2_2007);
					}
				}
			}
			else if (fareTypeCd == FareTypeCd.GD) {
				// 運賃種別がグループ割の場合

				int passengerMinNum = fareType.getPassengerMinNum();
				if (passengerList.size() < passengerMinNum) {
					// 搭乗者数が利用可能最少人数未満の場合、業務例外をスロー
					throw new AtrsBusinessException(E_AR_B2_2010,
							fareType.getFareTypeName(), passengerMinNum);
				}
			}
		}
	}

	/**
	 * 搭乗者情報とカード会員情報の照合を行う。
	 *
	 * @param reserveFlightList 予約フライト情報一覧
	 * @throws AtrsBusinessException 照合失敗例外
	 */
	private void validatePassengerMemberInfo(List<ReserveFlight> reserveFlightList)
			throws AtrsBusinessException {

		for (ReserveFlight reserveFlight : reserveFlightList) {

			Assert.notNull(reserveFlight);

			// 搭乗者情報が登録されている会員情報と同一であることを確認

			// 搭乗者情報一覧
			List<Passenger> passengerList = reserveFlight.getPassengerList();
			Assert.notEmpty(passengerList);

			int position = 1;
			for (Passenger passenger : passengerList) {
				Assert.notNull(passenger);

				String membershipNumber = passenger.getMember().getMembershipNumber();

				// 搭乗者会員番号が入力されている場合のみ照合
				if (StringUtils.hasLength(membershipNumber)) {

					// 搭乗者のカード会員情報取得
					Member passengerMember = memberMapper.findOne(membershipNumber);

					// 会員情報が存在することを確認
					if (passengerMember == null) {
						throw new AtrsBusinessException(E_AR_B2_2005, position);
					}

					// 取得した搭乗者のカード会員情報と搭乗者情報が同一であることを確認
					if (!(passenger.getFamilyName()
							.equals(passengerMember.getKanaFamilyName())
							&& passenger.getGivenName()
									.equals(passengerMember.getKanaGivenName())
							&& passenger.getGender()
									.equals(passengerMember.getGender()))) {
						throw new AtrsBusinessException(E_AR_B2_2006, position);
					}
				}
				position++;
			}

		}
	}

	/**
	 * 予約代表者の年齢が予約代表者最小年齢以上であることをチェックする。
	 *
	 * @param age 予約代表者の年齢
	 */
	private void validateRepresentativeAge(int age) {

		if (age < representativeMinAge) {
			throw new AtrsBusinessException(E_AR_B2_2004, representativeMinAge);
		}
	}

	/**
	 * 予約代表者の情報をチェックする。
	 *
	 * @param reservation 予約情報
	 * @throws AtrsBusinessException チェック失敗例外
	 */
	private void validateRepresentativeMemberInfo(Reservation reservation)
			throws AtrsBusinessException {

		String repMembershipNumber = reservation.getRepMember().getMembershipNumber();

		// 予約代表者会員番号が入力されている場合のみチェック
		if (StringUtils.hasLength(repMembershipNumber)) {

			// 予約代表者の会員情報を取得
			Member repMember = memberMapper.findOne(repMembershipNumber);

			// 該当する会員情報が存在することを確認
			if (repMember == null) {
				throw new AtrsBusinessException(E_AR_B2_2002);
			}

			// 取得した会員情報と予約代表者情報が同一であることを確認
			if (!(reservation.getRepFamilyName().equals(repMember.getKanaFamilyName())
					&& reservation.getRepGivenName().equals(repMember.getKanaGivenName())
					&& reservation.getRepGender().equals(repMember.getGender()))) {
				throw new AtrsBusinessException(E_AR_B2_2003);
			}
		}
	}

}

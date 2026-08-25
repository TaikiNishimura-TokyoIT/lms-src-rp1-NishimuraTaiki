package jp.co.sss.lms.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.entity.TStudentAttendance;
import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.TStudentAttendanceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.LoginUserUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.TrainingTime;

/**
 * 勤怠情報（受講生入力）サービス
 * 
 * @author 東京ITスクール
 */
@Service
public class StudentAttendanceService {

	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private AttendanceUtil attendanceUtil;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	private LoginUserUtil loginUserUtil;
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private TStudentAttendanceMapper tStudentAttendanceMapper;

	/**
	 * 勤怠一覧情報取得
	 * 
	 * @param courseId
	 * @param lmsUserId
	 * @return 勤怠管理画面用DTOリスト
	 */
	public List<AttendanceManagementDto> getAttendanceManagement(Integer courseId,
			Integer lmsUserId) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = tStudentAttendanceMapper
				.getAttendanceManagement(courseId, lmsUserId, Constants.DB_FLG_FALSE);
		for (AttendanceManagementDto dto : attendanceManagementDtoList) {
			// 中抜け時間を設定
			if (dto.getBlankTime() != null) {
				TrainingTime blankTime = attendanceUtil.calcBlankTime(dto.getBlankTime());
				dto.setBlankTimeValue(String.valueOf(blankTime));
			}
			// 遅刻早退区分判定
			AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
			if (statusEnum != null) {
				dto.setStatusDispName(statusEnum.name);
			}
		}

		return attendanceManagementDtoList;
	}

	/**
	 * 出退勤更新前のチェック
	 * 
	 * @param attendanceType
	 * @return エラーメッセージ
	 */
	public String punchCheck(Short attendanceType) {
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 権限チェック
		if (!loginUserUtil.isStudent()) {
			return messageUtil.getMessage(Constants.VALID_KEY_AUTHORIZATION);
		}
		// 研修日チェック
		if (!attendanceUtil.isWorkDay(loginUserDto.getCourseId(), trainingDate)) {
			return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_NOTWORKDAY);
		}
		// 登録情報チェック
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		switch (attendanceType) {
		case Constants.CODE_VAL_ATWORK:
			if (tStudentAttendance != null
					&& !tStudentAttendance.getTrainingStartTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			break;
		case Constants.CODE_VAL_LEAVING:
			if (tStudentAttendance == null
					|| tStudentAttendance.getTrainingStartTime().equals("")) {
				// 出勤情報がないため退勤情報を入力出来ません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY);
			}
			if (!tStudentAttendance.getTrainingEndTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			TrainingTime trainingStartTime = new TrainingTime(
					tStudentAttendance.getTrainingStartTime());
			TrainingTime trainingEndTime = new TrainingTime();
			if (trainingStartTime.compareTo(trainingEndTime) > 0) {
				// 退勤時刻は出勤時刻より後でなければいけません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE);
			}
			break;
		}
		return null;
	}

	/**
	 * 出勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchIn() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 現在の研修時刻
		TrainingTime trainingStartTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				null);
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		if (tStudentAttendance == null) {
			// 登録処理
			tStudentAttendance = new TStudentAttendance();
			tStudentAttendance.setLmsUserId(loginUserDto.getLmsUserId());
			tStudentAttendance.setTrainingDate(trainingDate);
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setTrainingEndTime("");
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setNote("");
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setFirstCreateDate(date);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendance.setBlankTime(null);
			tStudentAttendanceMapper.insert(tStudentAttendance);
		} else {
			// 更新処理
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendanceMapper.update(tStudentAttendance);
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 退勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchOut() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		// 出退勤時刻
		TrainingTime trainingStartTime = new TrainingTime(
				tStudentAttendance.getTrainingStartTime());
		TrainingTime trainingEndTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				trainingEndTime);
		// 更新処理
		tStudentAttendance.setTrainingEndTime(trainingEndTime.toString());
		tStudentAttendance.setStatus(attendanceStatusEnum.code);
		tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
		tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
		tStudentAttendance.setLastModifiedDate(date);
		tStudentAttendanceMapper.update(tStudentAttendance);
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠フォームへ設定
	 * 
	 * @param attendanceManagementDtoList
	 * @return 勤怠編集フォーム
	 */
	public AttendanceForm setAttendanceForm(
			List<AttendanceManagementDto> attendanceManagementDtoList) {

		AttendanceForm attendanceForm = new AttendanceForm();
		attendanceForm.setAttendanceList(new ArrayList<DailyAttendanceForm>());
		attendanceForm.setLmsUserId(loginUserDto.getLmsUserId());
		attendanceForm.setUserName(loginUserDto.getUserName());
		attendanceForm.setLeaveFlg(loginUserDto.getLeaveFlg());

		LinkedHashMap<Integer, String> mapStartHour = new LinkedHashMap<>();
		mapStartHour.put(null, "");
		for (int i = 0; i <= 23; i++) {
			mapStartHour.put(i, String.format("%02d", i));
		}
		LinkedHashMap<Integer, String> mapStartMinute = new LinkedHashMap<>();
		mapStartMinute.put(null, "");
		for (int i = 0; i <= 59; i++) {
			mapStartMinute.put(i, String.format("%02d", i));
		}
		LinkedHashMap<Integer, String> mapEndHour = new LinkedHashMap<>();
		mapEndHour.put(null, "");
		for (int i = 0; i <= 23; i++) {
			mapEndHour.put(i, String.format("%02d", i));
		}
		LinkedHashMap<Integer, String> mapEndMinute = new LinkedHashMap<>();
		mapEndMinute.put(null, "");
		for (int i = 0; i <= 59; i++) {
			mapEndMinute.put(i, String.format("%02d", i));
		}

		// 途中退校している場合のみ設定
		if (loginUserDto.getLeaveDate() != null) {
			attendanceForm
					.setLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy-MM-dd"));
			attendanceForm.setDispLeaveDate(
					dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy年M月d日"));
		}

		// 勤怠管理リストの件数分、日次の勤怠フォームに移し替え
		for (AttendanceManagementDto dto : attendanceManagementDtoList) {

			DailyAttendanceForm dailyAttendanceForm = new DailyAttendanceForm();

			// 基本情報
			dailyAttendanceForm.setStudentAttendanceId(dto.getStudentAttendanceId());
			dailyAttendanceForm.setTrainingDate(dateUtil.toString(dto.getTrainingDate()));
			dailyAttendanceForm.setTrainingStartTime(dto.getTrainingStartTime());
			dailyAttendanceForm.setTrainingEndTime(dto.getTrainingEndTime());

			// 出勤時刻プルダウン
			dailyAttendanceForm.setTrainingStartTimeHour(new LinkedHashMap<>(mapStartHour));
			dailyAttendanceForm.setTrainingStartTimeMinute(new LinkedHashMap<>(mapStartMinute));

			// DBの出勤時刻を「時」と「分」に分解
			String trainingStartTime = dto.getTrainingStartTime();
			if (trainingStartTime != null
					&& !trainingStartTime.isEmpty()) {
				String hourString = trainingStartTime.substring(0, 2);
				String minuteString = trainingStartTime.substring(3, 5);
				Integer hour = Integer.parseInt(hourString);
				Integer minute = Integer.parseInt(minuteString);
				dailyAttendanceForm.setTrainingStartTimeHourValue(hour);
				dailyAttendanceForm.setTrainingStartTimeMinuteValue(minute);
			} else { // DBに出勤時刻がない場合
				dailyAttendanceForm.setTrainingStartTimeHourValue(null);
				dailyAttendanceForm.setTrainingStartTimeMinuteValue(null);
			}

			// 退勤時刻プルダウン
			dailyAttendanceForm.setTrainingEndTimeHour(new LinkedHashMap<>(mapStartHour));
			dailyAttendanceForm.setTrainingEndTimeMinute(new LinkedHashMap<>(mapStartMinute));

			// DBの退勤時刻を「時」と「分」に分解
			String trainingEndTime = dto.getTrainingEndTime();
			if (trainingEndTime != null && !trainingEndTime.isEmpty()) {
				String hourString = trainingEndTime.substring(0, 2);
				String minuteString = trainingEndTime.substring(3, 5);
				Integer hour = Integer.parseInt(hourString);
				Integer minute = Integer.parseInt(minuteString);
				dailyAttendanceForm.setTrainingEndTimeHourValue(hour);
				dailyAttendanceForm.setTrainingEndTimeMinuteValue(minute);
			} else { // DBに退勤時刻がない場合
				dailyAttendanceForm.setTrainingEndTimeHourValue(null);
				dailyAttendanceForm.setTrainingEndTimeMinuteValue(null);
			}

			// 中抜け時間
			if (dto.getBlankTime() != null) {
				dailyAttendanceForm.setBlankTime(dto.getBlankTime());
				dailyAttendanceForm.setBlankTimeValue(String.valueOf(attendanceUtil.calcBlankTime(dto.getBlankTime())));
			}

			// ステータス
			dailyAttendanceForm.setStatus(String.valueOf(dto.getStatus()));
			dailyAttendanceForm.setStatusDispName(dto.getStatusDispName());

			dailyAttendanceForm.setNote(dto.getNote());
			dailyAttendanceForm.setSectionName(dto.getSectionName());
			dailyAttendanceForm.setIsToday(dto.getIsToday());
			dailyAttendanceForm.setDispTrainingDate(dateUtil.dateToString(dto.getTrainingDate(), "yyyy年M月d日(E)"));

			// リストへ追加
			attendanceForm.getAttendanceList().add(dailyAttendanceForm);
		}

		return attendanceForm;
	}

	/**
	 * 勤怠登録・更新処理
	 * 
	 * @param attendanceForm
	 * @return 完了メッセージ
	 * @throws ParseException
	 */
	public String update(AttendanceForm attendanceForm) throws ParseException {

		Integer lmsUserId = loginUserUtil.isStudent() ? loginUserDto.getLmsUserId()
				: attendanceForm.getLmsUserId();

		// 現在の勤怠情報（受講生入力）リストを取得
		List<TStudentAttendance> tStudentAttendanceList = tStudentAttendanceMapper
				.findByLmsUserId(lmsUserId, Constants.DB_FLG_FALSE);

		// 入力された情報を更新用のエンティティに移し替え
		Date date = new Date();
		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {

			// 更新用エンティティ作成
			TStudentAttendance tStudentAttendance = new TStudentAttendance();
			// 日次勤怠フォームから更新用のエンティティにコピー
			BeanUtils.copyProperties(dailyAttendanceForm, tStudentAttendance);
			// 研修日付
			tStudentAttendance
					.setTrainingDate(dateUtil.parse(dailyAttendanceForm.getTrainingDate()));
			// 現在の勤怠情報リストのうち、研修日が同じものを更新用エンティティで上書き
			for (TStudentAttendance entity : tStudentAttendanceList) {
				if (entity.getTrainingDate().equals(tStudentAttendance.getTrainingDate())) {
					tStudentAttendance = entity;
					break;
				}
			}
			tStudentAttendance.setLmsUserId(lmsUserId);
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			// 出勤時刻整形
			//			TrainingTime trainingStartTime = null;
			//			trainingStartTime = new TrainingTime(dailyAttendanceForm.getTrainingStartTime());
			//			tStudentAttendance.setTrainingStartTime(trainingStartTime.getFormattedString());
			TrainingTime trainingStartTime = null;
			if (dailyAttendanceForm.getTrainingStartTimeHourValue() != null
					&& dailyAttendanceForm.getTrainingStartTimeMinuteValue() != null) {
				String startTime = String.format("%02d:%02d", dailyAttendanceForm.getTrainingStartTimeHourValue(),
						dailyAttendanceForm.getTrainingStartTimeMinuteValue());
				trainingStartTime = new TrainingTime(startTime);
				tStudentAttendance.setTrainingStartTime(trainingStartTime.getFormattedString());
			} else {
				tStudentAttendance.setTrainingStartTime("");
			}
			// 退勤時刻整形
			//			TrainingTime trainingEndTime = null;
			//			trainingEndTime = new TrainingTime(dailyAttendanceForm.getTrainingEndTime());
			//			tStudentAttendance.setTrainingEndTime(trainingEndTime.getFormattedString());
			TrainingTime trainingEndTime = null;
			if (dailyAttendanceForm.getTrainingEndTimeHourValue() != null
					&& dailyAttendanceForm.getTrainingEndTimeMinuteValue() != null) {
				String endTime = String.format("%02d:%02d", dailyAttendanceForm.getTrainingEndTimeHourValue(),
						dailyAttendanceForm.getTrainingEndTimeMinuteValue());
				trainingEndTime = new TrainingTime(endTime);
				tStudentAttendance.setTrainingEndTime(trainingEndTime.getFormattedString());
			} else {
				tStudentAttendance.setTrainingEndTime("");
			}
			// 中抜け時間
			tStudentAttendance.setBlankTime(dailyAttendanceForm.getBlankTime());
			// 遅刻早退ステータス
			if ((trainingStartTime != null || trainingEndTime != null)
					&& !dailyAttendanceForm.getStatusDispName().equals("欠席")) {
				AttendanceStatusEnum attendanceStatusEnum = attendanceUtil
						.getStatus(trainingStartTime, trainingEndTime);
				tStudentAttendance.setStatus(attendanceStatusEnum.code);
			}
			// 備考
			tStudentAttendance.setNote(dailyAttendanceForm.getNote());
			// 更新者と更新日時
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			// 削除フラグ
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			// 登録用Listへ追加
			tStudentAttendanceList.add(tStudentAttendance);
		}
		// 登録・更新処理
		for (TStudentAttendance tStudentAttendance : tStudentAttendanceList) {
			if (tStudentAttendance.getStudentAttendanceId() == null) {
				tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
				tStudentAttendance.setFirstCreateDate(date);
				tStudentAttendanceMapper.insert(tStudentAttendance);
			} else {
				tStudentAttendanceMapper.update(tStudentAttendance);
			}
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 過去日未入力チェック
	 * 
	 * @author NishimuraTaiki - Task.25
	 * @return 判定結果
	 * @throws ParseException
	 */
	public Boolean notEnterCheck() throws ParseException {

		// SimpleDateFormatクラスでフォーマットパターンを設定
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		// 現在の日付を取得
		Date now = new Date();
		//System.out.println("now："+ now);

		// 「yyyy/MM/dd」型の文字列に変換
		String nowStr = sdf.format(now);
		Date nowDate = null;
		// 文字列からDate型へ変換
		nowDate = sdf.parse(nowStr);
		//System.out.println("nowDate：" + nowDate);

		// MapperのnotEnterCount呼び出し
		Integer notEnterCount = tStudentAttendanceMapper.notEnterCount(loginUserDto.getLmsUserId(), (short) 0, nowDate);
		//System.out.println("notEnterCount：" + notEnterCount);

		Boolean notEnterFlg = null;
		if (notEnterCount > 0) {
			notEnterFlg = true; // カウントが0より大きいならtrue
		} else {
			notEnterFlg = false; // そうでないならfalse
		}
		//System.out.println("notEnterFlg：" + notEnterFlg)

		return notEnterFlg;
	}

	/**
	 * 出退勤時間フォーマット変換
	 * 
	 * @author NishimuraTaiki - Task.26
	 * @param attendanceForm
	 */
	public void formatConversion(AttendanceForm attendanceForm) {

	}

}
package jp.co.sss.lms.form;

import java.util.LinkedHashMap;

import lombok.Data;

/**
 * 日次の勤怠フォーム
 * 
 * @author 東京ITスクール
 */
@Data
public class DailyAttendanceForm {

	/** 受講生勤怠ID */
	private Integer studentAttendanceId;
	/** 途中退校日 */
	private String leaveDate;
	/** 日付 */
	private String trainingDate;
	/** 出勤時間 */
	private String trainingStartTime;
	/** 出勤時刻：時の選択肢 */
    private LinkedHashMap<Integer, String> trainingStartTimeHour;
    /** 出勤時刻：選択されている時 */
    private Integer trainingStartTimeHourValue;
    /** 出勤時刻：分の選択肢 */
    private LinkedHashMap<Integer, String> trainingStartTimeMinute;
    /** 出勤時刻：選択されている分 */
    private Integer trainingStartTimeMinuteValue;
    /** 退勤時間 */
	private String trainingEndTime;
	/** 退勤時刻：時の選択肢 */
    private LinkedHashMap<Integer, String> trainingEndTimeHour;
    /** 退勤時刻：選択されている時 */
    private Integer trainingEndTimeHourValue;
    /** 退勤時刻：分の選択肢 */
    private LinkedHashMap<Integer, String> trainingEndTimeMinute;
    /** 退勤時刻：選択されている分 */
    private Integer trainingEndTimeMinuteValue;
	/** 中抜け時間 */
	private Integer blankTime;
	/** 中抜け時間（画面表示用） */
	private String blankTimeValue;
	/** ステータス */
	private String status;
	/** 備考 */
	private String note;
	/** セクション名 */
	private String sectionName;
	/** 当日フラグ */
	private Boolean isToday;
	/** エラーフラグ */
	private Boolean isError;
	/** 日付（画面表示用） */
	private String dispTrainingDate;
	/** ステータス（画面表示用） */
	private String statusDispName;
	/** LMSユーザーID */
	private String lmsUserId;
	/** ユーザー名 */
	private String userName;
	/** コース名 */
	private String courseName;
	/** インデックス */
	private String index;

}
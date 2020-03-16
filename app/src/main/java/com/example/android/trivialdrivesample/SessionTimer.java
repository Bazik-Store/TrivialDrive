package com.example.android.trivialdrivesample;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Ehsan Joon for bazik-trivial-driver at 16/03/2020
 */
public class SessionTimer extends TimerTask {

  private static final String TAG = SessionTimer.class.getSimpleName();
  public static final Integer DELAY = 0;
  public static final Integer PEROID = 1000;
  private int passedTimeInSeconds = 0;
  private String startTime;
  private SimpleDateFormat simpleDateFormat;

  public SessionTimer(String startTim) {
    simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    this.startTime = startTim;
  }

  @Override
  public void run() {
    passedTimeInSeconds += 1;
  }

  String getEndTime() {
    String endTime;

    Date startDate = null;
    try {
      startDate = simpleDateFormat.parse(startTime);
    } catch (ParseException e) {
      e.printStackTrace();
      Log.d(TAG, "getEndTime Error [" + e.toString() + "]");
    }
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(startDate);
    calendar.add(Calendar.SECOND, passedTimeInSeconds);
    endTime = simpleDateFormat.format(calendar.getTime());
    Log.d(TAG, "EndTime is [" + endTime + "] with startTime [" + startTime + "] with passedSeconds is[" + passedTimeInSeconds + "] ");
    return endTime;
  }

}

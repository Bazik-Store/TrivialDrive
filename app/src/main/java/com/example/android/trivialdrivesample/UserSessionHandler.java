package com.example.android.trivialdrivesample;

import android.app.Activity;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.example.android.trivialdrivesample.util.IabHelper;

import java.util.Timer;

/**
 * CREATED BY Javadroid FOR `Session Time Tracker` PROJECT
 * AT: 2020/Feb/24 13:18
 */
public class UserSessionHandler {
  private static final String TAG = UserSessionHandler.class.getSimpleName();
  private static boolean startSessionSent = false;
  private static boolean endSessionSent = false;
  private static SessionTimer mSessionTimer;
  private static Timer mTimer;
  private static Activity mActivity;
  private static IabHelper mIabHelper;
  private static String mStartTime;

  public static void initial(Activity activity, IabHelper iabHelper, String startTime) {
    mActivity = activity;
    mIabHelper = iabHelper;
    mStartTime = startTime;
  }

  public static void submitStartSession() {

    //Prevent resending start session by changing current activity in application
    if (startSessionSent) {
      Log.w(TAG, "Start Session is sent before startSessionSent[" + startSessionSent + "]");
      return;
    }

    try {

      startSessionSent = mIabHelper.sendUserEvent("User has started his/her session now", mActivity.getPackageName(), false, mStartTime);
      endSessionSent = false;

      mTimer = new Timer();
      mSessionTimer = new SessionTimer(mStartTime);
      mTimer.scheduleAtFixedRate(mSessionTimer, SessionTimer.DELAY, SessionTimer.PEROID);

    } catch (RemoteException e) {
      e.printStackTrace();
      Log.d(TAG, "Error on send event [" + e.toString() + "]");
    }

  }

  public static void submitEndSession() {
    if (endSessionSent || mIabHelper == null || mActivity == null) {
      Log.w(TAG, "Prevent to send end session check this out!");
      return;
    }

    try {
      String sessionTime = mSessionTimer.getEndTime();
      endSessionSent = mIabHelper.sendUserEvent(
        "User has finished his/her session now",
        mActivity.getPackageName(),
        true,
        sessionTime
      );

      if (BuildConfig.DEBUG) {
        Log.d(TAG, "Session time is [" + sessionTime + "]");
        Toast.makeText(mActivity, "Session time is [" + sessionTime + "]", Toast.LENGTH_LONG).show();
      }

      cleanup();
    } catch (RemoteException e) {
      e.printStackTrace();
      Log.e(TAG, "Error on sending end session event [" + e.toString() + "]");
    }
  }

  private static void cleanup() {
    mTimer.cancel();
    mActivity = null;
    mIabHelper = null;
    startSessionSent = false;
  }

}

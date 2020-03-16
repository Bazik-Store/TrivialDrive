package com.example.android.trivialdrivesample;

import android.app.Activity;
import android.os.RemoteException;
import android.util.Log;

import com.example.android.trivialdrivesample.util.IabHelper;

import java.util.Timer;

/**
 * CREATED BY Javadroid FOR `Session Time Tracker` PROJECT
 * AT: 2020/Feb/24 13:18
 */
public class ActivityLifecycleHandler {
  private static final String TAG = ActivityLifecycleHandler.class.getSimpleName();
  private static boolean startSessionSent = false;
  private static boolean endSessionSent = false;
  private static SessionTimer sessionTimer;

  static void initialize(Activity activity, IabHelper iabHelper, String startTime) {

    //Prevent resending start session by changing current activity in application
    if (startSessionSent) return;

    try {

      startSessionSent = iabHelper.sendUserEvent("User has started his/her session now", activity.getPackageName(), false, startTime);
      Timer timer = new Timer();
      sessionTimer = new SessionTimer(startTime);
      timer.scheduleAtFixedRate(sessionTimer, SessionTimer.DELAY, SessionTimer.PEROID);

      Log.d(TAG, "Start event sent successfully");

    } catch (RemoteException e) {
      e.printStackTrace();
      Log.d(TAG, "Error on send event [" + e.toString() + "]");
    }

  }

  static void onActivityCreated(Activity activity, IabHelper iabHelper) {
  }

  static void onActivityStarted(Activity activity) {
  }

  static void onActivityResumed(Activity activity) {
  }

  static void onActivityPaused(Activity activity) {
  }

  static void onActivityStopped(Activity activity) {
  }

  static void onActivitySaveInstanceState(Activity activity) {
  }

  static void onActivityDestroyed(Activity activity, IabHelper iabHelper) {
    if (endSessionSent) return;
    try {
      endSessionSent = iabHelper.sendUserEvent(
        "User has finished his/her session now",
        activity.getPackageName(),
        true,
        sessionTimer.getEndTime()
      );
    } catch (RemoteException e) {
      e.printStackTrace();
      Log.e(TAG, "Error on sending end session event [" + e.toString() + "]");
    }
  }

  static void onApplicationCrashed() {
  }

}

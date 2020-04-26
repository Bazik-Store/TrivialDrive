package com.example.android.trivialdrivesample.session;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.trivialdrivesample.util.Logger;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;


/**
 * CREATED BY Javadroid FOR `Session Time Tracker` PROJECT
 * AT: 2020/Feb/24 13:06
 */
public class CustomLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

  private final String TAG = CustomLifecycleCallbacks.class.getSimpleName();
  private final String sessionStartTime;
  private Activity currentActivity;

  public CustomLifecycleCallbacks(String sessionStartTime, Activity activity) {
    this.sessionStartTime = sessionStartTime;
    this.currentActivity = activity;
  }

  @Override
  public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
    Log.d(TAG, "onActivityCreated: activity:[" + activity.getClass().getSimpleName() + "] currentActivity[" + currentActivity.getClass().getSimpleName() + "]");
    currentActivity = activity;
  }

  @Override
  public void onActivityStarted(@NonNull Activity activity) {
    Logger.debug(TAG, "onActivityStarted: activity [" + activity.getClass().getSimpleName() + "]");
  }

  @Override
  public void onActivityResumed(@NonNull Activity activity) {
    Logger.debug(TAG, "onActivityResumed: activity [" + activity.getClass().getSimpleName() + "] currentActivity[" + currentActivity.getClass().getSimpleName() + "]");

    //try to stop any active session
    UserSession.getInstance(currentActivity, sessionStartTime).endSession();

    //update current activity
    currentActivity = activity;

    //Start tracking a new session
    UserSession.getInstance(activity, sessionStartTime);
  }

  @Override
  public void onActivityPaused(@NonNull Activity activity) {
    Logger.debug(TAG, "onActivityPaused: activity [" + activity.getClass().getSimpleName() + "]");
  }

  /**
   * By navigating from activity One to Activity Two we gonna face with this lifecycle
   * Activity One:
   * OnPause()
   * Activity Two():
   * OnCreate()
   * OnStart()
   * OnResume()
   * And finally we got:
   * Activity One():
   * OnStop()
   * <p>
   * So because of above situation we need to save our current activity [currentActivity]
   * And after ending session for last activity (in this senario  activityOne)
   * Start Session for [currentActivity]
   */
  @Override
  public void onActivityStopped(@NonNull Activity activity) {
    Logger.debug(TAG, "onActivityStopped: activity [" + activity.getClass().getSimpleName() + "]");

    if (currentActivity == activity) {
      UserSession.getInstance(activity, sessionStartTime).endSession();
    }

  }

  @Override
  public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
    Logger.debug(TAG, "onActivitySaveInstanceState: activity [" + activity.getClass().getSimpleName() + "]");
  }

  @Override
  public void onActivityDestroyed(@NonNull Activity activity) {
    boolean isAppForeground = isAppInForeground();
    Logger.debug(TAG, "onActivityDestroyed: activity [" + activity.getClass().getSimpleName() + "] Application is in foreground? [" + isAppForeground + "]");
  }

  public static boolean isAppInForeground() {
    ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
    ActivityManager.getMyMemoryState(appProcessInfo);
    return (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE);
  }

}

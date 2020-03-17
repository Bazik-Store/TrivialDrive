package com.example.android.trivialdrivesample;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;


/**
 * CREATED BY Javadroid FOR `Session Time Tracker` PROJECT
 * AT: 2020/Feb/24 13:06
 */
public class CustomLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

  private final String TAG = CustomLifecycleCallbacks.class.getSimpleName();

  public CustomLifecycleCallbacks() {
  }

  @Override
  public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
    Log.v(TAG, "onCreate is called on activity [" + activity.getClass().getSimpleName() + "]");
  }

  @Override
  public void onActivityStarted(@NonNull Activity activity) {
    Log.v(TAG, "onStart is called on activity [" + activity.getClass().getSimpleName() + "]");
  }

  @Override
  public void onActivityResumed(@NonNull Activity activity) {
    Log.v(TAG, "onResume is called on activity [" + activity.getClass().getSimpleName() + "]");
    UserSessionHandler.submitStartSession();
  }

  @Override
  public void onActivityPaused(@NonNull Activity activity) {
    Log.v(TAG, "onPaused is called on activity [" + activity.getClass().getSimpleName() + "]");
  }

  @Override
  public void onActivityStopped(@NonNull Activity activity) {
    Log.v(TAG, "onStop is called on activity [" + activity.getClass().getSimpleName() + "]");
  }

  @Override
  public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
    Log.v(TAG, "onSaveInstanceState is called on activity [" + activity.getClass().getSimpleName() + "]");
  }

  @Override
  public void onActivityDestroyed(@NonNull Activity activity) {
    boolean isAppForeground = isAppInForeground();
    Log.v(TAG, "onDestroy is called on activity [" + activity.getClass().getSimpleName() + "] Application is in foreground? [" + isAppForeground + "]");
    if (!isAppForeground && activity.isFinishing())
      UserSessionHandler.submitEndSession();
  }

  private boolean isAppInForeground() {
    ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
    ActivityManager.getMyMemoryState(appProcessInfo);
    return (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE);
  }

}

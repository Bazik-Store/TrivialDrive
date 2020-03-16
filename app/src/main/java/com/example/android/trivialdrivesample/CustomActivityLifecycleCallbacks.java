package com.example.android.trivialdrivesample;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.trivialdrivesample.util.IabHelper;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;


/**
 * CREATED BY Javadroid FOR `Session Time Tracker` PROJECT
 * AT: 2020/Feb/24 13:06
 */
public class CustomActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

  private final String TAG = CustomActivityLifecycleCallbacks.class.getSimpleName();
  private IabHelper iabHelper;

  public CustomActivityLifecycleCallbacks(Activity activity, IabHelper iabHelper, String startTime) {
    Log.v(TAG, "initial is called on activity with startTime[" + startTime + "]");
    this.iabHelper = iabHelper;
    ActivityLifecycleHandler.initialize(activity, iabHelper, startTime);
  }

  @Override
  public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
    Log.v(TAG, "onCreate is called on activity [" + activity.getClass().getSimpleName() + "]");
    ActivityLifecycleHandler.onActivityCreated(activity, iabHelper);
  }

  @Override
  public void onActivityStarted(@NonNull Activity activity) {
    Log.v(TAG, "onStart is called on activity [" + activity.getClass().getSimpleName() + "]");
    ActivityLifecycleHandler.onActivityStarted(activity);
  }

  @Override
  public void onActivityResumed(@NonNull Activity activity) {
    Log.v(TAG, "onResume is called on activity [" + activity.getClass().getSimpleName() + "]");
    ActivityLifecycleHandler.onActivityResumed(activity);
  }

  @Override
  public void onActivityPaused(@NonNull Activity activity) {
    Log.v(TAG, "onPaused is called on activity [" + activity.getClass().getSimpleName() + "]");
    ActivityLifecycleHandler.onActivityPaused(activity);
  }

  @Override
  public void onActivityStopped(@NonNull Activity activity) {
    Log.v(TAG, "onStop is called on activity [" + activity.getClass().getSimpleName() + "]");
    ActivityLifecycleHandler.onActivityStopped(activity);
  }

  @Override
  public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
    Log.v(TAG, "onSaveInstanceState is called on activity [" + activity.getClass().getSimpleName() + "]");
    ActivityLifecycleHandler.onActivitySaveInstanceState(activity);
  }

  @Override
  public void onActivityDestroyed(@NonNull Activity activity) {
    boolean isAppForeground = isAppInForeground();
    Log.v(TAG, "onDestroy is called on activity [" + activity.getClass().getSimpleName() + "] Application is in foreground? [" + isAppForeground + "]");
    if (!isAppForeground)
      ActivityLifecycleHandler.onActivityDestroyed(activity, iabHelper);
  }

  private boolean isAppInForeground() {
    ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
    ActivityManager.getMyMemoryState(appProcessInfo);
    return (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE);
  }
}

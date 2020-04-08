package com.example.android.trivialdrivesample.session;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Timer;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * CREATED BY Javadroid FOR `Session Time Tracker` PROJECT
 * AT: 2020/Feb/24 13:18
 */
public class UserSession implements SessionTimerInteractionListener {

  static final String BROADCAST_END_EVENT_KEY = "broad_cast_end_event_key";

  private static final String TAG = UserSession.class.getSimpleName();
  private static final String SERVICE_TRACKER_ACTION_NAME = "com.bazik.tracker.event.service";
  private static final String SERVICE_TRACKER_PACKAGE = "ir.irancell.bazik.y";

  //bundle values that gonna use to send events info to the Bazik
  private final static String SESSION_EVENT_NAME = "session_event_name";
  private final static String SESSION_PACKAGE_NAME = "session_package_name";
  private final static String IS_APPLICATION_BACKGROUND = "is_application_foreground";
  private final static String SESSION_START_TIME = "session_start_time";
  private final static String SESSION_PASSED_TIME_IN_SECONDS = "session_passed_time_in_seconds";
  private final static String IS_START_OF_SESSION = "is_start_of_session";

  private static volatile UserSession INSTANCE;

  private SessionTimer mSessionTimer;
  private Timer mTimer;
  private WeakReference<Activity> mActivity;
  private String mStartTime;
  private Messenger mMessenger;
  private boolean mIsBinded;
  private final String packageName;

  private UserSession(Activity activity, String mStartTime) {
    this.mStartTime = mStartTime;
    mActivity = new WeakReference<>(activity);
    packageName = mActivity.get().getPackageName();

    Intent mIntent = new Intent();
    mIntent.setAction(SERVICE_TRACKER_ACTION_NAME);
    mIntent.setPackage(SERVICE_TRACKER_PACKAGE);
    activity.bindService(mIntent, mServiceConnection, BIND_AUTO_CREATE);
    LocalBroadcastManager.getInstance(mActivity.get()).registerReceiver(endBroadCastReceiver, new IntentFilter(BROADCAST_END_EVENT_KEY));
  }

  public static UserSession getInstance(Activity activity, String mStartTime) {
    if (INSTANCE == null) {
      synchronized (UserSession.class) {
        INSTANCE = new UserSession(activity, mStartTime);
      }
    }
    return INSTANCE;
  }


  @Override
  public void periodReached(int passedSecondsInPeriod) {
    Log.d(TAG, "Send Update for End session with Activity [" + mActivity.get().getClass().getName() + "]");
    if (INSTANCE == null || !mIsBinded) {
      Log.e(TAG, "Can't submit any update fot EndSession INSTANCE[" + INSTANCE + "] mIsBinded[" + mIsBinded + "]");
      return;
    }

    Message updateEndSessionMessage = new Message();

    updateEndSessionMessage.setData(
      prepareBundleOfData(
        false ,
        "Automatically user session updated when he/she is active now!!!",
        passedSecondsInPeriod
      )
    );

    try {
      mMessenger.send(updateEndSessionMessage);
      Log.d(TAG, "Session time is [" + passedSecondsInPeriod + "]");
    } catch (RemoteException e) {
      //TODO What's the plan for this kind of situations? Suggestion: calling onClear() and leave this track
      e.printStackTrace();
      Log.e(TAG, "Error while sending end session to the Bazik [" + e.getLocalizedMessage() + "]");
    }
  }

  void endSession() {
    submitEndSession();
  }

  private void submitStartSession() {
    Log.d(TAG, "Send Start session with startTime [" + mStartTime + "] with Activity [" + mActivity.get().getClass().getName() + "]");
    if (mStartTime == null) {
      Log.e(TAG, "Session start time is Null");
      return;
    }

    try {

      Message startSessionMessage = new Message();

      startSessionMessage.setData(
        prepareBundleOfData(
          true,
          "User has started his/her session now",
          0
        )
      );

      mMessenger.send(startSessionMessage);
      startTimer();

    } catch (RemoteException e) {
      e.printStackTrace();
      Log.d(TAG, "Error on send event [" + e.toString() + "]");
    }

  }

  private void submitEndSession() {
    Log.d(TAG, "Send End session with Activity [" + mActivity.get().getClass().getName() + "]");
    if (INSTANCE == null || !mIsBinded) {
      Log.e(TAG, "Can't submitEndSession INSTANCE[" + INSTANCE + "] mIsBinded[" + mIsBinded + "]");
      return;
    }

    int sessionTime = mSessionTimer.getEndTime();

    Message endSessionMessage = new Message();

    endSessionMessage.setData(
      prepareBundleOfData(
        false,
        "User has finished his/her session now",
        sessionTime
      )
    );

    try {
      mMessenger.send(endSessionMessage);
      cleanup();
      Log.d(TAG, "Session time is [" + sessionTime + "]");

    } catch (RemoteException e) {
      //TODO What's the plan for this kind of situations? Suggestion: calling onClear() and leave this track
      e.printStackTrace();
      Log.e(TAG, "Error while sending end session to the Bazik [" + e.getLocalizedMessage() + "]");
    }
  }

  private BroadcastReceiver endBroadCastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.d(TAG, "End session received from AppLifeCycleService");
      submitEndSession();
    }
  };

  private void startTimer() {

    mTimer = new Timer();
    mSessionTimer = new SessionTimer(this);
    mTimer.scheduleAtFixedRate(mSessionTimer, SessionTimer.DELAY, SessionTimer.PERIOD);
  }

  private ServiceConnection mServiceConnection = new ServiceConnection() {

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
      //TODO What's the plan for this kind of situations? Suggestion: calling onClear() and leave this track

      Log.w(TAG, "Tracker Service is disConnected");
      mIsBinded = false;
      mServiceConnection = null;
    }

    @Override
    public void onServiceConnected(ComponentName arg0, IBinder arg1) {
      Log.d(TAG, "Tracker service is Connected");

      mIsBinded = true;
      mMessenger = new Messenger(arg1);
      //Send start session event
      submitStartSession();
    }
  };

  private Bundle prepareBundleOfData(boolean isStartOfSession, String message, int passedTime) {
    Bundle bundleDataForSession = new Bundle();
    bundleDataForSession.putBoolean(IS_START_OF_SESSION, isStartOfSession);
    bundleDataForSession.putString(SESSION_EVENT_NAME, message);
    bundleDataForSession.putString(SESSION_PACKAGE_NAME, packageName);
    bundleDataForSession.putBoolean(IS_APPLICATION_BACKGROUND, !CustomLifecycleCallbacks.isAppInForeground());
    bundleDataForSession.putString(SESSION_START_TIME, mStartTime);
    bundleDataForSession.putInt(SESSION_PASSED_TIME_IN_SECONDS, passedTime);
    return bundleDataForSession;
  }

  private void cleanup() {
    mTimer.cancel();

    if (mIsBinded && mActivity.get() != null) {
      mActivity.get().unbindService(mServiceConnection);
      mIsBinded = false;
    }

    if (endBroadCastReceiver == null)
      mActivity.get().unregisterReceiver(endBroadCastReceiver);

    mActivity = null;
    INSTANCE = null;
  }


}

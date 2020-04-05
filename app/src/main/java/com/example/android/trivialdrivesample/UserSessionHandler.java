package com.example.android.trivialdrivesample;

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
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Timer;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * CREATED BY Javadroid FOR `Session Time Tracker` PROJECT
 * AT: 2020/Feb/24 13:18
 */
public class UserSessionHandler {

  static final String BROADCAST_END_EVENT_KEY = "broad_cast_end_event_key";

  private static final String TAG = UserSessionHandler.class.getSimpleName();
  private static final String SERVICE_TRACKER_ACTION_NAME = "com.bazik.tracker.event.service";
  private static final String SERVICE_TRACKER_PACKAGE = "ir.irancell.bazik.y";

  //bundle values that gonna use to send events info to the Bazik
  private final static String SESSION_EVENT_NAME = "session_event_name";
  private final static String SESSION_PACKAGE_NAME = "session_package_name";
  private final static String SESSION_IS_ENDED = "session_is_ended";
  private final static String SESSION_START_TIME = "session_start_time";
  private final static String SESSION_END_TIME = "session_end_time";

  private static volatile UserSessionHandler INSTANCE;

  private SessionTimer mSessionTimer;
  private Timer mTimer;
  private WeakReference<Activity> mActivity;
  private String mStartTime;
  private Messenger mMessenger;
  private boolean mIsBinded;
  private final String packageName;

  private UserSessionHandler(Activity activity, String mStartTime) {
    this.mStartTime = mStartTime;
    mActivity = new WeakReference<>(activity);
    packageName = mActivity.get().getPackageName();

    Intent mIntent = new Intent();
    mIntent.setAction(SERVICE_TRACKER_ACTION_NAME);
    mIntent.setPackage(SERVICE_TRACKER_PACKAGE);
    activity.bindService(mIntent, mServiceConnection, BIND_AUTO_CREATE);
    LocalBroadcastManager.getInstance(mActivity.get()).registerReceiver(endBroadCastReceiver, new IntentFilter(BROADCAST_END_EVENT_KEY));
  }

  public static UserSessionHandler getInstance(Activity activity, String mStartTime) {
    if (INSTANCE == null) {
      synchronized (UserSessionHandler.class) {
        INSTANCE = new UserSessionHandler(activity, mStartTime);
      }
    }
    return INSTANCE;
  }

  public void endSession() {
    submitEndSession();
  }

  private void submitStartSession() {
    Log.d(TAG, "Send Start session with startTime ["+mStartTime+"] with Activity ["+mActivity.get().getClass().getName()+"]");
    if (mStartTime == null) {
      Log.e(TAG, "Session start time is Null");
      return;
    }

    try {

      Message startSessionMessage = new Message();

      Bundle bundleOfStartSession = new Bundle();
      bundleOfStartSession.putString(SESSION_EVENT_NAME, "User has started his/her session now");
      bundleOfStartSession.putString(SESSION_PACKAGE_NAME, packageName);
      bundleOfStartSession.putBoolean(SESSION_IS_ENDED, false);
      bundleOfStartSession.putString(SESSION_START_TIME, mStartTime);

      startSessionMessage.setData(bundleOfStartSession);

      mMessenger.send(startSessionMessage);
      startTimer();

    } catch (RemoteException e) {
      e.printStackTrace();
      Log.d(TAG, "Error on send event [" + e.toString() + "]");
    }

  }

  private void submitEndSession() {
    Log.d(TAG, "Send End session with Activity ["+mActivity.get().getClass().getName()+"]");
    if (INSTANCE == null || !mIsBinded) {
      Log.e(TAG, "Can't submitEndSession INSTANCE[" + INSTANCE + "] mIsBinded[" + mIsBinded + "]");
      return;
    }

    String sessionTime = mSessionTimer.getEndTime();

    Message endSessionMessage = new Message();

    Bundle bundleOfEndSession = new Bundle();
    bundleOfEndSession.putString(SESSION_EVENT_NAME, "User has finished his/her session now");
    bundleOfEndSession.putString(SESSION_PACKAGE_NAME, packageName);
    bundleOfEndSession.putBoolean(SESSION_IS_ENDED, true);
    bundleOfEndSession.putString(SESSION_END_TIME, sessionTime);

    endSessionMessage.setData(bundleOfEndSession);

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
    mSessionTimer = new SessionTimer(mStartTime);
    mTimer.scheduleAtFixedRate(mSessionTimer, SessionTimer.DELAY, SessionTimer.PEROID);
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

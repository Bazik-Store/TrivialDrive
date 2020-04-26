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

import com.example.android.trivialdrivesample.util.Logger;

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
    mActivity.get().bindService(mIntent, mServiceConnection, BIND_AUTO_CREATE);
    LocalBroadcastManager.getInstance(mActivity.get()).registerReceiver(endBroadCastReceiver, new IntentFilter(BROADCAST_END_EVENT_KEY));
  }

  public static UserSession getInstance(Activity activity, String mStartTime) {
    if (INSTANCE == null) {
      synchronized (UserSession.class) {
        Logger.debug(TAG, "getInstance: is called with this info: activity[" + activity.getClass().getSimpleName() + "] StartTime[" + mStartTime + "]");
        INSTANCE = new UserSession(activity, mStartTime);
      }
    }
    return INSTANCE;
  }


  @Override
  public void periodReached(int passedSecondsInPeriod) {
    Log.d(TAG, "Send Update for End session with Activity [" + mActivity.get().getClass().getName() + "]");
    if (INSTANCE == null || !mIsBinded) {
      Logger.error(TAG, "Can't submit any update fot EndSession INSTANCE[" + INSTANCE + "] mIsBinded[" + mIsBinded + "]");
      return;
    }

    Message updateEndSessionMessage = new Message();

    updateEndSessionMessage.setData(
      prepareBundleOfData(
        false,
        "Automatically user session updated when he/she is active now!!!",
        passedSecondsInPeriod
      )
    );

    try {
      mMessenger.send(updateEndSessionMessage);
      Logger.debug(TAG, "periodReached: Passed seconds is["+ passedSecondsInPeriod + "]");
    } catch (RemoteException e) {
      e.printStackTrace();
      Logger.error(TAG, "periodReached: Error while sending periodicReached seconds [" + e.getLocalizedMessage() + "] Try to call Cleanup() to clean this mess!!!");
      cleanup();
    }
  }

  void endSession() {
    submitEndSession();
  }


  private BroadcastReceiver endBroadCastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      Logger.debug(TAG, "broadcastReceiver: End session received from AppLifeCycleService");
      submitEndSession();
    }
  };

  private void submitStartSession() {
    Logger.debug(TAG, "submitStartSession: Send startTime [" + mStartTime + "] with Activity [" + mActivity.get().getClass().getName() + "]");
    if (mStartTime == null) {
      Logger.warning(TAG, "submitStartSession: Session start time is Null!!! try to cleanup this mess by calling cleanup()");
      cleanup();
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
      Logger.error(TAG, "Error on send event [" + e.toString() + "] Try to call Cleanup() to clean this mess!!!");
      cleanup();
    }

  }

  private void submitEndSession() {
    Logger.debug(TAG, "submitEndSession: Send End session with Activity [" + mActivity.get().getClass().getName() + "]");
    if (INSTANCE == null || !mIsBinded) {
      Logger.error(TAG, "submitEndSession: Can't submit EndSession info: INSTANCE[" + INSTANCE + "] mIsBinded[" + mIsBinded + "]");
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
      Logger.debug(TAG, "Session time is [" + sessionTime + "]");
    } catch (RemoteException e) {
      e.printStackTrace();
      Logger.error(TAG, "Error while sending end session to the Bazik [" + e.getLocalizedMessage() + "] Try to call Cleanup() to clean this mess!!!");
    }

    cleanup();
  }

  private void startTimer() {

    mTimer = new Timer();
    mSessionTimer = new SessionTimer(this);
    mTimer.scheduleAtFixedRate(mSessionTimer, SessionTimer.DELAY, SessionTimer.PERIOD);
  }

  private ServiceConnection mServiceConnection = new ServiceConnection() {

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
      Logger.warning(TAG, "onServiceDisconnected: Tracker service is disconnected");
      cleanup();
    }

    @Override
    public void onServiceConnected(ComponentName arg0, IBinder arg1) {
      Logger.debug(TAG, "Tracker service is Connected with mActivity[" + mActivity.get().getClass().getSimpleName() + "]");

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
    Logger.debug(TAG, "cleanup: Called");
    mTimer.cancel();

    if (mIsBinded && mActivity.get() != null) {
      mActivity.get().unbindService(mServiceConnection);
      mIsBinded = false;
      Logger.debug(TAG, "cleanup: try to unbindService, mIsBinded[" + mIsBinded + "] mActivity[" + mActivity.get().getClass().getSimpleName() + "]");
    }

    if (endBroadCastReceiver != null && mActivity != null) {
      LocalBroadcastManager.getInstance(mActivity.get()).unregisterReceiver(endBroadCastReceiver);
      Logger.debug(TAG, "cleanup: unregister [endBroadCastReceiver]");
    }

    mActivity = null;
    INSTANCE = null;
  }
}

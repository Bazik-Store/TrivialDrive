package com.example.android.trivialdrivesample.session;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by Ehsan Joon for bazik-trivial-driver at 17/03/2020
 * Be aware of that when you press your home button and try to kill the application
 * OnPause(), OnStop(), OnDestroy() methods won't fire. So because of that situations
 * We decided to create this damn service, inorder to submit the user's endSession.
 *
 * Because of the stopWithTask=false:
 * the service will automaticallybe stopped by the system if the user removes
 * a task that is rooted in one of the application's activities.
 */
public class AppLifeCycleService extends Service {
  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  public void onTaskRemoved(Intent rootIntent) {
    super.onTaskRemoved(rootIntent);
    sendEndEventBroadcast();
    stopSelf();
  }

  private void sendEndEventBroadcast(){
    Intent intent = new Intent(UserSession.BROADCAST_END_EVENT_KEY);
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
  }

}

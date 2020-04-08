package com.example.android.trivialdrivesample.session;

import java.util.TimerTask;

/**
 * Created by Ehsan Joon for bazik-trivial-driver at 16/03/2020
 */
public class SessionTimer extends TimerTask {

  private static final String TAG = SessionTimer.class.getSimpleName();
  private static final Integer PERIODIC_TIME_IN_SECONDS = 10;
  static final int DELAY = 0;
  static final int PERIOD = 1000;
  private int passedTimeInSeconds = 0;
  private SessionTimerInteractionListener listener;

  SessionTimer(SessionTimerInteractionListener listener) {
    this.listener = listener;
  }

  @Override
  public void run() {
    passedTimeInSeconds++;

    if (passedTimeInSeconds % PERIODIC_TIME_IN_SECONDS == 0) {
      listener.periodReached(passedTimeInSeconds);
      passedTimeInSeconds = 0;
    }
  }

  Integer getEndTime() {
    return passedTimeInSeconds;
  }

}

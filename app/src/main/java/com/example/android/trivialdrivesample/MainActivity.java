/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trivialdrivesample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.example.android.trivialdrivesample.util.IabHelper;
import com.example.android.trivialdrivesample.util.IabResult;
import com.example.android.trivialdrivesample.util.Purchase;

/**
 * Example game using in-app billing version 3.
 * <p>
 * Before attempting to run this sample, please read the README file. It
 * contains important information on how to set up this project.
 * <p>
 * All the game-specific logic is implemented here in MainActivity, while the
 * general-purpose boilerplate that can be reused in any app is provided in the
 * classes in the util/ subdirectory. When implementing your own application,
 * you can copy over util/*.java to make use of those utility classes.
 * <p>
 * This game is a simple "driving" game where the player can buy gas
 * and drive. The car has a tank which stores gas. When the player purchases
 * gas, the tank fills up (1/4 tank at a time). When the player drives, the gas
 * in the tank diminishes (also 1/4 tank at a time).
 * <p>
 * The user can also purchase a "premium upgrade" that gives them a red car
 * instead of the standard blue one (exciting!).
 * <p>
 * The user can also purchase a subscription ("infinite gas") that allows them
 * to drive without using up any gas while that subscription is active.
 * <p>
 * It's important to note the consumption mechanics for each item.
 * <p>
 * PREMIUM: the item is purchased and NEVER consumed. So, after the original
 * purchase, the player will always own that item. The application knows to
 * display the red car instead of the blue one because it queries whether
 * the premium "item" is owned or not.
 * <p>
 * INFINITE GAS: this is a subscription, and subscriptions can't be consumed.
 * <p>
 * GAS: when gas is purchased, the "gas" item is then owned. We consume it
 * when we apply that item's effects to our app's world, which to us means
 * filling up 1/4 of the tank. This happens immediately after purchase!
 * It's at this point (and not when the user drives) that the "gas"
 * item is CONSUMED. Consumption should always happen when your game
 * world was safely updated to apply the effect of the purchase. So,
 * in an example scenario:
 * <p>
 * BEFORE:      tank at 1/2
 * ON PURCHASE: tank at 1/2, "gas" item is owned
 * IMMEDIATELY: "gas" is consumed, tank goes to 3/4
 * AFTER:       tank at 3/4, "gas" item NOT owned any more
 * <p>
 * Another important point to notice is that it may so happen that
 * the application crashed (or anything else happened) after the user
 * purchased the "gas" item, but before it was consumed. That's why,
 * on startup, we check if we own the "gas" item, and, if so,
 * we have to apply its effects to our world and consume it. This
 * is also very important!
 */
public class MainActivity extends Activity {
    // Debug tag, for logging
    static final String TAG = "TrivialDrive";
    // SKU for our subscription (infinite gas)
    static final String SKU_INFINITE_GAS_MONTHLY = "033P183899";
    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10001;
    // How many units (1/4 tank is our unit) fill in the tank.
    static final int TANK_MAX = 4;
    // Does the user have an active subscription to the infinite gas plan?
    boolean mSubscribedToInfiniteGas = false;
    // Current amount of gas in tank, in units
    int mTank;

    // The helper object
    IabHelper mHelper;

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                setWaitScreen(false);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                setWaitScreen(false);
                return;
            }

            Log.d(TAG, "Purchase successful.");
            if (purchase.getSku().equals(SKU_INFINITE_GAS_MONTHLY)) {
                // bought the infinite gas subscription
                Log.d(TAG, "Infinite gas subscription purchased.");
                alert("Thank you for subscribing to infinite gas!");
                mSubscribedToInfiniteGas = true;
                mTank = TANK_MAX;
                updateUi();
                setWaitScreen(false);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // load game data
        loadData();

        /* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY
         * (that you got from the Google Play developer console). This is not your
         * developer public key, it's the *app-specific* public key.
         *
         * Instead of just storing the entire literal string here embedded in the
         * program,  construct the key at runtime from pieces or
         * use bit manipulation (for example, XOR with some other string) to hide
         * the actual key.  The key itself is not secret information, but we don't
         * want to make it easy for an attacker to replace the public key with one
         * of their own and then fake messages from the server.
         */
        String base64EncodedPublicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDTcEfJMZzM8+LZ9biA72XBVBA4TQziLx8HWUX1tukXOTq/iqEr3MSC6C68y23fR2efwmkovsg8T8DSCLacByusp2RsmFzuiW/Z7ghPxYr27+LZURRKqpes8zReq+b0A9a7ItifGaAuBKudB7886JqDe1k0H8OZuQJSNSBGNz36jwIDAQAB";

        // Some sanity checks to see if the developer (that's you!) really followed the
        // instructions to run this sample (don't put these checks on your app!)
        /*if (base64EncodedPublicKey.contains("CONSTRUCT_YOUR")) {
            throw new RuntimeException("Please put your app's public key in MainActivity.java. See README.");
        }
        if (getPackageName().startsWith("com.example")) {
            throw new RuntimeException("Please change the sample's package name! See README.");
        }*/

        // Create the helper, passing it our context and the public key to verify signatures with
        Log.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(true);
        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");
                Log.d(TAG, "result value:[" + result.toString() + "]");
                // Just in case we're not able to find the Bazik Application on the device, so definitely we're gonna do that forcibly :|
                if (result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE) {
                    showDownloadDialog();
                } else if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    complain("Problem setting up in-app billing: " + result);
                } else if (result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_OK) {
                    Log.d(TAG, "User has subscription");
                }

            }
        });

    }

    /**
     * To show a dialog with download button & the URL of the latest release of Bazik application.
     */
    private void showDownloadDialog() {
        new Builder(MainActivity.this)
          .setTitle("بازیک")
          .setMessage("برای استفاده از تمام امکانات این بازی و شرکت در کلوپ جوایز، باید اپلیکیشن بازیک را دانلود و نصب نمایید.")
          .setPositiveButton("دانلود",
            new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(
                      new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse("http://bazik.ninja/")));
                }
            })
          .create()
          .show()
        ;
    }

    // "Subscribe to infinite gas" button clicked. Explain to user, then start purchase
    // flow for subscription.
    public void onInfiniteGasButtonClicked(View arg0) {
        Log.d(TAG, "Launching purchase flow for gas subscription.");

        try {
            mHelper.launchPurchaseFlow(this, SKU_INFINITE_GAS_MONTHLY,
              RC_REQUEST, mPurchaseFinishedListener, "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error launching purchase flow. Another async operation in progress.");
            setWaitScreen(false);
        }
    }

    /**
     * When user get a hit of score and you wanted to submit the score for the
     * Bazik SDK, Please notice that this score will be involved in leader board calculations,
     * it means that each user in each game will be ordered by their scores that already have submitted
     * while they were struggling to achieve more and more.
     *
     * @param view
     */
    public void onSubmitScoreClicked(View view) {
        Log.d(TAG, "onSubmitScoreClicked() called with: view = [" + view + "]");
        try {
            mHelper.submitScore(
              view.getContext().getPackageName(), // Set The Game's package name
              "1", // Catch it from the developer panel when you defined the score
              100 // You have to make an appropriate value for your score according to the game scenario
            );
        } catch (Exception e) {
            e.printStackTrace();
            showDownloadDialog();
        }

    }


    /**
     * Open up the leader board for score id "1"
     * <p>
     * packageName: send your package name as same as you've registered the game in developer console
     * scoreId: send the score id you can find it from developer console same as you've entered the game's score
     * scope: it's an optional parameter it can be null if you don't have any idea about time scale
     * </p>
     *
     * @param view
     */
    public void onOpenUpLeaderBoardClicked(View view) {
        try {
            mHelper.openLeaderBoard(view.getContext().getPackageName(),
              "1", // catch it from the developer panel when you defined the score
              "ALL" // "ALL","MONTHLY","WEEKLY","DAILY"
            );
        } catch (Exception e) {
            e.printStackTrace();
            showDownloadDialog();
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    /**
     * Verifies the developer payload of a purchase.
     */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        Log.d(TAG, "verifyDeveloperPayload: >>>>>> payload : " + payload);
        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }


    /**
     * When Yozu wanted to open up rating and commenting page on Baik
     *
     * @param arg0
     */
    public void onRateUsClicked(View arg0) {
        /***
         * WARNING: on a real application, we really recommend you use the package name (BuildConfig.APPLICATION_ID)
         * as same as you've registered in Bazik Developer Console
         * */
        try {
            Intent intent = new Intent(Intent.ACTION_EDIT);
            intent.setData(Uri.parse("bazik_store://game_comments?id=" + BuildConfig.APPLICATION_ID));
            intent.setPackage("ir.irancell.bazik.y");
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            showDownloadDialog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // We're being destroyed. It's important to dispose of the helper here!
    @Override
    public void onDestroy() {
        super.onDestroy();

        try {

            // very important:
            if (mHelper != null) {
                mHelper.disposeWhenFinished();
                mHelper = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // updates UI to reflect model
    public void updateUi() {
        // update the car color to reflect premium status or lack thereof
        if (mSubscribedToInfiniteGas) {
            ((ImageView) findViewById(R.id.free_or_premium)).setImageResource(R.drawable.premium);
        }
    }

    // Enables or disables the "please wait" screen.
    void setWaitScreen(boolean set) {
        findViewById(R.id.screen_main).setVisibility(set ? View.GONE : View.VISIBLE);
        findViewById(R.id.screen_wait).setVisibility(set ? View.VISIBLE : View.GONE);
    }

    void complain(String message) {
        Log.e(TAG, "**** TrivialDrive Error: " + message);
        alert("Error: " + message);
    }

    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d(TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }

    void saveData() {

        /*
         * WARNING: on a real application, we recommend you save data in a secure way to
         * prevent tampering. For simplicity in this sample, we simply store the data using a
         * SharedPreferences.
         */

        SharedPreferences.Editor spe = getPreferences(MODE_PRIVATE).edit();
        spe.putInt("tank", mTank);
        spe.apply();
        Log.d(TAG, "Saved data: tank = " + String.valueOf(mTank));
    }

    void loadData() {
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        mTank = sp.getInt("tank", 2);
        Log.d(TAG, "Loaded data: tank = " + String.valueOf(mTank));
    }


}

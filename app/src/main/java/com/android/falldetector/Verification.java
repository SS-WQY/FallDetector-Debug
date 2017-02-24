package com.android.falldetector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.telephony.SmsManager;
import android.text.format.Time;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

public class Verification extends Activity {
    WindowManager.LayoutParams layoutParams;
    boolean bright;
    Uri notification;
    Ringtone r;

    Timer tim;
    TimerTask lvl1;
    TimerTask lvl2;
    TimerTask lvl3;

    PowerManager.WakeLock wl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        bright = false;

        layoutParams = this.getWindow().getAttributes();
        notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        r = RingtoneManager.getRingtone(getApplicationContext(), notification);
        tim = new Timer();

        Button buttonYes = (Button) findViewById(R.id.buttonYes);
        buttonYes.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                wl.release();
                r.stop();
                tim.cancel();
                finish();
            }
        });

        Button buttonNo = (Button) findViewById(R.id.buttonNo);
        buttonNo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                lvl3.run();
            }
        });

        lvl3 = new TimerTask() { //After 480 seconds, call emergency contact
            public void run() {
                wl.release();
                tim.cancel(); //Drop all alarms
                r.stop();
                //Contact emergency number
                sendAndCall(null);

                tim.cancel();
            }
        };
        lvl2 = new TimerTask() { //After 300 seconds, increase alarm intensity
            public void run() {
                r.stop();
                tim.schedule(lvl3, 6000);
            }
        };
        lvl1 = new TimerTask() { //After 180 seconds, set off alarm/flash/vibrate
            public void run() {
                AudioManager aman = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                aman.setStreamVolume(r.getStreamType(), aman.getStreamMaxVolume(r.getStreamType()) / 2, AudioManager.FLAG_PLAY_SOUND);
                r.play();
                tim.schedule(lvl2, 4000);
            }
        };
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "wake");
        wl.acquire();
    }

    @Override
    public void onBackPressed() {
        // Swallow all back button presses
    }

    private Contact loadContact() {
        Contact contact = new Contact();

        InputStream in;
        InputStreamReader isr;
        BufferedReader br;

        try {
            String contactFileName = "contact.txt";
            in = new BufferedInputStream(openFileInput(contactFileName));
            isr = new InputStreamReader(in);
            br = new BufferedReader(isr);

            contact.name = br.readLine();
            contact.cell = br.readLine();
            contact.phoneOther = br.readLine();
            contact.email = br.readLine();

            br.close();
            isr.close();
            in.close();
        } catch (FileNotFoundException e) {
            // will happen until user creates their emergency contact for the first time
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return contact;
    }

    public void sendAndCall(View view) {
        //Contact emergency number
        Contact c = loadContact();

        if (c != null) {
            String number = c.cell;
            Time now = new Time();
            now.setToNow();
            String msg = "[SmartAlert]\n(" + now.format("%D, %R") + ")\nThere's a good chance I might have fallen and am injured. Please help.";

            SmsManager man = SmsManager.getDefault();
            man.sendTextMessage(number, null, msg, null, null);

            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + c.cell));
            startActivity(callIntent);

            finish();
        } else {
            finish();
        }
    }

}

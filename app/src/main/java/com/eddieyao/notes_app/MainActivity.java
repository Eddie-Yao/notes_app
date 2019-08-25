package com.eddieyao.notes_app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    DatabaseReference mRef;
    DatabaseReference mCondition;

    public static final String CHANNEL_1 = "channel1";
    public boolean onBreak = false;
    public int sessionTime;
    public int initialTime;
    public CountDownTimer sessionTimer;
    public CountDownTimer breakTimer;
    public boolean mSessionTimerRunning = false;
    public boolean mSessionTimerPaused = false;
    public boolean mBreakTimerRunning = false;
    public long mSessionTimeLeftInMillis;
    public boolean internalMessage;

//    private Socket mSocket;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRef= FirebaseDatabase.getInstance().getReference();
        mRef.child("break").setValue(0);
        mRef.child("start_session").setValue(0);
        mRef.child("end_session").setValue(0);


        createNotificationChannel();
        setContentView(R.layout.activity_main);

        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int time = dataSnapshot.getValue(Integer.class);
                if (time != 0) {
                    initialTime = time * 1000;
                    mSessionTimeLeftInMillis = time * 1000;
                    startSessionTimer();
                    mRef.child("start_session").setValue(0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        mRef.child("start_session").addValueEventListener(postListener);

        ValueEventListener endListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int ending = dataSnapshot.getValue(Integer.class);
                if (!internalMessage) {
                    if (ending != 0) {
                        pauseTimer();
                        mRef.child("end_session").setValue(0);
                        mSessionTimerRunning = false;
                        TextView countdownTimer = findViewById(R.id.countdownTimer);
                        ProgressBar timerProgress = findViewById(R.id.timerProgress);
                        countdownTimer.setText("00:00:00");
                        timerProgress.setProgress(0);
                    }
                } else {
                    internalMessage = false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        mRef.child("end_session").addValueEventListener(endListener);

        ValueEventListener breakListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final int breakLength = dataSnapshot.getValue(Integer.class);
                if (!internalMessage) {
                    if (breakLength != 0) {
                        if (mSessionTimerRunning) {
                            mSessionTimerPaused = true;
                            pauseTimer();
                        }
                        mRef.child("break").setValue(0);
                        new CountDownTimer(breakLength * 1000, 10) {
                            int totalTime = breakLength * 1000;
                            TextView countdownTimer = findViewById(R.id.countdownTimer);
                            ProgressBar timerProgress = findViewById(R.id.timerProgress);

                            public void onTick(long millisUntilFinished) {
                                timerProgress.setMax(totalTime);
                                int hours = ((int) millisUntilFinished / 1000) / 3600;
                                int secondsLeft = ((int) millisUntilFinished / 1000) - hours * 3600;
                                int minutes = secondsLeft / 60;
                                int seconds = secondsLeft - minutes * 60;
                                String formattedTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);

                                timerProgress.setProgress((int) millisUntilFinished);
                                countdownTimer.setText(formattedTime);
                            }

                            public void onFinish() {
                                timerProgress.setProgress(0);
                                countdownTimer.setText("00:00:00");
                                if (mSessionTimerPaused) {
                                    mSessionTimerPaused = false;
                                    startSessionTimer();
                                }
                                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setTitle("BREAK ENDED");
                                builder.setCancelable(true);
                                builder.setNeutralButton("Close", null);
                                AlertDialog alertDialog = builder.create();
                                alertDialog.show();
                                alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.parseColor("#00adb5"));
                                alertDialog.getWindow().getDecorView().getBackground().setColorFilter(new LightingColorFilter(0xFF000000, 0xFF3a4750));
                            }
                        }.start();
                    }
                } else {
                    internalMessage = false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        mRef.child("break").addValueEventListener(breakListener);

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_1, "Channel 1", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("This is Channel 1");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);;
        }
    }

    public void sendBreak(View view) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(Html.fromHtml("<font color='#eeeeee'>Specify Break Time</font>"));
        alert.setMessage(" ");

        LinearLayout linear=new LinearLayout(this);

        linear.setOrientation(LinearLayout.VERTICAL);
        final TextView text=new TextView(this);
        text.setText("    0 minutes");
        text.setPadding(10, 10, 10, 10);
        text.setTextColor(Color.parseColor("#eeeeee"));

        SeekBar seek=new SeekBar(this);
        seek.setMax(10);
        seek.setProgress(0);
        seek.getThumb().setColorFilter(Color.parseColor("#eeeeee"), PorterDuff.Mode.MULTIPLY);
        seek.getProgressDrawable().setColorFilter(Color.parseColor("#00adb5"), PorterDuff.Mode.MULTIPLY);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                text.setText("    " + String.valueOf(new Integer(progress)) + " minutes");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        linear.addView(seek);
        linear.addView(text);

        alert.setView(linear);

        alert.setPositiveButton("Ok",new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog,int id)
            {
            if (Integer.parseInt(Character.toString(text.getText().charAt(4))) != 0) {
                final int breakLength;
                String firstInt;
                String secondInt;
                if (Character.isDigit(text.getText().charAt(5))) {
                    firstInt = Character.toString(text.getText().charAt(4));
                    secondInt = Character.toString(text.getText().charAt(5));
                    breakLength = Integer.parseInt(firstInt + secondInt);
                } else {
                    firstInt = Character.toString(text.getText().charAt(4));
                    breakLength = Integer.parseInt(firstInt);
                }
                Toast.makeText(getApplicationContext(), "Break Started", Toast.LENGTH_LONG).show();

                internalMessage = true;
                mRef.child("break").setValue(breakLength * 60);

                if (mSessionTimerRunning) {
                    mSessionTimerPaused = true;
                    pauseTimer();
                }
                new CountDownTimer(breakLength*60*1000, 10) {
                    int totalTime = breakLength*60*1000;
                    TextView countdownTimer = findViewById(R.id.countdownTimer);
                    ProgressBar timerProgress = findViewById(R.id.timerProgress);

                    public void onTick(long millisUntilFinished) {
                        timerProgress.setMax(totalTime);
                        int hours = ((int) millisUntilFinished/1000) / 3600;
                        int secondsLeft = ((int) millisUntilFinished/1000) - hours * 3600;
                        int minutes = secondsLeft / 60;
                        int seconds = secondsLeft - minutes * 60;
                        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);

                        timerProgress.setProgress((int) millisUntilFinished);
                        countdownTimer.setText(formattedTime);
                    }

                    public void onFinish() {
                        timerProgress.setProgress(0);
                        countdownTimer.setText("00:00:00");
                        if (mSessionTimerPaused) {
                            mSessionTimerPaused = false;
                            startSessionTimer();
                        }

                        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("BREAK ENDED");
                        builder.setCancelable(true);
                        builder.setNeutralButton("Close", null);
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.parseColor("#00adb5"));
                        alertDialog.getWindow().getDecorView().getBackground().setColorFilter(new LightingColorFilter(0xFF000000, 0xFF3a4750));


                    }
                }.start();
            }

            }
        });

        alert.setNegativeButton("Cancel",new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {

            }
        });

        AlertDialog dialog = alert.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#00adb5"));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#00adb5"));
        dialog.getWindow().getDecorView().getBackground().setColorFilter(new LightingColorFilter(0xFF000000, 0xFF3a4750));
//        mSocket.emit("new message", "break");
    }

    public void startSessionTimer() {
        sessionTimer = new CountDownTimer(mSessionTimeLeftInMillis, 10) {
            @Override
            public void onTick(long millisUntilFinished) {
                mSessionTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                TextView countdownTimer = findViewById(R.id.countdownTimer);
                ProgressBar timerProgress = findViewById(R.id.timerProgress);
                countdownTimer.setText("00:00:00");
                timerProgress.setProgress(0);
                mSessionTimerRunning = false;
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(Html.fromHtml("<font color='#eeeeee'>SESSION ENDED</font>"));
                builder.setNeutralButton("Close", null);
                builder.setCancelable(true);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.parseColor("#00adb5"));
                alertDialog.getWindow().getDecorView().getBackground().setColorFilter(new LightingColorFilter(0xFF000000, 0xFF3a4750));

            }
        }.start();

        mSessionTimerRunning = true;

    }

    public void pauseTimer() {
        sessionTimer.cancel();
        mSessionTimerRunning = false;

    }

    public void updateCountDownText() {
        TextView text = findViewById(R.id.countdownTimer);
        ProgressBar timerProgress = findViewById(R.id.timerProgress);
        int hours = ((int) mSessionTimeLeftInMillis/1000) / 3600;
        int secondsLeft = ((int) mSessionTimeLeftInMillis/1000) - hours * 3600;
        int minutes = secondsLeft / 60;
        int seconds = secondsLeft - minutes * 60;
        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        text.setText(formattedTime);
        timerProgress.setMax(initialTime);
        timerProgress.setProgress((int) mSessionTimeLeftInMillis);

    }

    public void sendEnd(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(Html.fromHtml("<font color='#eeeeee'>Ending Session</font>"));
        alert.setMessage(Html.fromHtml("<font color='#eeeeee'>Do you wish to end the current session?</font>"));

        alert.setPositiveButton("Yes",new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog,int id)
            {
                if (mSessionTimerRunning) {
                    Toast.makeText(getApplicationContext(), "Session Ended", Toast.LENGTH_LONG).show();
                    internalMessage = true;
                    pauseTimer();
                    mRef.child("end_session").setValue(1);
                    mSessionTimerRunning = false;
                    TextView countdownTimer = findViewById(R.id.countdownTimer);
                    ProgressBar timerProgress = findViewById(R.id.timerProgress);
                    countdownTimer.setText("00:00:00");
                    timerProgress.setProgress(0);
                }

            }
        });

        alert.setNegativeButton("No",new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {

            }
        });
        AlertDialog dialog = alert.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#00adb5"));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#00adb5"));
        dialog.getWindow().getDecorView().getBackground().setColorFilter(new LightingColorFilter(0xFF000000, 0xFF3a4750));



    }


    @Override
    protected void onStart() {
        super.onStart();
        mCondition = mRef;
    }

    @Override
    protected void onStop() {
        super.onStop();
//        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//        builder.setMessage("Please start a break first");
//        builder.setCancelable(true);
//        AlertDialog alertDialog = builder.create();
//        alertDialog.show();

        if (!mSessionTimerPaused) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_1)
                    .setSmallIcon(R.drawable.ic_priority_high_black_24dp)
                    .setContentTitle("Work Discontinued")
                    .setContentText("Start a break before leaving the app")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(contentIntent);

            Intent launchIntent = new Intent(this, MainActivity.class);
            PendingIntent resumeIntent = PendingIntent.getActivity(
                    this, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.addAction(R.drawable.ic_menu_camera, "Launch", resumeIntent);

            NotificationManagerCompat manager = NotificationManagerCompat.from(this);
            manager.notify(0, builder.build());
        }
    }
}

package com.eddieyao.notes_app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String CHANNEL_1 = "channel1";
    public int sessionTime;

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://chat.socket.io");
        } catch (URISyntaxException e) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSocket.on("start_session", onNewSession);
        mSocket.connect();
        createNotificationChannel();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        ProgressBar timerProgress = findViewById(R.id.timerProgress);
        new CountDownTimer(90*1000, 10) {
            int totalTime = 90*1000;
            TextView countdownTimer = findViewById(R.id.countdownTimer);
            ProgressBar timerProgress = findViewById(R.id.timerProgress);

            public void onTick(long millisUntilFinished) {
                timerProgress.setMax(totalTime);
                int hours = ((int) millisUntilFinished/1000) / 3600;
                int secondsLeft = ((int) millisUntilFinished/1000) - hours * 3600;
                int minutes = secondsLeft / 60;
                int seconds = secondsLeft - minutes * 60;

                String formattedTime = "";
                if (hours < 10)
                    formattedTime += "0";
                formattedTime += hours + ":";

                if (minutes < 10)
                    formattedTime += "0";
                formattedTime += minutes + ":";

                if (seconds < 10)
                    formattedTime += "0";
                formattedTime += seconds ;
                timerProgress.setProgress((int) millisUntilFinished);
                countdownTimer.setText(formattedTime);
            }

            public void onFinish() {
                countdownTimer.setText("Session Ended");
            }
        }.start();
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

    private Emitter.Listener onNewSession = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    int seconds;
                    try {
                        seconds = data.getInt("duration");
                    } catch (JSONException e) {
                        return;
                    }
                    sessionTime = seconds;
                    new CountDownTimer(sessionTime*1000, 1000) {
                        TextView countdownTimer = findViewById(R.id.countdownTimer);
                        public void onTick(long millisUntilFinished) {
                            int hours = ((int) millisUntilFinished/1000) / 3600;
                            int secondsLeft = ((int) millisUntilFinished/1000) - hours * 3600;
                            int minutes = secondsLeft / 60;
                            int seconds = secondsLeft - minutes * 60;

                            String formattedTime = "";
                            if (hours < 10)
                                formattedTime += "0";
                            formattedTime += hours + ":";

                            if (minutes < 10)
                                formattedTime += "0";
                            formattedTime += minutes + ":";

                            if (seconds < 10)
                                formattedTime += "0";
                            formattedTime += seconds ;

                            countdownTimer.setText(formattedTime);
                        }

                        public void onFinish() {
                            countdownTimer.setText("Session Ended");
                        }
                    }.start();
                }
            });
        }
    };

    private void sendBreak(View view) {
        mSocket.emit("new message", "break");
    }

    private void sendEnd(View view) {
        mSocket.emit("new message", "end");
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_tools) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
//        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//        builder.setMessage("Please start a break first");
//        builder.setCancelable(true);
//        AlertDialog alertDialog = builder.create();
//        alertDialog.show();

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

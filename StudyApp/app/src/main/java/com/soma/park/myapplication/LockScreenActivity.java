package com.soma.park.myapplication;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.soma.park.myapplication.Activities.PasswordActivity;
import com.soma.park.myapplication.Elements.ReferenceMonitor;

import java.util.Calendar;

public class LockScreenActivity extends Activity {
    private static final String TAG = "LockScreen";
    private ReferenceMonitor referenceMonitor = ReferenceMonitor.getInstance();
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    ScreenService mScreenService;
    private AlarmManager alarmManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lockscreen);
        //Toast.makeText(this, "Activity : onCreate", Toast.LENGTH_SHORT).show();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        //FLAG_SHOW_WHEN_LOCKED: 기본 잠금화면 보다 위에 activity를 띄워라
        //FLAG_DISMISS_KEYGUARD: 기본 잠금화면을 없애라 -> KeyguardManager와 KeyguardLock 사용할 것

        startActivity(new Intent(this, Splash.class));

        pref = getSharedPreferences("pref", Activity.MODE_PRIVATE);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mScreenService = new ScreenService();

        if(pref.getInt("First", 0) != 1){
            Handler hd = new Handler();
            hd.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(LockScreenActivity.this, AccessTerms.class);
                    startActivityForResult(intent, 1);
                }
            }, 3000);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Toast.makeText(this, "Activity : onStart", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "reservState : " + String.valueOf(mScreenService.reservState));
        // service intent 를 만들고, startService 메소드를 사용합니다.
        // 이 메소드를 통해서 우리가 만든 서비스가 동작하게 됩니다.

        if(mScreenService.reservState) {
            Intent intent = new Intent(LockScreenActivity.this, ScreenService.class);
            startService(intent);
        }
        setContentView(R.layout.activity_lockscreen);

        Button settingButton = (Button) findViewById(R.id.setting_button);
        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pref.getBoolean("alarmstate", false)) {
                    Log.d(TAG, "예약 시간에는 설정 불가능!");
                } else {
                    Intent newintent = new Intent(LockScreenActivity.this, PasswordActivity.class);
                    newintent.putExtra("state", 2);
                    startActivityForResult(newintent, 2);
                }
            }
        });

        TextView lockText = (TextView) findViewById(R.id.lock_textview);
        lockText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                referenceMonitor.setStudymode();
                editor = pref.edit();
                editor.putBoolean("nowlock", true);
                editor.commit();

                Intent intent1 = new Intent(LockScreenActivity.this, AlarmStartReceiver.class);
                Calendar calendar1 = Calendar.getInstance();
                calendar1.set(Calendar.SECOND, 0);
                Log.d(TAG, String.valueOf(calendar1.getTime()));
                PendingIntent pIntent1 = PendingIntent.getBroadcast(LockScreenActivity.this, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {      //api 19 이상
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar1.getTimeInMillis(), pIntent1);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar1.getTimeInMillis(), pIntent1);
                }

                Intent intent2 = new Intent(LockScreenActivity.this, AlarmStopReceiver.class);
                Calendar calendar2 = Calendar.getInstance();
                calendar2.set(Calendar.MINUTE, (calendar2.get(Calendar.MINUTE)+1));
                calendar2.set(Calendar.SECOND, 0);
                Log.d(TAG, String.valueOf(calendar2.getTime()));
                PendingIntent pIntent2 = PendingIntent.getBroadcast(LockScreenActivity.this, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar2.getTimeInMillis(), pIntent2);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar2.getTimeInMillis(), pIntent2);
                }
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "onActivityResult");
        //super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case 1:
                if(resultCode == RESULT_OK) {
                    if(intent.getExtras().getInt("agree") == 0) {
                        finish();
                    }
                    if(intent.getExtras().getInt("agree") == 1) {

                        Intent newintent = new Intent(LockScreenActivity.this, PasswordActivity.class);
                        newintent.putExtra("state",0);
                        startActivity(newintent);
                    }
                }
                break;
            case 2:
                if(resultCode == RESULT_OK) {
                    if(intent.getExtras().getInt("validation") == 0) {
                        Toast.makeText(this,"암호가 올바르지 않습니다",Toast.LENGTH_SHORT).show();
                    }
                    if(intent.getExtras().getInt("validation") == 1) {
                        Intent newintent = new Intent(LockScreenActivity.this, SettingActivity.class);
                        startActivity(newintent);
                    }
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(referenceMonitor.getSTATE()==referenceMonitor.STUDYMODE || referenceMonitor.getSTATE()==referenceMonitor.INVALIDMODE) {
            referenceMonitor.setStudymode();
            Intent intent = new Intent(LockScreenActivity.this, ScreenService.class);
            startService(intent);
        }
    }
    //    @Override
//    public void onBackPressed() {
//        // Don't allow back to dismiss.
//        return;
//    }

//    protected void onUserLeaveHint() {
//        finish();
//        Intent intent = new Intent(this, ScreenService.class);
//        stopService(intent);
//        super.onUserLeaveHint();
//    }

//    @Override
//    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
//        if(keyCode == KeyEvent.KEYCODE_HOME) {
//            return false;
//        }
//        return true;
//    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        mHomeKeyLocker.unlock();
//        mHomeKeyLocker = null;
//    }

//    protected void onWindowVisibilityChanged (int visibility) {
//        mLauncher.onWindowVisibilityChanged(visibility);
//    }

//    public void onAttachedToWindow() {
//        this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG|
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        super.onAttachedToWindow();
//    } // 옛날 버전에서만 가능. home key disable 어떻게 ?

//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
//        super.onWindowFocusChanged(hasFocus);
//    }

}
package utehn.dev.mymobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {

    Handler handler;
    Runnable runnable;
    Long delay_time;
    Long time = 3000L;

    Button btnStart;
    EditText edtNumber, edtSec;
    String sn, sms;
    TextView tvSimSn;
    long sec;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        // init Widget ///
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(this);

        edtNumber = (EditText) findViewById(R.id.edtNumber);
        tvSimSn = (TextView) findViewById(R.id.tvSimSn);
        edtSec = (EditText) findViewById(R.id.edtSec);

        // end initWidget ///


        TelephonyManager telephoneMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        sn = telephoneMgr.getSimSerialNumber();
        tvSimSn.setText(sn);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String num = preferences.getString("sms", "");
        String str_sec = preferences.getString("sec", "");

        edtNumber.setText(num);
        edtSec.setText(str_sec);


    }// end OnCreate

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnStart) {
            sms = edtNumber.getText().toString();
            if (sms.equals(null) || sms.equals("")) {
                return;
            }

            if (edtSec.getText().toString().equals(null)) {
                sec = 60;
            } else {
                sec = Long.valueOf(edtSec.getText().toString());
            }


            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("sms", sms);
            editor.putString("sn", sn);
            editor.putString("sec", String.valueOf(sec));
            editor.commit();
            stopService(new Intent(this, MyService.class));
            startService(new Intent(this, MyService.class));
            finish();

        }
    }


}// end Class

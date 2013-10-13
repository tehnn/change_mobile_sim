package utehn.dev.mymobile;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;


public class MyService extends Service {

    String sms,sn_new,sn_old,op;
    Context context;


    LocationManager manager;
    LocationListener mlocListener;
    Double lat,lon;
    String pv;

    Long sec;


    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 10f;


    int noti_id=9988;
    NotificationManager mNM;

    private final IBinder mBinder = new LocalBinder();

    Timer timer ;


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        MyService getService() {
            return MyService.this;
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        this.context = this;
        timer = new Timer();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String str_sec=pref.getString("sec","");
        sec= Long.valueOf(str_sec);


       // showNotification();

        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mlocListener = new MyLocationListener();



        if(manager.isProviderEnabled( LocationManager.GPS_PROVIDER))
        {
            manager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, mlocListener);
        }else{
            manager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER, 0, 0, mlocListener);
        }

    }

    private void showNotification() {
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.ic_launcher,null,System.currentTimeMillis());
        notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        Intent noti = new Intent(this, MainActivity.class);
        noti.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,noti, 0);
        notification.setLatestEventInfo(this, "MyMobile","Monitoring.. Changed SIM", contentIntent);
        mNM.notify(noti_id, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        TelephonyManager telephoneMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        sn_new = telephoneMgr.getSimSerialNumber();


        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                if(manager.isProviderEnabled( LocationManager.GPS_PROVIDER))
                {
                    manager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, mlocListener);
                }else{
                    manager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER, 0, 0, mlocListener);
                }

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                sn_old = preferences.getString("sn","");
                sms = preferences.getString("sms","");
                String testSms = sms +","+sn_new + "," + sn_old+","+pv+","+lat+","+lon;
                Log.d("SIMM: ", sms +","+sn_new + "," + sn_old+","+pv+","+lat+","+lon);
                //Log.d("LatLon:",lat+","+lon);
                //sendSMS2(sms,testSms);

                if(!sn_new.equals(sn_old)){
                    Log.d("SIM:","sim is changed.");
                    TelephonyManager mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    GsmCellLocation loc = (GsmCellLocation) mTelephonyManager.getCellLocation();
                    String op = mTelephonyManager.getSimOperatorName();
                    String imei = mTelephonyManager.getDeviceId();
                    String cid = Integer.toString(loc.getCid());
                    String lac = Integer.toString(loc.getLac());

                    String sms_body = op+", IMEI="+imei+", CellID="+cid+", LAC="+lac;

                    //if(!lat.equals(null)){
                     sms_body=sms_body+" http://maps.google.com?q="+lat+","+lon+"";
                    //}

                    //Log.d("SIM info",sms_body);
                    //sendSMS(sms,sms_body);
                    sendSMS2(sms,sms_body);
                }
            }

        },sec*1000,sec*1000); //วินาที*มิลลิวินาที
        return Service.START_STICKY;
    }//end onStartCommand

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }// end onStart


    @Override
    public void onDestroy() {
        super.onDestroy();
        //mNM.cancel(noti_id);
       // Toast.makeText(getApplication(), "Stop Service",Toast.LENGTH_SHORT).show();
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
        Log.d("TAG", "Service is stopped..");

    }

    public class MyLocationListener implements LocationListener
    {
        public void onLocationChanged(Location location)
        {
            lat = location.getLatitude();
            lon = location.getLongitude();
            pv =location.getProvider();

        }

        public void onProviderDisabled(String provider){}

        public void onProviderEnabled(String provider){}

        public void onStatusChanged(String provider, int status, Bundle extras){}
    }



    //---sends an SMS message to another device---
    private void sendSMS(String phoneNumber, String message)
    {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                       // Toast.makeText(getBaseContext(), "SMS sent", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                       // Toast.makeText(getBaseContext(), "Generic failure",Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        //Toast.makeText(getBaseContext(), "No service",Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        //Toast.makeText(getBaseContext(), "Null PDU", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        //Toast.makeText(getBaseContext(), "Radio off",Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                       // Toast.makeText(getBaseContext(), "SMS is send.",Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        //Toast.makeText(getBaseContext(), "SMS not send.",Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);

    }
    //---sends an SMS message to another device---

    // send SMS 2


    private void sendSMS2(String phoneNumber, String message)
    {

        //   PendingIntent pi = PendingIntent.getActivity(this, 0,
        //       new Intent(this, Main.class), 0);
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }


    // end send SMS 2

}// end class

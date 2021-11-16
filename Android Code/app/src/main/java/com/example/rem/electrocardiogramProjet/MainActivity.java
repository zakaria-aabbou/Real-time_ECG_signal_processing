package com.example.rem.electrocardiogramProjet;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothSocket;

import android.content.Intent;
import android.content.pm.ActivityInfo;

import android.graphics.Color;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;



import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;

@SuppressLint("HandlerLeak")

// code updated by zakaria abbou
public class MainActivity extends Activity implements View.OnClickListener{

    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        if (Bluetooth.connectedThread != null) {
            Bluetooth.connectedThread.write("Q");}//Stop streaming
        super.onBackPressed();
    }

    //Les toggle boutons
    static boolean ferme;//whether lock the x-axis to 0-5
    static boolean AutoScrollX;//auto scroll to the last x value
    static boolean Stream;//Start or stop streaming

    int old_interval=0;
    int new_interval=0;
    int mean_interval=20;
    //Initialisation des boutons
    Button bXmoins;
    Button bXplus;
    ToggleButton tbFerme;
    ToggleButton tbScroll;
    ToggleButton tbStream;
    ToggleButton beep;
    //Initialisation de graphView
    static LinearLayout GraphView;
    static GraphView graphView;
    static GraphViewSeries Series;
    //graph value
    private static double graph2LastXValue = 0;
    private static int Xview=10;
    //Les boutons de connexion et déconnexion de bluetooth
    Button bConnect, bDisconnect;

    Handler mHandler = new Handler(){
        @TargetApi(Build.VERSION_CODES.ECLAIR)
        @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            switch(msg.what){
                case Bluetooth.SUCCESS_CONNECT:
                    Bluetooth.connectedThread = new Bluetooth.ConnectedThread((BluetoothSocket)msg.obj);
                    Toast.makeText(MainActivity.this, "Connected!", Toast.LENGTH_SHORT).show();
                    Bluetooth.connectedThread.start();
                    break;
                case Bluetooth.MESSAGE_READ:
                    if(tbStream.isChecked()){
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, 5);  // On cree une chaine de caracteres a partir du table bytes
                        String bpm=new String(readBuf, 0, 80);
                        String ibi=new String(readBuf, 0, 80);

                        if (strIncom.indexOf('.')==2 && strIncom.indexOf('s')==0){
                            strIncom = strIncom.replace("s", "");
                            if (isFloatNumber(strIncom)){
                                Series.appendData(new GraphViewData(graph2LastXValue,Double.parseDouble(strIncom)),AutoScrollX);
                                //Le control des axes d'absisces
                                if (graph2LastXValue >= Xview && ferme == true){
                                    Series.resetData(new GraphViewData[] {});
                                    graph2LastXValue = 0;
                                }else graph2LastXValue += 0.1;

                                if(ferme == true)
                                    graphView.setViewPort(0, Xview);
                                else
                                    graphView.setViewPort(graph2LastXValue-Xview, Xview);

                                GraphView.removeView(graphView);
                                GraphView.addView(graphView);

                            }
                        }

                        int temp1,temp2;
                        temp1=bpm.indexOf('b');
                        temp2=bpm.indexOf(',');
                        // Test sur la chaine de caractère qui contient la valeur de bpm
                        if(temp1>=0 && temp2>=temp1){
                            bpm = bpm.substring(temp1+1,temp2);
                            TextView BPM = (TextView) findViewById (R.id.BPM);
                            BPM.setText(bpm);
                            BPM.setBackgroundResource(R.drawable.heartbeat);
                            //beep
                            int beepBPM = Integer.parseInt(bpm);
                            if(beep.isChecked()){
                                if(beepBPM < 100){ // NE COMPTER QUE LES VALEURS DE BPM INF A 100
                                    try {
                                        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 70);
                                        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            }
                            //Pour le rythme cardiaque
                            if(beepBPM > 100){
                                TextView arrythmia = (TextView) findViewById (R.id.arrythmiaStatus);
                                arrythmia.setText("irrégulier");
                                arrythmia.setTextColor(Color.BLACK);
                                arrythmia.setBackgroundResource(R.color.red);

                            }else{
                                TextView arrythmia = (TextView) findViewById (R.id.arrythmiaStatus);
                                arrythmia.setText("régulier");
                                arrythmia.setTextColor(Color.BLACK);
                                arrythmia.setBackgroundResource(R.color.white);
                            }
                        }else{
                            TextView BPM = (TextView) findViewById (R.id.BPM);
                            BPM.setBackgroundResource(R.drawable.heartbeat2);
                        }
                        temp1=ibi.indexOf('i');
                        temp2=ibi.indexOf('e');
                        // Test sur la chaine de caractère qui contient la valeur de bpm
                        if(temp1>=0 && temp2>=temp1){
                            ibi = ibi.substring(temp1+1,temp2);
                            old_interval=new_interval;
                            new_interval=Integer.parseInt(ibi);
                            TextView IBI = (TextView) findViewById (R.id.IBI);
                            IBI.setText(ibi);
                            int timeDifference= java.lang.Math.abs(old_interval-new_interval);
                            mean_interval=(mean_interval+timeDifference)/2;
                            if(timeDifference>(mean_interval+200)){
                                TextView arrythmia = (TextView) findViewById (R.id.arrythmiaStatus);
                                arrythmia.setText("TRUE");
                                arrythmia.setBackgroundResource(R.color.white);

                            }else{
                                TextView arrythmia = (TextView) findViewById (R.id.arrythmiaStatus);
                                arrythmia.setText("FALSE"); //String.valueOf(mean_interval)
                                arrythmia.setBackgroundResource(R.color.black);
                            }
                            mean_interval=(mean_interval+timeDifference)/2;
                        }
                    }
                    break;
            }
        }
        // Fonction qui nous aide a détérminer est ce qu'un nombre supporte la conversion vers un double
        //Returne true or false
        public boolean isFloatNumber(String nombre){
            try{
                Double.parseDouble(nombre);
            } catch(NumberFormatException nfe) {
                return false;
            }
            return true;
        }
    };

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//Hide title
        this.getWindow().setFlags(WindowManager.LayoutParams.
                FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);//Hide Status bar
        setContentView(R.layout.activity_main);

        LinearLayout background = (LinearLayout)findViewById(R.id.bg);
        background.setBackgroundColor(Color.BLACK);

        init();
        ButtonInit();

    }

    void init(){

        //initialisation de graphview
        GraphView = (LinearLayout) findViewById(R.id.Graph);
        Series = new GraphViewSeries("Signal",
                new GraphViewStyle(Color.GREEN, 3),//La color est l'épaisseur de graph
                new GraphViewData[] {new GraphViewData(0, 0)});
        graphView = new LineGraphView(
                this // context
                , "Electrocardiogram Projet" //Titre de graph
        );
        graphView.setViewPort(0, Xview);
        graphView.setScrollable(true);
        graphView.setScalable(true);
        graphView.setShowLegend(true);
        graphView.setLegendAlign(LegendAlign.BOTTOM);
        graphView.setManualYAxis(true);
        graphView.setManualYAxisBounds(5, 0);
        graphView.addSeries(Series); // data
        GraphView.addView(graphView);
    }

    void ButtonInit(){
        //Initialisation de bouton Connect
        bConnect = (Button)findViewById(R.id.bConnect);
        bConnect.setOnClickListener(this);

        //Initialisation de bouton Disconnect
        bDisconnect = (Button)findViewById(R.id.bDisconnect);
        bDisconnect.setOnClickListener(this);

        //Le bouton de control d'axe d'absisce
        bXmoins = (Button)findViewById(R.id.bXminus);
        bXmoins.setOnClickListener(this);
        bXplus = (Button)findViewById(R.id.bXplus);
        bXplus.setOnClickListener(this);
        //
        tbFerme = (ToggleButton)findViewById(R.id.tbLock);
        tbFerme.setOnClickListener(this);
        tbScroll = (ToggleButton)findViewById(R.id.tbScroll);
        tbScroll.setOnClickListener(this);
        tbStream = (ToggleButton)findViewById(R.id.tbStream);
        tbStream.setOnClickListener(this);
        //init toggleButton
        ferme=true;
        AutoScrollX=true;
        Stream=true;
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch(v.getId()){
            // Le cas ou on clique sur le bouton Connect
            case R.id.bConnect:
                startActivity(new Intent("android.intent.action.BT1"));
                break;
            // Le cas ou on clique sur le bouton Disconnect
            case R.id.bDisconnect:
                Bluetooth.disconnect();
                break;
            case R.id.bXminus:
                if (Xview<30) Xview++;
                break;
            case R.id.bXplus:
                if (Xview>1) Xview--;
                break;
            case R.id.tbLock:
                if (tbFerme.isChecked()){
                    ferme = true;
                }else{
                    ferme = false;
                }
                break;
            case R.id.tbScroll:
                if (tbScroll.isChecked()){
                    AutoScrollX = true;
                }else{
                    AutoScrollX = false;
                }
                break;
            //case R.id.tbStream:
//                if (tbStream.isChecked()){
//                    if (Bluetooth.connectedThread != null)
//                        Bluetooth.connectedThread.write("E");
//                }else{
//                    if (Bluetooth.connectedThread != null)
//                        Bluetooth.connectedThread.write("Q");
//                }
                //break;
        }
    }




}
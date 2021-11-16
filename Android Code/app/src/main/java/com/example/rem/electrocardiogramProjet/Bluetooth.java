
package com.example.rem.electrocardiogramProjet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class Bluetooth extends Activity implements OnItemClickListener{

    public static void disconnect(){
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
    }

    public static void gethandler(Handler handler){//Bluetooth handler
        mHandler = handler;
    }
    static Handler mHandler = new Handler();

    static ConnectedThread connectedThread;
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    protected static final int SUCCESS_CONNECT = 0;
    protected static final int MESSAGE_READ = 1;

    //Définition des variables
    ArrayAdapter<String> listAdapter;
    ListView listView;
    static BluetoothAdapter btAdapter;
    Set<BluetoothDevice> devicesArray;
    ArrayList<String> pairedDevices;
    ArrayList<BluetoothDevice> devices;
    IntentFilter filter;
    BroadcastReceiver receiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},1);

        // On commence l'exuction de a fontion init()
        init();
        //On test est ce que le bluetooth est supporté ou non par le téléphone
        if (btAdapter==null){
            Toast.makeText(this, "No bluetooth detected", Toast.LENGTH_SHORT).show();
            finish();
        }else{ // Si il est supporté on test est ce qu'il est activer ou non
            if (!btAdapter.isEnabled()){
                // S'il n'est pas activé on exécute la fonction turnOnBT() pour l'activé
                turnOnBT();
            }
            // On cherche les dispositifs appairé avec notre téképhone
            getPairedDevices();
        }

    }

    private void startDiscovery() {
        // TODO Auto-generated method stub
        btAdapter.cancelDiscovery();
        btAdapter.startDiscovery();
    }

    //Fonction pour l'activation de Bluetooth
    private void turnOnBT() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, 1);
    }

    //Fonction pour la recherche des dispositifs appairé
    private void getPairedDevices() {
        devicesArray = btAdapter.getBondedDevices();
        if (devicesArray.size()>0){
            for(BluetoothDevice device:devicesArray){
                pairedDevices.add(device.getName());
            }
        }
    }

    // Fonction pour l'initialisation
    private void init(){

        listView = (ListView)findViewById(R.id.listView);
        listView.setOnItemClickListener(this);

        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,0);
        listView.setAdapter(listAdapter);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        pairedDevices = new ArrayList<String>();

        devices = new ArrayList<BluetoothDevice>();

        receiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                // Lorsque la fonction startDiscovery() a détecté un nouveau dispositif
                if (BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    devices.add(device);
                    String s = "";
                    for(int a=0;a<pairedDevices.size();a++){
                        if(device.getName()!=null && pairedDevices.get(a)!=null){
                            if (device.getName().equals(pairedDevices.get(a))){
                                //append
                                s = "(Paired)";
                                listAdapter.add(device.getName()+" "+s+" "+"\n"+device.getAddress());
                            }
                        }
                    }
                // Lorsque la fonction startDiscovery() a émet un intente avec l’action ‘ACTION_DISCOVERY_STARTED’
                }else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                    Toast.makeText(getApplicationContext(), "Searching for devices", Toast.LENGTH_SHORT).show();

                }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                    int count=listAdapter.getCount();
//                    Toast.makeText(getApplicationContext(), "Search finished and found "+count+" devices", Toast.LENGTH_SHORT).show();

                }else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                    if (btAdapter.getState() == btAdapter.STATE_OFF){
                        turnOnBT();
                    }
                }
            }

        };

        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        super.onPause();
        unregisterReceiver(receiver);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED){
            Toast.makeText(getApplicationContext(), "Bluetooth must be enabled to continue", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        // TODO Auto-generated method stub
        if (btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
        }
        if (listAdapter.getItem(arg2).contains("(Paired)")){

            BluetoothDevice selectedDevice = devices.get(arg2);
            ConnectThread connect = new ConnectThread(selectedDevice);
            connect.start();
        }else {
            Toast.makeText(this, "device is not paired", Toast.LENGTH_SHORT).show();
             }
    }

    //Thread qui gere la connexion
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // On utilise un objet temporaire, car l'objet mmSocket est finale
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Obtenir un client socket
            try {
                // MY_UUID est un identifiant universel unique pour le module Bluetooth qui on souhaite connecte avec
                // MY_UUID sert a identifier le socket client
                // tmp represente le client socket obtenue on executons la methode createRfcommSocketToServiceRecord()
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp; //stoquer le client socket dans  mmSocket
        }

        public void run() {
            // Annuler la recherche pour des nouveau dispositifs car il va rendre la connexion lente
            btAdapter.cancelDiscovery();

            try {
                // Etablir la connexion via le socket en utilisant la methode connect()
                mmSocket.connect();
            } catch (IOException connectException) {
                //Si la connexion a echouer, on ferme le socket
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }
            // On affiche un Toast dans MainActivity et on lance un autre thread via un Handler
            mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();
        }

        // Annluer les connexion en cours, et fermer le socket
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    //Thread qui gere les recoies des donnees via le bluetooth depuis le capteur et la carte Arduino
    static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // recoie et envoie des donnees en utilisant des objets temporaires
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        StringBuffer sbb = new StringBuffer();
        public void run() {

            byte[] buffer;  // buffer pour stoquer le flot des donnees
            int bytes; //  bytes returner par la methode read()
            // Ecoute de Inputstream jusqu'a une exception est leve
            while (true) {
                try {
                    try {
                        sleep(30);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    buffer = new byte[1024];
                    // Lire depuis InputStream
                    bytes = mmInStream.read(buffer);
                    // Envoyer les bytes recuperer vers MainActivity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String income) {

            try {
                mmOutStream.write(income.getBytes());
                for(int i=0;i<income.getBytes().length;i++)
                    Log.v("outStream"+Integer.toString(i),Character.toString((char)(Integer.parseInt(Byte.toString(income.getBytes()[i])))));
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.d("asd", "granted");
                }
                return;
            }
        }
    }

}
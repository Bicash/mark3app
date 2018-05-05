package com.toolpar.mark3app;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity implements MqttCallback, View.OnClickListener{
    private String server = "tcp://cubics.io:1883";
    private MqttClient client;
    private String TAG = "MARK III";
    private String postboxCode = "ESIL/";
    private LinearLayout boxes;
    //    Topics
    private String boxGroups = "box-groups/";
    private String boxGroupsStatus = boxGroups + "status";
    private String boxGroupsOpenTopic = boxGroups + postboxCode + "open";
    private String boxGroupsInfoTopic = boxGroups + postboxCode + "info";
    private String boxGroupsCalibrateBoxesTopic = boxGroups + postboxCode + "calibrate";
    private String boxGroupsCalibrateBoxes = boxGroups + postboxCode + "calibrateBoxes";
    private String boxGroupsCalibrateInfoTopic = boxGroups + postboxCode + "calibrateInfo";
    private String boxGroupsCalibrateStopTopic = boxGroups + postboxCode + "calibrateStop";

    //    Init UI
    private TextView postboxState;
    private TextView postboxMcode;
    private TextView calibrateInfo;
    private Button postboxCalibrate;
    private Button calibrateStop;
    private Button getInfo;
    private LinearLayout.LayoutParams lParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        layout initialize
        boxes = findViewById(R.id.boxes);
        lParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lParams.setMargins( 8, 8, 8, 8);
//        components initialize
        postboxState = findViewById(R.id.postboxState);
        postboxMcode = findViewById(R.id.postboxMcode);
        calibrateInfo = findViewById(R.id.calibrateInfo);
        postboxCalibrate = findViewById(R.id.postboxCalibrate);
        calibrateStop = findViewById(R.id.calibrateStop);
        getInfo = findViewById(R.id.getInfo);
        postboxCalibrate.setOnClickListener(this);
        calibrateStop.setOnClickListener(this);
        getInfo.setOnClickListener(this);
        try {
            connectToMqtt();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void connectToMqtt() throws MqttException {
        client = new MqttClient(server, postboxCode, new MemoryPersistence());//задаем сервер и clientid
        client.setCallback(MainActivity.this);//Установка callback на активити
        if (!client.isConnected()) {
            client.connect();//соединение с сервером
            Log.i(TAG, "connected to cubics");
            client.subscribe(boxGroupsStatus);
            Log.i(TAG, "Subscribed to topic: " + boxGroupsStatus);
            client.subscribe(boxGroupsCalibrateBoxes);
            Log.i(TAG, "Subscribed to topic: " + boxGroupsCalibrateBoxes);
            client.subscribe(boxGroupsCalibrateInfoTopic);
            Log.i(TAG, "Subscribed to topic: " + boxGroupsCalibrateInfoTopic);
            Toast.makeText(this, "Connected to cubics", Toast.LENGTH_SHORT).show();
        }
    }

    private void publishJSONMessage(JSONObject mes, String topic) throws MqttException, UnsupportedEncodingException {
        String msg = mes.toString();
        byte[] encodedPayload;
        encodedPayload = msg.getBytes("UTF-8");
        MqttMessage message = new MqttMessage(encodedPayload);
        client.publish(topic, message);
        Log.i(TAG, "publishMessage to topic: " + topic + ", message: " + message);
    }

    private void publishMessage(String mes, String topic) throws MqttException, UnsupportedEncodingException {
        String msg = mes.toString();
        byte[] encodedPayload;
        encodedPayload = msg.getBytes("UTF-8");
        MqttMessage message = new MqttMessage(encodedPayload);
        client.publish(topic, message);
        Log.i(TAG, "publishMessage to topic: " + topic + ", message: " + message);
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.i(TAG, "connectionLost....");
        runOnUiThread(new Runnable(){
            public void run() {
                Toast.makeText(MainActivity.this, "Connection Lost", Toast.LENGTH_SHORT).show();
            }
        });
        try {
            connectToMqtt();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void messageArrived(String topic, final MqttMessage message) throws Exception {
        final String payload = new String(message.getPayload());
        Log.i(topic, payload);
        if (topic.equals(boxGroupsStatus)){
            final JSONObject result = new JSONObject(payload);
            final String status = result.getString("status");
            final String code = result.getString("code");
            runOnUiThread(new Runnable(){
                public void run() {
                    postboxMcode.setText(code);
                    postboxMcode.setTextColor(getResources().getColor(R.color.colorPrimary));
                }
            });
            if (status.equals("ON")){
                runOnUiThread(new Runnable(){
                    public void run() {
                        postboxState.setText(status);
                        postboxState.setTextColor(getResources().getColor(R.color.stateON));
                    }
                });
            }
            boolean calibrate = result.getBoolean("calibrate");
            if (calibrate){
                runOnUiThread(new Runnable(){
                    public void run() {
                        calibrateInfo.setText("MARK III needs calibrate, pleese click for calibrate");
                    }
                });
            }
            if (!calibrate){
                runOnUiThread(new Runnable(){
                    public void run() {
                        calibrateInfo.setText("MARK III Calibrated");
                    }
                });
            }
        }
        if (topic.equals(boxGroupsCalibrateInfoTopic)){
            runOnUiThread(new Runnable(){
                public void run() {
                    calibrateInfo.setText(payload);
                }
            });
        }
        if (topic.equals(boxGroupsCalibrateBoxes)){
            runOnUiThread(new Runnable(){
                public void run() {
                    Toast.makeText(MainActivity.this, payload, Toast.LENGTH_SHORT).show();
                    try {
                        JSONObject result = new JSONObject(payload);
                        String box = result.getString("code");
                        String size = result.getString("size");
                        int boxId = result.getInt("boxId");
                        Button btn = new Button(MainActivity.this);
                        btn.setId(boxId);
                        btn.setText(box + ", size " + size);
                        btn.setTextColor(Color.WHITE);
                        btn.setBackgroundColor(getResources().getColor(R.color.stateON));
                        btn.setOnClickListener(MainActivity.this);
                        boxes.addView(btn, lParams);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.i(TAG, "deliveryComplete....");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.getInfo:
                try {
                    publishMessage("getInfo", boxGroupsInfoTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.postboxCalibrate:
                try {
                    publishMessage("calibrate", boxGroupsCalibrateBoxesTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.calibrateStop:
                try {
                    publishMessage("End calibrate", boxGroupsCalibrateStopTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 1:
                try {
                    publishMessage("box1", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 2:
                try {
                    publishMessage("box2", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 3:
                try {
                    publishMessage("box3", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 4:
                try {
                    publishMessage("box4", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 5:
                try {
                    publishMessage("box5", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 6:
                try {
                    publishMessage("box6", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 7:
                try {
                    publishMessage("box7", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 8:
                try {
                    publishMessage("box8", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 9:
                try {
                    publishMessage("box9", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 10:
                try {
                    publishMessage("box10", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 11:
                try {
                    publishMessage("box11", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 12:
                try {
                    publishMessage("box12", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 13:
                try {
                    publishMessage("box13", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 14:
                try {
                    publishMessage("box14", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 15:
                try {
                    publishMessage("box15", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 16:
                try {
                    publishMessage("box16", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 17:
                try {
                    publishMessage("box17", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 18:
                try {
                    publishMessage("box18", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 19:
                try {
                    publishMessage("box19", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 20:
                try {
                    publishMessage("box20", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 21:
                try {
                    publishMessage("box21", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 22:
                try {
                    publishMessage("box22", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 23:
                try {
                    publishMessage("box23", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 24:
                try {
                    publishMessage("box24", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 25:
                try {
                    publishMessage("box25", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 26:
                try {
                    publishMessage("box26", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 27:
                try {
                    publishMessage("box27", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 28:
                try {
                    publishMessage("box28", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 29:
                try {
                    publishMessage("box29", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 30:
                try {
                    publishMessage("box30", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 31:
                try {
                    publishMessage("box31", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 32:
                try {
                    publishMessage("box32", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 33:
                try {
                    publishMessage("box33", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 34:
                try {
                    publishMessage("box34", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 35:
                try {
                    publishMessage("box35", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 36:
                try {
                    publishMessage("box36", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 37:
                try {
                    publishMessage("box37", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 38:
                try {
                    publishMessage("box38", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 39:
                try {
                    publishMessage("box39", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 40:
                try {
                    publishMessage("box40", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 41:
                try {
                    publishMessage("box41", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 42:
                try {
                    publishMessage("box42", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 43:
                try {
                    publishMessage("box43", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 44:
                try {
                    publishMessage("box44", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 45:
                try {
                    publishMessage("box45", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 46:
                try {
                    publishMessage("box46", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 47:
                try {
                    publishMessage("box47", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 48:
                try {
                    publishMessage("box48", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 49:
                try {
                    publishMessage("box49", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 50:
                try {
                    publishMessage("box50", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 51:
                try {
                    publishMessage("box51", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 52:
                try {
                    publishMessage("box52", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 53:
                try {
                    publishMessage("box53", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 54:
                try {
                    publishMessage("box54", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 55:
                try {
                    publishMessage("box55", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 56:
                try {
                    publishMessage("box56", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 57:
                try {
                    publishMessage("box57", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 58:
                try {
                    publishMessage("box58", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 59:
                try {
                    publishMessage("box59", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 60:
                try {
                    publishMessage("box60", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 61:
                try {
                    publishMessage("box61", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 62:
                try {
                    publishMessage("box62", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 63:
                try {
                    publishMessage("box63", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case 64:
                try {
                    publishMessage("box64", boxGroupsOpenTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}

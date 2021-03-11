package ru.mofrison.mqttclient;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity implements MQTTClient.EventHandler {
    TextView dataReceived;
    MQTTClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataReceived =  (TextView) findViewById(R.id.dataReceived);

        mqttClient = new MQTTClient(getApplicationContext(), "tcp://192.168.1.11:1883", null);
    //    mqttClient.SetMqttEventHandler(this);

        mqttClient.Connect("username", "password", this);
    }

    @Override
    public void onStart(){
        super.onStart();
    }

    @Override
    public void OnConnectedMQTT() {
        Toast.makeText(this, "Connect!", Toast.LENGTH_SHORT).show();

        mqttClient.Subscribe("mqttTest", 1, this);
        mqttClient.Publish("mqttTest", Build.MANUFACTURER + " " + Build.PRODUCT + " connected", this);
    }

    @Override
    public void OnReceivingMQTTMessage(String topic, MqttMessage message) {
        dataReceived.setText(topic + ": " + message.toString());
    }

    @Override
    public void OnSuccessMQTT(String info) {
        Log.i("mqttTest", info);
    }

    @Override
    public void OnFailureMQTT(String error) {
        Log.w("mqttTest", error);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Closing the connection when exiting
        mqttClient.Unsubscribe("mqttTest", this);
        mqttClient.Disconnect(this);
    }
}
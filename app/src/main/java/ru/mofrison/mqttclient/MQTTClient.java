package ru.mofrison.mqttclient;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

public class MQTTClient
{
    private final MqttAndroidClient client;

    public interface EventHandler {
        void OnConnectedMQTT();
        void OnReceivingMQTTMessage(String topic, MqttMessage message);
        void OnSuccessMQTT(String error);
        void OnFailureMQTT(String error);
    }

    public MQTTClient (@NonNull final Context context, String serverURI, String clientId) {
        if(clientId == null || clientId.isEmpty()) { clientId = MqttClient.generateClientId(); }
        client = new MqttAndroidClient(context, serverURI, clientId);
    }

    public void Connect(String userName, String password, @NonNull EventHandler handler)
    {
        if(client.getServerURI().isEmpty()){
            handler.OnFailureMQTT("Connection failed! URI is empty. Check your connection settings!");
            return;
        }

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(userName);
        options.setPassword(password.toCharArray());

        client.connect(options, new IMqttActionListener(){
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                // We are connected
                handler.OnSuccessMQTT("Connected to " + client.getServerURI());
                handler.OnConnectedMQTT();
            }
            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                // Something went wrong e.g. connection timeout or firewall problems
                handler.OnFailureMQTT("Connection failed! Check your connection settings!");
            }
        });
    }

    public void Subscribe(@NonNull String topic, int qos, @NonNull EventHandler handler)
    {
        if(!client.isConnected() || topic.isEmpty()) {
            handler.OnFailureMQTT("Failed subscribing to " + topic + " Client is " + client.isConnected());
            return;
        }

        client.subscribe(topic, qos, new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                handler.OnReceivingMQTTMessage(topic, message);
                Log.i(topic, message.toString());
            }
        });
    }

    public void Unsubscribe(@NonNull String topic, @Nullable EventHandler handler)
    {
        if((!client.isConnected() || topic.isEmpty()) && handler != null) {
            handler.OnFailureMQTT("Unsubscribe " + topic + " is failed. Client is " + client.isConnected());
            return;
        }

        IMqttToken unsubToken = client.unsubscribe(topic);
        unsubToken.setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                // The subscription could successfully be removed from the client
               handler.OnSuccessMQTT("Unsubscribe " + topic);
            }
            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                // some error occurred, this is very unlikely as even if the client
                // did not had a subscription to the topic the unsubscribe action
                // will be successfully
                handler.OnFailureMQTT("Unsubscribe " + topic + " is failed.");
            }
        });
    }

    public void Publish(String topic, String payload, @NonNull EventHandler handler) {
        if(!client.isConnected() || topic.isEmpty() || payload.isEmpty()) {
            handler.OnFailureMQTT("Message: " + payload +" is not published in topic " + topic);
            return;
        }
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            //message.setRetained(true); // To send a retained MQTT message
            client.publish(topic, message);
            handler.OnSuccessMQTT("Message is published");
        } catch (UnsupportedEncodingException e) {
            handler.OnFailureMQTT("Message is not published: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void SetMqttEventHandler(@NonNull EventHandler handler) {

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                if(handler != null && cause !=null ) {
                    handler.OnFailureMQTT("New message is not received: " + cause.getMessage());
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                handler.OnReceivingMQTTMessage(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                handler.OnSuccessMQTT("New message received");
            }
        });
    }

    public void Disconnect(@Nullable EventHandler handler) {
        if (client.isConnected()) {
            IMqttToken disconToken = client.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // we are now successfully disconnected
                    if(handler != null) {
                        handler.OnSuccessMQTT("Message is received");
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // something went wrong, but probably we are disconnected anyway
                    if(handler != null) {
                        handler.OnFailureMQTT("Couldn't disconnect from server");
                    }
                }
            });
        }
    }
}

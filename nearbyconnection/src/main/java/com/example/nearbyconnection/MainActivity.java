package com.example.nearbyconnection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {

    private static final String SERVICE_ID = "com.example.nearbyconnection";
    private static final String TAG = "Things:";
    private final String PIN_LED = "BCM18";
    public Gpio mLedGpio;
    private Boolean ledStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ledStatus = false;
        PeripheralManager service = PeripheralManager.getInstance();
        try {
            mLedGpio = service.openGpio(PIN_LED);
            mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e) {
            Log.e(TAG, "Error en el API PeripheralIO", e);
        }
        startAdvertising();
    }

    private void startAdvertising() {
        Nearby.getConnectionsClient(this).startAdvertising(
                "Nearby LED", SERVICE_ID, mConnectionLifecycleCallback, new AdvertisingOptions(Strategy.P2P_STAR)
        ).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Error al comenzar el modo anunciante", e);
            }
        });
    }

    private void stopAdvertising() {
        Nearby.getConnectionsClient(this).stopAdvertising();
        Log.i(TAG, "Deteniendo el modo anunciante");
    }

    private final ConnectionLifecycleCallback mConnectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
            Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(endpointId, mPayloadCallback);
            Log.i(TAG, "Aceptando conexión entrante sin autenticación");
        }

        @Override
        public void onConnectionResult(String endpointId, ConnectionResolution result) {
            switch (result.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    Log.i(TAG, "Estamos conectados");
                    stopAdvertising();
                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    Log.i(TAG, "Conexión rechazada por unoo ambos lados");
                    break;
                case ConnectionsStatusCodes.STATUS_ERROR:
                    Log.i(TAG, "Conexión perdida antes de ser aceptada");
                    break;
            }
        }

        @Override
        public void onDisconnected(String s) {
            Log.i(TAG, "Desconexión del endpoint, no se pueden intercambiar datos");
            startAdvertising();
        }
    };

    private final PayloadCallback mPayloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            String message = new String(payload.asBytes());
            Log.i(TAG, "se ha recibido una transferencia desde (" + endpointId+") con el siguiente contenido: " + message);
            disconnect(endpointId);
            switch(message){
                case "SWITCH":
                    switchLED();
                    break;
                default:
                    Log.w(TAG, "No existe una acción asociada a este mensaje ");
                    break;
            }
        }

        @Override
        public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    public void switchLED(){
        try{
            if(ledStatus){
                mLedGpio.setValue(false);
                ledStatus = false;
                Log.i(TAG, "LED OFF");
            }else{
                mLedGpio.setValue(true);
                ledStatus = true;
                Log.i(TAG, "LED ON");
            }
        }catch(IOException e){
            Log.e(TAG, "Error en el API PeripheralIO", e);
        }
    }

    protected void disconnect(String endpointId){
        Nearby.getConnectionsClient(this).disconnectFromEndpoint(endpointId);
        Log.i(TAG, "Desconectado del endPoint("+  endpointId+").");
    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        stopAdvertising();
        if(mLedGpio != null){
            try{
                mLedGpio.close();
            }catch(IOException e){
                Log.e(TAG, "Error en el API PeripheralIO", e);
            }finally{
                mLedGpio = null;
            }
        }
    }

}

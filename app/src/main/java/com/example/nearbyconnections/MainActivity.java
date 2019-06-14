package com.example.nearbyconnections;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
    private static final String SERVICE_ID="com.example.nearbyconnections";
    private static final String TAG = "Mobile:  ";

    Button botonLED;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.textView1);
        botonLED = (Button) findViewById(R.id.buttonLED);

        botonLED.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Boton presionado");
                startDiscovery();
                textView.setText("Buscando... ");
            }
        });

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSONS_REQUEST_ACCESS_COARSE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],int[] grantREsults){
        switch(requestCode){
            case MY_PERMISSONS_REQUEST_ACCESS_COARSE_LOCATION:{
                if(grantREsults.length > 0 && grantREsults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.i(TAG, "Permiso concedido");
                }else{
                    Log.i(TAG,"permiso negado");
                    textView.setText("Debe aceptar los permisos para comenzar");
                    botonLED.setEnabled(false);
                }
                return;
            }
        }
    }

    public void startDiscovery(){
        Nearby.getConnectionsClient(this).startDiscovery(SERVICE_ID, mEndpointDiscoveryCallback, new DiscoveryOptions(Strategy.P2P_STAR))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Estamos en modo descubrimiento");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Modo descubrimiento no iniciado", e);
                    }
                });
    }

    private void stopDiscovery(){
        Nearby.getConnectionsClient(this).stopDiscovery();
        Log.i(TAG, "Se ha detenido el modo descubrimiento");
    }

    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback= new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
            Log.i(TAG, "Descubierto dispositivo con id: "+endpointId);
            textView.setText("Descubierto: "+discoveredEndpointInfo.getEndpointName());
            stopDiscovery();
            Log.i(TAG, "Conectando...");
            Nearby.getConnectionsClient(getApplicationContext()).requestConnection("Nearby LED", endpointId, mConnectionLifecycleCallback)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Error en solicitud de conexión", e);
                            textView.setText("desconectado");
                        }
                    });
        }

        @Override
        public void onEndpointLost(String s) {

        }
    };

    private final ConnectionLifecycleCallback mConnectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String enpointId, ConnectionInfo connectionInfo) {
            Log.i(TAG,"Aceptando conexión entrante sin autenticación");
            Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(enpointId, mPayloadCallback);
        }

        @Override
        public void onConnectionResult(String endpointId, ConnectionResolution result) {
            switch(result.getStatus().getStatusCode()){
                case ConnectionsStatusCodes.STATUS_OK:
                    Log.i(TAG, "Estamos Conectados");
                    textView.setText("Conectado");
                    sendData(endpointId, "SWITCH");
                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    Log.i(TAG, "Conexión rechazada por uno o ambos lados");
                    textView.setText("Desconectado");
                    break;
                case ConnectionsStatusCodes.STATUS_ERROR:
                    Log.i(TAG, "Conexión perdida antes de poder ser aceptada");
                    textView.setText("Desconectado");
                    break;
            }
        }

        @Override
        public void onDisconnected(String s) {
            Log.i(TAG, "Desconexión del endpoint, no se pueden intercambiar datos");
            textView.setText("Desconectado");
        }
    };

    private final PayloadCallback mPayloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {

        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    private void sendData(String endpointId, String mensaje){
        textView.setText("Transfiriendo.....");
        Payload data= null;
        try{
            data= Payload.fromBytes(mensaje.getBytes("UTF-8"));
        }catch(UnsupportedEncodingException e){
            Log.e(TAG, "Error en la codificación del mensaje", e);
        }
        Nearby.getConnectionsClient(this).sendPayload(endpointId, data);
        Log.i(TAG,"Mensaje enviado");
    }

}

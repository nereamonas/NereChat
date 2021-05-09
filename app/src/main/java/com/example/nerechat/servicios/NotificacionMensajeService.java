package com.example.nerechat.servicios;

import android.app.NotificationChannel;
import android.app.NotificationManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import com.example.nerechat.MainActivity;
import com.example.nerechat.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class NotificacionMensajeService extends FirebaseMessagingService{

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage){
        super.onMessageReceived(remoteMessage);
        Log.d("Logs","Tamaño data recibido: ");
        if (remoteMessage.getData().size() > 0) {
            Log.d("Logs","Tamaño data recibido: "+remoteMessage.getData().size());
        }
        if (remoteMessage.getNotification() != null) { //cogemos la notif del mensaje. si es distinto de null
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this); //Cogemos las preferencias
            if (prefs.contains("notif")) { //Comprobamos si existe notif
                Boolean activadas = prefs.getBoolean("notif", true);  //Comprobamos si las notificaciones estan activadas
                Log.d("Logs", "estado notificaciones: " + activadas);
                if (activadas) { //Si tenemos las notif activadas, lanzamos la notificacion
                    NotificationManager elManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    NotificationCompat.Builder elBuilder = new NotificationCompat.Builder(this, "IdCanal");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel elCanal = new NotificationChannel("IdCanalRecordatorioDiario", "NombreCanal",
                                NotificationManager.IMPORTANCE_DEFAULT);
                        elManager.createNotificationChannel(elCanal);
                    }
                    Intent intent = null;

                    if (remoteMessage.getData().get("type").equals("sms")) {
                        intent = new Intent(this, MainActivity.class);
                        intent.putExtra("abrir", "chat");
                        intent.putExtra("usuario", remoteMessage.getData().get("userID"));
                    }

                    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    elBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_chat))
                            .setSmallIcon(R.drawable.ic_chat)
                            .setContentTitle(remoteMessage.getNotification().getTitle()) //Cogemos el titulo
                            .setContentText(remoteMessage.getNotification().getBody()) //Cogemos el cuerpo
                            .setVibrate(new long[]{0, 1000, 500, 1000})
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent);

                    elManager.notify(1, elBuilder.build());
                    Log.d("Logs", "Mensaje: Notificacion recibida: " + remoteMessage.getNotification().getBody());
                }
            }
        }
    }
}

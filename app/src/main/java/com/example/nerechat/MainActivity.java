package com.example.nerechat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.example.nerechat.base.BaseActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends BaseActivity {

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_chatsamigos, R.id.navigation_mapa, R.id.navigation_fotos, R.id.navigation_perfil)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        mAuth= FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser(); //El usuario actual que tiene la sesion iniciada
        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Perfil"); //La base de datos perfil


        mDatabaseRef.child(mUser.getUid()).child("conectado").setValue("Conectado");

        FirebaseMessaging.getInstance().subscribeToTopic(mUser.getUid()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("Logs","SUSCRITO EL USUARIO A NOTIFICACIONES");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                Log.d("Logs","NO SE HA PODIDO SUSCRIBIR EL USUARIO A NOTIFIACIONES");
            }
        });
        Log.d("Logs","NO SE HA PODIDO SUSCRIBIR EL USUARIO A NOTIFIACIONESxxxxxxxxxxxxxxxxxxxxxxxxx");

        //Poner visible la navegacion de abajo
        BottomNavigationView nv= findViewById(R.id.nav_view);
        nv.setVisibility(View.VISIBLE);
        //Ocultar el action bar x defecto
        getSupportActionBar().hide();

        //cambiarEstado("Conectado");
        //Controlaremos todos los botones del menu
        navView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.navigation_chatsamigos) {  //Si clicamos en rutinas, abriremos con navigation, la ventana donde se muestran las rutinas. he igual con todos
                    Log.d("Logs", "navigation_chatsamigos");
                    abrir_chatAmigos();
                } else if (id==R.id.navigation_mapa){
                    Log.d("Logs", "navigation_mapa");
                    abrir_mapa();
                }else if (id == R.id.navigation_fotos) {
                    Log.d("Logs", "navigation_fotos");
                    abrir_fotos();
                }else if (id==R.id.navigation_perfil){
                    Log.d("Logs", "navigation_perfil");
                    abrir_perfil();
                }
                return true;
            }
        });

        if (getIntent().hasExtra("abrir")) {
            String abrir = getIntent().getExtras().getString("abrir");
            if (abrir.equals("chat")){

                String usuario = getIntent().getExtras().getString("usuario");
                abrir_chat(usuario);
            }else if(abrir.equals("ajustes")){
                abrir_ajustes();
            }
        }


    }

    public void abrir_chat(String s){
        Bundle bundle = new Bundle();
        bundle.putString("usuario", s);
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build();
        Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.action_global_chatFragment, bundle,options);
    }

    public void abrir_ajustes(){
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build();
        Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.action_global_ajustesFragment,null,options);
    }

    public void abrir_mapa(){
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build();
        Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.action_global_navigation_mapa,null,options);
    }
    public void abrir_chatAmigos(){
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build();
        Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.action_global_navigation_chatsamigos,null,options);
    }

    public void abrir_fotos(){
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build();
        Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.action_global_navigation_fotos,null,options);

    }

    public void abrir_perfil(){
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build();
        Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.action_global_navigation_perfil,null,options);

    }
/*
    public void cambiarEstado(String estado){
        mDatabaseRef.child(mUser.getUid()).child("conectado").setValue(estado);
    }
    @Override
    public void onBack() {
        mDatabaseRef.child(mUser.getUid()).child("conectado").setValue("No está conectado");
        super.onStop();

    }

@Override
public void onBackPressed() {
        mDatabaseRef.child(mUser.getUid()).child("conectado").setValue("No está conectado");

   super.onBackPressed();

}

    @Override
    public void onResume() {
        super.onResume();
        mDatabaseRef.child(mUser.getUid()).child("conectado").setValue("Conectado");

    }
*/
    @Override
    public void onStop() {
        super.onStop();
        String texto="";
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this); //Cogemos las preferencias
        if (prefs.contains("ultimaHora")) { //Comprobamos si existe notif
            Boolean bloqueado = prefs.getBoolean("ultimaHora", true);  //Comprobamos si las notificaciones estan activadas
            Log.d("Logs", "estado visulizar ultimaHora: " + bloqueado);
            if (!bloqueado) { //Si tenemos las notif activadas, lanzamos la notificacion
                Date date = new Date();
                SimpleDateFormat hora = new SimpleDateFormat("HH:mm");
                texto = "Última vez a las " + hora.format(date);
            }
        }

        mDatabaseRef.child(mUser.getUid()).child("conectado").setValue(texto);

    }

}
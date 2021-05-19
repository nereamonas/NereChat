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

    //La actividad principal, que se encarga ded navegar por los diferentes fragmentos del menu

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Tendremos un bottonnavigationview con 4 destinos. los chats, el mapa, las fotos y la info del perfil
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_chatsamigos, R.id.navigation_mapa, R.id.navigation_fotos, R.id.navigation_perfil)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        //Hasieratuamos las instancias de firebase que necesitaremos
        mAuth= FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser(); //El usuario actual que tiene la sesion iniciada
        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Perfil"); //La base de datos perfil

        //Cuando abrimos este fragment tenemos que cambiar en firebase en la informacion de perifl nuestro estado a conectados para que el resto de usuarios vea que tenemos
        //la aplicacion abierta y estamos disponibles.
        mDatabaseRef.child(mUser.getUid()).child("conectado").setValue("Conectado");

        FirebaseMessaging.getInstance().subscribeToTopic(mUser.getUid()); //Suscribimos el usuario para que reciba notificaciones. su topic será su uid

        //Poner visible la navegacion de abajo
        BottomNavigationView nv= findViewById(R.id.nav_view);
        nv.setVisibility(View.VISIBLE);
        //Ocultar el action bar x defecto
        getSupportActionBar().hide();

        //Controlaremos todos los botones del menu
        navView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.navigation_chatsamigos) {  //Si clicamos en chats, abriremos con navigation, la ventana donde se muestran los chats
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

        //Cuando venga de una notificacion de mensaje o se ha recargado la pagina desde ajustes, tendrá informado en los argumentos que le llegan la variable abrir. en el caso de ser asi:
        if (getIntent().hasExtra("abrir")) {
            String abrir = getIntent().getExtras().getString("abrir");
            if (abrir.equals("chat")){ //Opcion 1, que tengamos q abrir el chat, de ser asi viene de una notificacion y tambien trae informado el usuario
                String usuario = getIntent().getExtras().getString("usuario");
                abrir_chat(usuario);
            }else if(abrir.equals("ajustes")){ //Opcion 2 quiere q se abra ajustes
                abrir_ajustes();
            }else if(abrir.equals("fotos")){
                abrir_fotos();
            }
        }

    }

    public void abrir_chat(String s){ //Abrimos el chat, como tenemos el uid del usuario se abrira sin problema
        Bundle bundle = new Bundle();
        bundle.putString("usuario", s);
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build();
        Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.action_global_chatFragment, bundle,options);
    }

    public void abrir_ajustes(){//Abrimos la ventana de ajustes
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build();
        Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.action_global_ajustesFragment,null,options);
    }

    public void abrir_mapa(){//Abrimos la ventana de mapa
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build();
        Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.action_global_navigation_mapa,null,options);
    }
    public void abrir_chatAmigos(){//Abrimos la ventana de los chats con amigos
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build();
        Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.action_global_navigation_chatsamigos,null,options);
    }

    public void abrir_fotos(){//Abrimos la ventana de fotos
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build();
        Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.action_global_navigation_fotos,null,options);

    }

    public void abrir_perfil(){//Abrimos la ventana de perfil
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build();
        Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.action_global_navigation_perfil,null,options);

    }

    //Cuando se cierre la ventana ya no estaremos conectamos entonces en firebase tenemos que guardar nuestra ultima hora
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
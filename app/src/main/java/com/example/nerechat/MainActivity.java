package com.example.nerechat;

import android.app.ActivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_chatsamigos, R.id.navigation_fotos, R.id.navigation_perfil)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        mAuth= FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser(); //El usuario actual que tiene la sesion iniciada
        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Perfil"); //La base de datos perfil


        mDatabaseRef.child(mUser.getUid()).child("conectado").setValue("Conectado");

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
                } else if (id == R.id.navigation_fotos) {
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

    @Override
    public void onStop() {
        super.onStop();
        mDatabaseRef.child(mUser.getUid()).child("conectado").setValue("No está conectado");

    }
*/
}
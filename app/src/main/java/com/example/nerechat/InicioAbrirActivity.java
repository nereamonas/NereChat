package com.example.nerechat;

import androidx.annotation.NonNull;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.example.nerechat.base.BaseActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class InicioAbrirActivity extends BaseActivity {

    //Simplemente es como una actividad de transicion. cuando se abra la app se mostrara unos segundos esta ventana.
    //Aqui se verá si el usuario ya cuenta con una sesion iniciada o por el contrario no tiene ninguna iniciada.
    //En el caso de ya tener iniciada, se reenvia directamente al mainActivity para que no tenga que volver a pasar por el inicio de sesion
    //Cada vez q cierre y abra la app. Por el contrario, si la sesion está cerrada se le enviará a la ventana de inicio de sesion

    //Cargamos la informacion de firebase. que necesitamos para ssaber si esta abierta la sesion
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRef;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio_abrir);

        //Inicializamos variables
        mAuth=FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser(); //El usuario actual que tiene iniciada la sesion
        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Perfil"); //para comprobar el perfil del usuario correspondiente

        comprobarSiYaHaySesion(); //Comprobar

    }




    public void comprobarSiYaHaySesion(){ //Con firebase, podemos ver si tenemos algun usuario con la sesion iniciada y asi enviarle directamente a la pantalla principal sin que tenga que iniciar sesion

        mUser=mAuth.getCurrentUser(); //El usuario actual que tiene iniciada la sesion
        if (mUser!=null){  //Si existe un mUser significa que ya tenemos una sesion iniciada
            Log.d("Logs", "mUser: "+mUser.getUid());
            //Pero podemos haber abandonado la app sin llegar a crear el perfil del todo.
            //Comprobamos si tenemos algun perfil creado en la base de datos, de ser asi vamos a la pantalla principal, y en el caso de no tener perfil, llevamos a la pantalla de crear un perfil
            mDatabaseRef.child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.exists()){//Comporbamos que ese mUser, tenga creado un perfil. si ya tiene el perfil creado vamos directamente a mainActivicity
                        Log.d("Logs", "ya tiene perfil asique vamos al main");
                        abrirPrincipal();
                    }else{//Si no tiene el usuario creado, es porque ha abandonado en la pagina NuevoUsuarioActivity, asique le llevamos a esa pag para que termine de crear todo bien
                        Log.d("Logs", "no tiene perfil tiene q crearse uno");
                        abrirCrearPerfil();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });
        }else{
            abrirIniciarSesion();
        }

    }

    public void abrirPrincipal(){ //Abrimos la pantalla principal porque ya se ha autenticado correctamente
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    public void abrirIniciarSesion(){ //Abrimos la pantalla principal porque ya se ha autenticado correctamente
        Intent i = new Intent(this, IniciarSesionActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }


    public void abrirCrearPerfil(){ //Abrimos la ventana de que falta crear el perfil de usuario
        Intent i = new Intent(this, CrearPerfilActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

}
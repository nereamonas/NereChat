package com.example.nerechat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.nerechat.base.BaseViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class PerfilOtroUsActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRef;

    String pId;

    CircleImageView circleImageView;
    TextView nombreUsu,estado,fechaUnion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_otro_us);

        circleImageView=findViewById(R.id.circleImageViewPerfilOtroUsE);
        nombreUsu=findViewById(R.id.textPerfilOtroUs_NombreUs);

        fechaUnion=findViewById(R.id.textPerfilOtroUs_fechaunion);
        estado=findViewById(R.id.textPerfilOtroUs_Estado);

        mAuth= FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser(); //El usuario actual que tiene la sesion iniciada
        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Perfil"); //La base de datos perfil

        pId= getIntent().getExtras().getString("usuario");

        Log.d("Logs","pId recibido: "+pId);
        cargarInformacion();



    }


    public void cargarInformacion(){

        //Tenemos que coger de la base de datos la informacion del otro usuario. como tenemos su UID es sencillo
        mDatabaseRef.child(pId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    //Si existe el usuario
                    Log.d("Logs","Snapchot "+snapshot.toString());
                    nombreUsu.setText(snapshot.child("nombreUsuario").getValue().toString()); //Cogemos su nombre de usuario
                    estado.setText(snapshot.child("estado").getValue().toString()); //Cogemos su foto de perfil
                    fechaUnion.setText(snapshot.child("fechaCreacion").getValue().toString()); //Cogemos su foto de perfil
                    Picasso.get().load(snapshot.child("fotoPerfil").getValue().toString()).into(circleImageView); //Mostramos la foto de perfil en pantalla
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}
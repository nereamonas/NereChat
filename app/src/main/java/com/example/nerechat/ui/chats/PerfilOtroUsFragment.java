package com.example.nerechat.ui.chats;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.nerechat.R;
import com.example.nerechat.base.BaseViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class PerfilOtroUsFragment extends Fragment {

    //Mostraremos la informacion del perfil del otro usuario

    private BaseViewModel perfilOtroUsViewModel;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRef;

    String pId;

    CircleImageView imageView;
    TextView nombreUsu,estado,fechaUnion;

    String fotoPerfilurl="";
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        perfilOtroUsViewModel =
                new ViewModelProvider(this).get(BaseViewModel.class);
        View root=inflater.inflate(R.layout.fragment_perfil_otro_us, container, false);

        //hasieratuamos los elementos
        imageView=root.findViewById(R.id.circleImageViewPerfilOtroUsE);
        nombreUsu=root.findViewById(R.id.textPerfilOtroUs_NombreUs);

        fechaUnion=root.findViewById(R.id.textPerfilOtroUs_fechaunion);
        estado=root.findViewById(R.id.textPerfilOtroUs_Estado);

        mAuth= FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser(); //El usuario actual que tiene la sesion iniciada
        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Perfil"); //La base de datos perfil

        pId = getArguments().getString("usuario"); //Cogemos el pid del usuario del que tenemos que mostrar la informacion, que nos viene como argumento del fragment anterior

        FirebaseMessaging.getInstance().subscribeToTopic(mUser.getUid()); //Para recibir las notif d ese id

        cargarInformacion(); //Cargamos la info del usuario

        imageView.setOnClickListener(new View.OnClickListener() { //Al hacer click en la foto cargaremos un nuevo fragment que simplemente muestra la imagen ampliada y nos permitirá hacer zoom sobre ella
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle(); //Con el bundle podemos pasar datos
                bundle.putString("imagen", fotoPerfilurl); //Le tenemos q pasar la url de la imagen
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_perfilOtroUsFragment_to_verImagenFragment, bundle,options);

            }
        });

        return root;
    }


    public void cargarInformacion(){

        //Tenemos que coger de la base de datos la informacion del otro usuario. como tenemos su UID es sencillo
        mDatabaseRef.child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    //Si existe el usuario cogemos sus datos y los insertamos en los textview/imaegview
                    Log.d("Logs","Snapchot "+snapshot.toString());
                    nombreUsu.setText(snapshot.child("nombreUsuario").getValue().toString()); //Cogemos su nombre de usuario
                    estado.setText(snapshot.child("estado").getValue().toString()); //Cogemos su foto de perfil
                    fechaUnion.setText(snapshot.child("fechaCreacion").getValue().toString()); //Cogemos su foto de perfil
                    fotoPerfilurl=snapshot.child("fotoPerfil").getValue().toString();
                    Picasso.get().load(snapshot.child("fotoPerfil").getValue().toString()).into(imageView); //Mostramos la foto de perfil en pantalla
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


}
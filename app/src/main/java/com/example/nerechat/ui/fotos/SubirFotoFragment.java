package com.example.nerechat.ui.fotos;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nerechat.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class SubirFotoFragment extends Fragment {

    int CODIGO_GALERIA = 1;
    ImageView fotoImageView;
    Uri uriImg;
    EditText editTextTextMultiLine;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRef;
    StorageReference mStorageRef;

    ProgressDialog progressDialog;

    Toolbar toolbar;
    ConstraintLayout constraintLayout;

    ImageView toolbarImagenAjustes;
    TextView toolbarTitulo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root=inflater.inflate(R.layout.fragment_subir_foto, container, false);


        fotoImageView = root.findViewById(R.id.fotoImageView);
        editTextTextMultiLine = root.findViewById(R.id.editTextTextMultiLine);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser(); //El usuario actual que tiene la sesion iniciada
        mDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Imagen"); //La base de datos perfil
        mStorageRef = FirebaseStorage.getInstance().getReference().child("Fotos"); //En Storage almacenaremos todas las imagenes de perfil que suban los usuarios, y en la base de datos guardamos la uri que hace referencia a la foto en storage

        progressDialog=new ProgressDialog(getContext());


        //Toolbar
        toolbar=root.findViewById(R.id.chat_toolbarsubirfoto);
        constraintLayout=root.findViewById(R.id.toolbatPrincipalLayout);
        comprobarColores(); //El toolbar tiene un problema para sincronizarse con el tema, asique lo he hecho en este metodo aparte
        toolbarImagenAjustes=root.findViewById(R.id.imageViewToolbarPrincipalAjustes);
        toolbarTitulo=root.findViewById(R.id.toolbarPrincipalTitulo);
        toolbarTitulo.setText(getString(R.string.nav_subirfoto));
        toolbarImagenAjustes.setOnClickListener(new View.OnClickListener() { //Cuando se clique en el boton  de ajustes del toolvar, navegaremos al fragment de ajustes
            @Override
            public void onClick(View v) {
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_subirFotoFragment_to_ajustesFragment,null,options);

            }
        });

        fotoImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Logs", "Abrimos galeria para seleccionar una foto");
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);  //Creamos un intent de la galeria. para abrir la galeria
                startActivityForResult(gallery, CODIGO_GALERIA); //Abrimos la actividad y pasamos el cogido de resultado para recibir cuando se finalice el intent

            }
        });

        Button subirfoto=root.findViewById(R.id.buttonSubirFoto);
        subirfoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                subirFotoFirebase(v);
            }
        });

        ImageView cancel=root.findViewById(R.id.imageButton);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelar(v);
            }
        });
        return root;
    }


    public void cancelar(View view){ //Abrimos la pantalla principal

        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build();
        Navigation.findNavController(view).navigate(R.id.action_subirFotoFragment_to_navigation_fotos, null, options);
    }


    public void subirFotoFirebase(View view){
        //mStorageRef.child(mUser.getUid()).delete();

        Date date = new Date(); //Cargamos la hora exacta en la que estamos para enviar el mensaje
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

        String titulo=mUser.getUid()+"_"+formatter.format(date);


        if (uriImg!=null) {

            progressDialog.setTitle(getString(R.string.progressDialog_SubiendoImagen));
            progressDialog.setMessage(getString(R.string.progressDialog_porfavorespere));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();


            // Una vez borrado subimos la foto nueva
            mStorageRef.child(titulo).putFile(uriImg).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) { //Si se ha subido correctamente
                    //Cogemos la url

                    mStorageRef.child(titulo).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {

                            HashMap<String,Object> hashMap = new HashMap<String,Object>();
                            hashMap.put("descripcion", editTextTextMultiLine.getText().toString());
                            hashMap.put("uri", uri.toString());
                            hashMap.put("id", titulo);
                            hashMap.put("uid", mUser.getUid());
                            hashMap.put("likes", ""+0);

                            mDatabaseRef.child(titulo).updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener() {
                                @Override
                                public void onSuccess(Object o) {
                                    //Si to do ha ido bien, se ha añadido correctamente a la base de datos por lo que se ha creado el perfil
                                    Toast.makeText(getContext(), "Imagen subida", Toast.LENGTH_SHORT).show();
                                    progressDialog.dismiss();
                                    NavOptions options = new NavOptions.Builder()
                                            .setLaunchSingleTop(true)
                                            .build();
                                    Navigation.findNavController(view).navigate(R.id.action_subirFotoFragment_to_navigation_fotos, null, options);

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    //No se ha podido crear el usuario
                                    progressDialog.dismiss();
                                    Toast.makeText(getContext(), "Ha ocurrido algun error al subir la imagen", Toast.LENGTH_SHORT).show();
                                }
                            });

                        }
                    });
                }
            });
        }else{
            Toast.makeText(getContext(), "Selecciona una imagen", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //Recogeremos la informacion de fin de actividad de elegir la foto de la galeria
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("Logs","onActivityResult");
        if(requestCode == CODIGO_GALERIA){ //Si la respuesta viene de la galeria y es correcta.
            if(resultCode == Activity.RESULT_OK){ //Si to do ha ido bien
                //Cargamos la imagen
                this.uriImg=data.getData(); //Cogemos la uri de la imagen cargada y la guardamos en un aldagai
                Log.d("Logs","URL de la imagen de la galeria: "+uriImg);
                fotoImageView.setImageURI(uriImg);  //mostramos en pantalla la imagen subida
                //subirFotoFirebase();
            }
        }

    }

    public void comprobarColores(){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String tema = "";
        if (prefs.contains("tema")) {
            tema = prefs.getString("tema", null);
        }
        switch (tema) {
            case "morado":
                toolbar.setBackgroundColor(getResources().getColor(R.color.toolbar_morado,getContext().getTheme()));
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.toolbar_morado,getContext().getTheme()));
                break;
            case "naranja":
                toolbar.setBackgroundColor(getResources().getColor(R.color.toolbar_naranja,getContext().getTheme()));
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.toolbar_naranja,getContext().getTheme()));
                break;
            case "verde":
                toolbar.setBackgroundColor(getResources().getColor(R.color.toolbar_verde,getContext().getTheme()));
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.toolbar_verde,getContext().getTheme()));
                break;
            case "azul":
                toolbar.setBackgroundColor(getResources().getColor(R.color.azul_medioOscuro,getContext().getTheme()));
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.azul_medioOscuro,getContext().getTheme()));
                break;
            case "verdeazul":
                toolbar.setBackgroundColor(getResources().getColor(R.color.toolbar_verdeazul,getContext().getTheme()));
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.toolbar_verdeazul,getContext().getTheme()));
                break;
            default:
                toolbar.setBackgroundColor(getResources().getColor(R.color.azul_medioOscuro,getContext().getTheme()));
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.azul_medioOscuro,getContext().getTheme()));
                break;
        }
    }
}
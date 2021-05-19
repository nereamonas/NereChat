package com.example.nerechat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.nerechat.base.BaseActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;

public class SubirFotoActivity extends BaseActivity {

    int CODIGO_GALERIA = 1;
    ImageView fotoImageView;
    Uri uriImg;
    EditText editTextTextMultiLine;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRef;
    StorageReference mStorageRef;

    ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subir_foto);

        fotoImageView = findViewById(R.id.fotoImageView);
        editTextTextMultiLine = findViewById(R.id.editTextTextMultiLine);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser(); //El usuario actual que tiene la sesion iniciada
        mDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Imagen"); //La base de datos perfil
        mStorageRef = FirebaseStorage.getInstance().getReference().child("Fotos"); //En Storage almacenaremos todas las imagenes de perfil que suban los usuarios, y en la base de datos guardamos la uri que hace referencia a la foto en storage

        progressDialog=new ProgressDialog(this);

        fotoImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Logs", "Abrimos galeria para seleccionar una foto");
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);  //Creamos un intent de la galeria. para abrir la galeria
                startActivityForResult(gallery, CODIGO_GALERIA); //Abrimos la actividad y pasamos el cogido de resultado para recibir cuando se finalice el intent

            }
        });
    }

    public void cancelar(View view){ //Abrimos la pantalla principal
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }


    public void abrirPrincipal(){ //Abrimos la pantalla principal
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        i.putExtra("abrir", "fotos");
        startActivity(i);
        finish();
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
                                    Toast.makeText(SubirFotoActivity.this, "Imagen subida", Toast.LENGTH_SHORT).show();
                                    progressDialog.dismiss();
                                    abrirPrincipal(); //Abrimos la pantalla principal

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    //No se ha podido crear el usuario
                                    progressDialog.dismiss();
                                    Toast.makeText(SubirFotoActivity.this, "Ha ocurrido algun error al subir la imagen", Toast.LENGTH_SHORT).show();
                                }
                            });

                        }
                    });
                }
            });
        }else{
            Toast.makeText(SubirFotoActivity.this, "Selecciona una imagen", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //Recogeremos la informacion de fin de actividad de elegir la foto de la galeria
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("Logs","onActivityResult");
        if(requestCode == CODIGO_GALERIA){ //Si la respuesta viene de la galeria y es correcta.
            if(resultCode == RESULT_OK){ //Si to do ha ido bien
                //Cargamos la imagen
                this.uriImg=data.getData(); //Cogemos la uri de la imagen cargada y la guardamos en un aldagai
                Log.d("Logs","URL de la imagen de la galeria: "+uriImg);
                fotoImageView.setImageURI(uriImg);  //mostramos en pantalla la imagen subida
                //subirFotoFirebase();
            }
        }

    }
}

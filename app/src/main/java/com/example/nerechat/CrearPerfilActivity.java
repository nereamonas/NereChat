package com.example.nerechat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.nerechat.base.BaseActivity;
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

import de.hdodenhof.circleimageview.CircleImageView;

public class CrearPerfilActivity extends BaseActivity {
    //Aparte de registrarnos con firebase auth, tenemos que crear un perfil, para poder añadir nuestra informacion, nombre de usuario, foto de perfil.
    //Para unir la authenticacion con el perfil, crearemos en la base de datos una nueva tabla llamada perfil, donde se guardarán todos los perfiles. de titulo tendra el user.uid y asi podemos unir el auth con el perfil

     CircleImageView imagenPerfil;
     EditText nombreUsuario;

     Uri uriImg;
     int CODIGO_GALERIA=1;

     FirebaseAuth mAuth;
     FirebaseUser mUser;
     DatabaseReference mDatabaseRef;
     StorageReference mStorageRef;
     ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_perfil);

        //Inicializamos las variables
        imagenPerfil=findViewById(R.id.profile_image);
        nombreUsuario=findViewById(R.id.editTextNombreUsuario);

        progressDialog= new ProgressDialog(this);
        mAuth=FirebaseAuth.getInstance(); //Authenticacion de firebase
        mUser=mAuth.getCurrentUser(); //El usuario actual que tiene la sesion iniciada
        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Perfil"); //Referencia a la base de datos a la tabla Perfil. donde guardaremos el perfil creado
        mStorageRef= FirebaseStorage.getInstance().getReference().child("ImagenesPerfil"); //En Storage almacenaremos todas las imagenes de perfil que suban los usuarios, y en la base de datos guardamos la uri que hace referencia a la foto en storage


        imagenPerfil.setOnClickListener(new View.OnClickListener() { //Cuando clicamos en la foto de perfil, abriremos la galeria, y daremos opcion a seleccionar una foto de la galeria
            @Override
            public void onClick(View v) {
                Log.d("Logs","Abrimos galeria para seleccionar una foto");
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);  //Creamos un intent de la galeria. para abrir la galeria
                startActivityForResult(gallery,CODIGO_GALERIA); //Abrimos la actividad y pasamos el cogido de resultado para recibir cuando se finalice el intent
            }
        });


    }

    public void guardarDatos(View v){ //Pulsamos en guardar datos.
        //Primero debemos comprobar que los datos introducidos, el nombre de usuario y la foto de perfil son validos
        String name=nombreUsuario.getText().toString();

        if (!name.equals("")){ //El nombre debe ser distinto de null
            if (uriImg!=null) { //Y se debe haber seleccionado una foto de la galeria
                //Mostramos un progress dialog para informar de que se esta creando el perfil
                progressDialog.setTitle("Creando perfil");
                progressDialog.setMessage("Por favor espere hasta que se acabe de crear el perfil");
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();

                //Primero, subiremos a firebase Storage la foto de perfil del usuario y recogeremos la url donde se ha subido. Subiremos la imagen con el nombre mUser.getUid para tener una referencia a ella
                mStorageRef.child(mUser.getUid()).putFile(uriImg).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) { //Si se ha subido correctamente
                        //Cogemos la url
                            mStorageRef.child(mUser.getUid()).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    //Cogemos la fecha de hoy
                                    Date date = new Date();
                                    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                                    SimpleDateFormat hora = new SimpleDateFormat("HH:mm");

                                    //Ya tenemos la url de la foto de perfil de firebase storage. Ahora creamos un hashmap, donde guardamos el nombredeusuario, la foto de perfil y el estado
                                    HashMap hashMap = new HashMap<>();
                                    hashMap.put("nombreUsuario", name);
                                    hashMap.put("fotoPerfil", uri.toString());
                                    hashMap.put("conectado", "Última vez a las "+hora.format(date));
                                    hashMap.put("estado", "Heyy, ¡Estoy usando NereChat!");
                                    hashMap.put("fechaCreacion",formatter.format(date));
                                    hashMap.put("uid", mUser.getUid());
                                    //Una vez tenemos el hashmap lo subimos a la base de datos, dentro de la tabla perfil, y el mUser.getUid como titulo.
                                    mDatabaseRef.child(mUser.getUid()).updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener() {
                                        @Override
                                        public void onSuccess(Object o) {
                                            //Si to do ha ido bien, se ha añadido correctamente a la base de datos por lo que se ha creado el perfil
                                            progressDialog.dismiss(); //Cancelamos la barra de progreso
                                            abrirPrincipal(); //Abrimos la pantalla principal
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            //No se ha podido crear el usuario
                                            progressDialog.dismiss();  //Cancelamos la barra de progreso

                                            Toast.makeText(CrearPerfilActivity.this, getString(R.string.toast_nosehapodidocrearelperfil), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            });
                    }
                });
            }else{
                Toast.makeText(this,getString(R.string.toast_Tienesqueseleccionarunafotodeperfil),Toast.LENGTH_SHORT).show();;
            }
        }else{
            Toast.makeText(this,getString(R.string.toast_Elnombredeusuarionoesvalido),Toast.LENGTH_SHORT).show();;
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //Recogeremos la informacion de fin de actividad de elegir la foto de la galeria
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("Logs","onActivityResult");
        if(requestCode == CODIGO_GALERIA){ //Si la respuesta viene de la galeria y es correcta.
            if(resultCode == RESULT_OK){ //Si to do ha ido bien
                //Cargamos la imagen
                this.uriImg=data.getData(); //Cogemos la uri de la imagen cargada y la guardamos en un aldagai
                Log.d("Logs","URL de la imagen de la galeria: "+uriImg);
                imagenPerfil.setImageURI(uriImg);  //mostramos en pantalla la imagen subida
            }
        }

    }


    public void abrirPrincipal(){ //Abrimos la pantalla principal
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }
}
package com.example.nerechat.ui.perfil;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.nerechat.CrearPerfilActivity;
import com.example.nerechat.IniciarSesionActivity;
import com.example.nerechat.R;
import com.example.nerechat.base.BaseViewModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

public class PerfilFragment extends Fragment {

    int CODIGO_GALERIA=1;
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRef;
    StorageReference mStorageRef;

    private BaseViewModel perfilViewModel;

    CircleImageView circleImageView;
    ImageView editUsername,editEstado;
    FloatingActionButton floatingButtonCambiarFoto;
    TextView nombreUsu,estado;
    Uri uriImg;


    ImageView toolbarImagenAjustes;
    TextView toolbarTitulo;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        perfilViewModel =
                new ViewModelProvider(this).get(BaseViewModel.class);
        View root = inflater.inflate(R.layout.fragment_perfil, container, false);

        circleImageView=root.findViewById(R.id.circleImageViewPerfilE);
        editUsername=root.findViewById(R.id.imageViewUsuarioEdit);
        editEstado=root.findViewById(R.id.imageViewEstadoEdit);
        floatingButtonCambiarFoto=root.findViewById(R.id.floatingActionButtonEditFoto);
        nombreUsu=root.findViewById(R.id.textPerfil_NombreUs);
        estado=root.findViewById(R.id.textPerfil_Estado);
        Button b=root.findViewById(R.id.buttonLogOut);

        mAuth= FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser(); //El usuario actual que tiene la sesion iniciada
        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Perfil"); //La base de datos perfil
        mStorageRef= FirebaseStorage.getInstance().getReference().child("ImagenesPerfil"); //En Storage almacenaremos todas las imagenes de perfil que suban los usuarios, y en la base de datos guardamos la uri que hace referencia a la foto en storage


        //Toolbar
        toolbarImagenAjustes=root.findViewById(R.id.imageViewToolbarPrincipalAjustes);
        toolbarTitulo=root.findViewById(R.id.toolbarPrincipalTitulo);
        toolbarTitulo.setText(getString(R.string.nav_perfil));
        toolbarImagenAjustes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_navigation_perfil_to_ajustesFragment,null,options);

            }
        });

        cargarInformacion();

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent i = new Intent(getContext(), IniciarSesionActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                ((AppCompatActivity)getActivity()).finish();
            }
        });


        editEstado.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Creamos una alerta
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Inserta el nuevo estado");
                //Añadimos en la alerta un edit text
                final EditText input = new EditText(getContext());  //Creamos un edit text. para q el usuairo pueda insertar el titulo
                builder.setView(input);
                //Si el usuario da al ok
                builder.setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() {  //Si el usuario acepta, mostramos otra alerta con los ejercicios que puede agregar
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String nuevoEstado = input.getText().toString();

                        // actualizar
                        if (nuevoEstado != "") {
                            mDatabaseRef.child(mUser.getUid()).child("estado").setValue(nuevoEstado);
                            Toast.makeText(getContext(), "Estado actualizado", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "El estado no see ha podido actualizar", Toast.LENGTH_SHORT).show();
                        }

                    }
                });

                //Si se cancela, no se creará la rutina y se cancelará el dialogo
                builder.setNegativeButton(getString(R.string.Cancelar), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        editUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Creamos una alerta
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Inserta el nuevo nombre de usuario");
                //Añadimos en la alerta un edit text
                final EditText input = new EditText(getContext());  //Creamos un edit text. para q el usuairo pueda insertar el titulo
                builder.setView(input);
                //Si el usuario da al ok
                builder.setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() {  //Si el usuario acepta, mostramos otra alerta con los ejercicios que puede agregar
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String nuevoNombreDeUsuario = input.getText().toString();

                        // actualizar
                        if (nuevoNombreDeUsuario != "") {
                            mDatabaseRef.child(mUser.getUid()).child("nombreUsuario").setValue(nuevoNombreDeUsuario);
                            Toast.makeText(getContext(), "Nombre de uusario actualizado", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "El nombre de usuario no see ha podido actualizar", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                //Si se cancela, no se creará la rutina y se cancelará el dialogo
                builder.setNegativeButton(getString(R.string.Cancelar), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        floatingButtonCambiarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Logs","Abrimos galeria para seleccionar una foto");
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);  //Creamos un intent de la galeria. para abrir la galeria
                startActivityForResult(gallery,CODIGO_GALERIA); //Abrimos la actividad y pasamos el cogido de resultado para recibir cuando se finalice el intent

            }
        });



        return root;
    }


    public void cargarInformacion(){

        //Tenemos que coger de la base de datos la informacion del otro usuario. como tenemos su UID es sencillo
        mDatabaseRef.child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    //Si existe el usuario
                    nombreUsu.setText(snapshot.child("nombreUsuario").getValue().toString()); //Cogemos su nombre de usuario
                    estado.setText(snapshot.child("estado").getValue().toString()); //Cogemos su foto de perfil
                    Picasso.get().load(snapshot.child("fotoPerfil").getValue().toString()).into(circleImageView); //Mostramos la foto de perfil en pantalla
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    public void actualizarFirebaseImage(){
        mStorageRef.child(mUser.getUid()).delete();

        // Una vez borrado subimos la foto nueva
        mStorageRef.child(mUser.getUid()).putFile(uriImg).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) { //Si se ha subido correctamente
                //Cogemos la url
                mStorageRef.child(mUser.getUid()).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        mDatabaseRef.child(mUser.getUid()).child("fotoPerfil").setValue(uri);
                        Toast.makeText(getContext(), "Imagen actualizada", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
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
                circleImageView.setImageURI(uriImg);  //mostramos en pantalla la imagen subida
                actualizarFirebaseImage();
            }
        }

    }

}
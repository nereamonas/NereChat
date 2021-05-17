package com.example.nerechat.ui.chats;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.example.nerechat.R;
import com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios.ViewHolderMensaje;
import com.example.nerechat.base.BaseViewModel;
import com.example.nerechat.helpClass.Mensaje;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

public class ChatFragment extends Fragment {

    private BaseViewModel chatViewModel;

    //Se muestra el chat con una persona

    Toolbar toolbar;
    RecyclerView recyclerView;
    EditText mensaje;
    ImageView send, seleccionarImagen;
    CircleImageView barraPerfilImg;
    TextView barraUsername, barraStado;

    //Serán los datos del usuario con el que estamos hablando
    String pId, pNombreUsu, pFotoPerfil, pEstado;
    String miFotoPerfil, miNombreUsuario;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRef, mDatabaseRefMensajes;
    StorageReference mStorageRef;
    FirebaseRecyclerOptions<Mensaje> options;
    FirebaseRecyclerAdapter<Mensaje, ViewHolderMensaje> adapter;

    String UrlNotif = "https://fcm.googleapis.com/fcm/send";
    RequestQueue requestQueue;

    int codigoRequestGaleria = 4;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        chatViewModel =
                new ViewModelProvider(this).get(BaseViewModel.class);
        View root = inflater.inflate(R.layout.fragment_chat, container, false);


        //Cogemos el extra que le hemos pasado. Hace referencia al idDelOtroUsuario

        pId = getArguments().getString("usuario");

        recyclerView = root.findViewById(R.id.chat_recyclerView);
        mensaje = root.findViewById(R.id.chat_mensaje);
        send = root.findViewById(R.id.chat_send);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        barraPerfilImg = root.findViewById(R.id.barraImagenPErfil);
        barraUsername = root.findViewById(R.id.barraNombreUsu);
        barraStado = root.findViewById(R.id.barraEstado);
        seleccionarImagen = root.findViewById(R.id.chat_seleccionarImagen);

        //Cargamos los datos de firebase que necesitaremos
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Perfil"); //La base de datos perfil
        mDatabaseRefMensajes = FirebaseDatabase.getInstance().getReference().child("MensajesChat"); //Y la base de datos mensajeChat donde se almacenarán todos los mensajes
        mStorageRef = FirebaseStorage.getInstance().getReference().child("FotosMensajes"); //En Storage almacenaremos todas las imagenes de perfil que suban los usuarios, y en la base de datos guardamos la uri que hace referencia a la foto en storage

        cambiarEstado("Conectado");

        requestQueue = Volley.newRequestQueue(getContext());
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
        //Cargamos el toolbar y cargamos la informacion del otro usaurio en el toolbar
        toolbar = root.findViewById(R.id.chat_toolbar);
        //setSupportActionBar(findViewById(R.id.chat_toolbar));

        BottomNavigationView nv = ((AppCompatActivity) getActivity()).findViewById(R.id.nav_view);
        nv.setVisibility(View.GONE);

        cargarInfoBarra();
        cargarMiFotoPerfil(); //Buscamos mi foto de perfil porque se usara en los mensajes;
        cambiarMensajesALeido();
        //Cargamos todos los mensajes que se han mandado con ese usuario
        cargarMensajes();

        send.setOnClickListener(new View.OnClickListener() { //Cuando clickemos en el boton send será que queremos mandar un mensaje. Asique mandamos el mensaje
            @Override
            public void onClick(View v) {
                mandarMensaje(null);
            }
        });

        seleccionarImagen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);  //Creamos un intent de la galeria. para abrir la galeria
                startActivityForResult(gallery, codigoRequestGaleria); //Abrimos la actividad y pasamos el cogido de resultado para recibir cuando se finalice el intent

            }
        });

        barraPerfilImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle(); //Con el bundle podemos pasar datos
                bundle.putString("usuario", pId);
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_chatFragment_to_perfilOtroUsFragment, bundle, options);
            }
        });
        barraUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle(); //Con el bundle podemos pasar datos
                bundle.putString("usuario", pId);
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_chatFragment_to_perfilOtroUsFragment, bundle, options);
            }
        });
        return root;

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //Recogeremos la informacion de fin de actividad de elegir la foto de la galeria
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("Logs", "onActivityResult");
        if (requestCode == codigoRequestGaleria) { //Si la respuesta viene de la galeria y es correcta.
            if (resultCode == RESULT_OK) { //Si to do ha ido bien
                //Cargamos la imagen
                Uri uri = data.getData(); //Cogemos la uri de la imagen cargada y la guardamos en un aldagai
                Log.d("Logs", "URL de la imagen de la galeria: " + uri);

                if (uri != null) { //Y se debe haber seleccionado una foto de la galeria
                    String uuid = UUID.randomUUID().toString();
                    mStorageRef.child(uuid).putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) { //Si se ha subido correctamente
                            //Cogemos la url
                            mStorageRef.child(uuid).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    //Cogemos la fecha de hoy
                                    mandarMensaje(uri);

                                }
                            });
                        }
                    });

                }
            }
        }

    }

    public void cargarInfoBarra() {
        //Tenemos que coger de la base de datos la informacion del otro usuario. como tenemos su UID es sencillo
        mDatabaseRef.child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    //Si existe el usuario
                    pNombreUsu = snapshot.child("nombreUsuario").getValue().toString(); //Cogemos su nombre de usuario
                    pFotoPerfil = snapshot.child("fotoPerfil").getValue().toString(); //Cogemos su foto de perfil
                    pEstado = snapshot.child("conectado").getValue().toString(); //Cogemos su estado
                    Picasso.get().load(pFotoPerfil).into(barraPerfilImg); //Mostramos la foto de perfil en pantalla
                    barraUsername.setText(pNombreUsu);//Mostramos el nombre de usuario en pantalla

                    mantenerEstadoActualizado();
                    //Mirar si tiene la ultima hora para mostrarla
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext()); //Cogemos las preferencias
                    if (prefs.contains("ultimaHora")) { //Comprobamos si existe notif
                        Boolean bloqueado = prefs.getBoolean("ultimaHora", true);  //Comprobamos si las notificaciones estan activadas
                        Log.d("Logs", "estado visulizar ultimaHora: " + bloqueado);
                        if (bloqueado) { //Si esta bloqueado no mostramos nada
                            barraStado.setText("");
                        } else {
                            barraStado.setText(pEstado); //no estan bloqueados asiq mostramos
                        }
                    } else {
                        barraStado.setText(pEstado); //si no se ha establecido nada en ajustes mostramos
                    }
                }
                mDatabaseRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    public void cargarMiFotoPerfil() {
        //Tenemos que coger de la base de datos la informacion del otro usuario. como tenemos su UID es sencillo
        mDatabaseRef.child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    //Si existe el usuario
                    miFotoPerfil = snapshot.child("fotoPerfil").getValue().toString(); //Cogemos mi foto de perfil
                    miNombreUsuario = snapshot.child("nombreUsuario").getValue().toString();
                }
                mDatabaseRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void cargarMensajes() {
        //Se ha creado con un recycler view + card view como la lista de usuarios. que firebase facilita el trabajo.
        //Tenemos que crear una clase Chat para guardar los valores de la bbdd
        options = new FirebaseRecyclerOptions.Builder<Mensaje>().setQuery(mDatabaseRefMensajes.child(mUser.getUid()).child(pId), Mensaje.class).build();
        adapter = new FirebaseRecyclerAdapter<Mensaje, ViewHolderMensaje>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ViewHolderMensaje holder, int position, @NonNull Mensaje model) {

                //Tenemos dos opciones. Que el mensaje lo hayamos mandado nosotros, o que nosotros seamos los receptores del mensaje
                if (model.getUsuario().equals(mUser.getUid())) { //En el caso de que el mensaje lo haya mandado yo
                    holder.mensajeTextoUno.setVisibility(View.GONE); //Oculto la info del otro
                    holder.mensajeFotoPerfilUno.setVisibility(View.GONE);
                    holder.mensajeHoraUno.setVisibility(View.GONE);
                    holder.imageLikeUno.setVisibility(View.GONE);
                    holder.mensajeTextoDos.setVisibility(View.VISIBLE); //Pongo visible la info del mio
                    holder.mensajeFotoPerfilDos.setVisibility(View.VISIBLE);
                    holder.mensajeHoraDos.setVisibility(View.VISIBLE);
                    holder.imageDobleCheckDos.setVisibility(View.VISIBLE);

                    if (!model.getReaccion().equals("")) {
                        holder.imageLikeDos.setVisibility(View.VISIBLE);
                        if (model.getReaccion().equals("like")) {
                            holder.imageLikeDos.setImageDrawable(getContext().getResources().getDrawable(R.mipmap.ic_like));
                        } else if (model.getReaccion().equals("feliz")) {
                            holder.imageLikeDos.setImageDrawable(getContext().getResources().getDrawable(R.mipmap.ic_feliz));
                        } else if (model.getReaccion().equals("enfadado")) {
                            holder.imageLikeDos.setImageDrawable(getContext().getResources().getDrawable(R.mipmap.ic_enfadado));
                        }
                    } else {
                        holder.imageLikeDos.setVisibility(View.GONE);
                    }
                    if (model.getLeido().equals("si")) {
                        holder.imageDobleCheckDos.setImageDrawable(getContext().getResources().getDrawable(R.mipmap.ic_doblecheck_azul));
                    }

                    if (model.getMensaje().contains("https://firebasestorage.googleapis.com/")) {

                        Glide.with(getContext())
                                .load(model.getMensaje())
                                .asBitmap()
                                .placeholder(R.drawable.place_holder_image)
                                .error(R.drawable.place_holder_image)
                                .into(new SimpleTarget<Bitmap>() {
                                    @Override
                                    public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                        Bitmap bitmap=resource;
                                        Bitmap myBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth()/4, bitmap.getHeight()/4, false);
                                        Spannable span = new SpannableString("I");
                                        ImageSpan image = new ImageSpan(getContext(), myBitmap);
                                        span.setSpan(image, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

                                        holder.mensajeTextoDos.setText(span, TextView.BufferType.SPANNABLE);

                                    }
                                    @Override
                                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                        super.onLoadFailed(e, errorDrawable);
                                    }
                                });

                    } else {
                        holder.mensajeTextoDos.setText(model.getMensaje()); //Pongo mi mezu
                    }
                    holder.mensajeHoraDos.setText(model.getHora());
                    Picasso.get().load(miFotoPerfil).into(holder.mensajeFotoPerfilDos);
                    holder.mensajeTextoDos.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            /**/

                            //POPUP MENU
                            //Creating the instance of PopupMenu
                            PopupMenu popup = new PopupMenu(getContext(), holder.mensajeTextoDos);
                            //Inflating the Popup using xml file
                            popup.getMenuInflater().inflate(R.menu.popup_menu_mensaje, popup.getMenu());


                            //registering popup with OnMenuItemClickListener
                            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                public boolean onMenuItemClick(MenuItem item) {
                                    int id = item.getItemId();
                                    if (id == R.id.popupMenu_EliminarMensaje) {  //Si clicamos en rutinas, abriremos con navigation, la ventana donde se muestran las rutinas. he igual con todos
                                        Log.d("Logs", "popupMenu_EliminarMensaje");
                                        //abrir_chatAmigos();
                                        //Preguntamos si quiere eliminar el mensaje

                                        AlertDialog.Builder dialogo = new AlertDialog.Builder(v.getContext());
                                        dialogo.setTitle("Eliminar mensaje");
                                        dialogo.setMessage("Quieres eliminar el mensaje " + model.getMensaje() + "?");

                                        dialogo.setPositiveButton("Solo para mi", new DialogInterface.OnClickListener() {  //Botón si. es decir, queremos eliminar la rutina
                                            public void onClick(DialogInterface dialogo1, int id) {
                                                //Si dice que si quiere eliminar. Actualizamos la lista y lo borramos de la base de datos
                                                //Cogemos el id del elemento seleccionado por el usuario
                                                Toast.makeText(getContext(), "Has eliminado el mensaje " + model.getMensaje() + "solo para ti", Toast.LENGTH_SHORT).show();

                                                mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                            Mensaje mensaje = d.getValue(Mensaje.class);
                                                            if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).removeValue();
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                    }
                                                });
                                                Log.d("Logs", "mensaje eliminado");
                                            }
                                        });

                                        dialogo.setNegativeButton("Para todos", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialogo1, int id) {
                                                Toast.makeText(getContext(), "Has eliminado el mensaje " + model.getMensaje() + " para todos", Toast.LENGTH_SHORT).show();

                                                mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                            Mensaje mensaje = d.getValue(Mensaje.class);
                                                            if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).removeValue();
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                    }
                                                });
                                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                            Mensaje mensaje = d.getValue(Mensaje.class);
                                                            if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).removeValue();
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                    }
                                                });
                                                Log.d("Logs", "mensaje eliminado");
                                            }
                                        });
                                        //En el caso de que el usuario diga que no quiere borrarlo, pues no hará nada. se cerrará el dialogo
                                        dialogo.setNeutralButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialogo1, int id) {
                                                Log.d("Logs", "no se eliminara el mensaje");
                                            }
                                        });
                                        dialogo.show();

                                    } else if (id == R.id.popupMenu_emoji) {

                                        //POPUP MENU
                                        //Creating the instance of PopupMenu
                                        PopupMenu popupReaccionar = new PopupMenu(getContext(), holder.mensajeTextoDos);
                                        //Inflating the Popup using xml file
                                        popupReaccionar.getMenuInflater().inflate(R.menu.popup_reaccionar, popupReaccionar.getMenu());

                                        //registering popup with OnMenuItemClickListener
                                        popupReaccionar.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                            public boolean onMenuItemClick(MenuItem item) {
                                                int id = item.getItemId();
                                                if (id == R.id.popupReaccionar_like) {
                                                    if (model.getReaccion().equals("like")) {

                                                        Toast.makeText(getContext(), "Has quitado el like", Toast.LENGTH_SHORT).show();
                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("reaccion").setValue("");
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });
                                                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("reaccion").setValue("");
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });
                                                    } else {

                                                        Toast.makeText(getContext(), "Te ha gustado el mensaje", Toast.LENGTH_SHORT).show();
                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("reaccion").setValue("like");
                                                                    }
                                                                }
                                                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                     for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                            Mensaje mensaje = d.getValue(Mensaje.class);
                                                                            if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("reaccion").setValue("like");
                                                                            }
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                                    }
                                                                });
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });

                                                    }
                                                } else if (id == R.id.popupReaccionar_feliz) {
                                                    if (model.getReaccion().equals("feliz")) {

                                                        Toast.makeText(getContext(), "Has quitado el estado feliz", Toast.LENGTH_SHORT).show();
                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("reaccion").setValue("");
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });
                                                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("reaccion").setValue("");
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });
                                                    } else {

                                                        Toast.makeText(getContext(), "REaccion feliz", Toast.LENGTH_SHORT).show();
                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("reaccion").setValue("feliz");
                                                                    }
                                                                }
                                                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                            Mensaje mensaje = d.getValue(Mensaje.class);
                                                                            if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("reaccion").setValue("feliz");
                                                                            }
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                                    }
                                                                });
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });

                                                    }
                                                } else if (id == R.id.popupReaccionar_enfadado) {
                                                    if (model.getReaccion().equals("enfadado")) {

                                                        Toast.makeText(getContext(), "Has quitado el estado enfadado", Toast.LENGTH_SHORT).show();
                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("reaccion").setValue("");
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });
                                                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("reaccion").setValue("");
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });
                                                    } else {

                                                        Toast.makeText(getContext(), "estado enfadado", Toast.LENGTH_SHORT).show();
                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("reaccion").setValue("enfadado");
                                                                    }
                                                                }
                                                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                            Mensaje mensaje = d.getValue(Mensaje.class);
                                                                            if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("reaccion").setValue("enfadado");
                                                                            }
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                                    }
                                                                });
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });

                                                    }
                                                }
                                                return true;
                                            }
                                        });
                                        MenuPopupHelper menuHelperReaccionar = new MenuPopupHelper(getContext(), (MenuBuilder) popupReaccionar.getMenu(), holder.mensajeFotoPerfilDos);
                                        menuHelperReaccionar.setForceShowIcon(true);
                                        menuHelperReaccionar.show();
                                        //popupReaccionar.show();//showing popup menu
                                        //abrir_mapa();
                                    } else if (id == R.id.popupMenu_editarMensaje) {
                                        Log.d("Logs", "popupMenu_editarMensaje");
                                        //abrir_fotos();

                                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                        builder.setTitle("Escribe el mensaje editado");
                                        //Añadimos en la alerta un edit text
                                        final EditText input = new EditText(getContext());  //Creamos un edit text. para q el usuairo pueda insertar el titulo
                                        builder.setView(input);
                                        //Si el usuario da al ok
                                        builder.setPositiveButton("Guardar", new DialogInterface.OnClickListener() {  //Si el usuario acepta, mostramos otra alerta con los ejercicios que puede agregar
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                String mensajeNuevo = input.getText().toString();

                                                if (!mensajeNuevo.equals("")) {  //Comprobamos que haya ingresado un titulo, ya que si el titulo es nulo, no se creará la rutina

                                                    mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                Mensaje mensaje = d.getValue(Mensaje.class);
                                                                if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                    mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("mensaje").setValue("Editado: " + mensajeNuevo);
                                                                }
                                                            }
                                                            mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                    for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                        Mensaje mensaje = d.getValue(Mensaje.class);
                                                                        if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                            mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("mensaje").setValue("Editado: " + mensajeNuevo);
                                                                        }
                                                                    }
                                                                }

                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError error) {
                                                                }
                                                            });
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {
                                                        }
                                                    });


                                                }

                                            }
                                        });

                                        //Si se cancela, no se creará la rutina y se cancelará el dialogo
                                        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                            }
                                        });

                                        builder.show();
                                    }
                                    return true;
                                }
                            });

                            MenuPopupHelper menuHelper = new MenuPopupHelper(getContext(), (MenuBuilder) popup.getMenu(), holder.mensajeFotoPerfilDos);
                            menuHelper.setForceShowIcon(true);

                            if (model.getMensaje().contains("https://firebasestorage.googleapis.com/")) {
                                popup.getMenu().findItem(R.id.popupMenu_editarMensaje).setEnabled(false);
                            }
                            menuHelper.show();
                            //popup.show();//showing popup menu


                        }
                    });
                    holder.mensajeTextoDos.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            Log.d("Logs", "popupMenu_like");
                            if (model.getReaccion().equals("like")) {

                                Toast.makeText(getContext(), "Has quitado el like", Toast.LENGTH_SHORT).show();
                                mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                            Mensaje mensaje = d.getValue(Mensaje.class);
                                            if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("reaccion").setValue("");
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                    }
                                });
                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                            Mensaje mensaje = d.getValue(Mensaje.class);
                                            if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("reaccion").setValue("");
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                    }
                                });
                            } else {

                                Toast.makeText(getContext(), "Te ha gustado el mensaje", Toast.LENGTH_SHORT).show();
                                mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                            Mensaje mensaje = d.getValue(Mensaje.class);
                                            if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("reaccion").setValue("like");
                                            }
                                        }
                                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("reaccion").setValue("like");
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                    }
                                });

                            }
                            return false;
                        }
                    });
                } else {
                    holder.mensajeTextoUno.setVisibility(View.VISIBLE);//Pongo visible la info del otro usuario
                    holder.mensajeFotoPerfilUno.setVisibility(View.VISIBLE);
                    holder.mensajeHoraUno.setVisibility(View.VISIBLE);
                    holder.mensajeTextoDos.setVisibility(View.GONE);//Oculto la info del mio
                    holder.mensajeFotoPerfilDos.setVisibility(View.GONE);
                    holder.mensajeHoraDos.setVisibility(View.GONE);
                    holder.imageLikeDos.setVisibility(View.GONE);
                    holder.imageDobleCheckDos.setVisibility(View.GONE);

                    if (!model.getReaccion().equals("")) {
                        holder.imageLikeUno.setVisibility(View.VISIBLE);
                        if (model.getReaccion().equals("like")) {
                            holder.imageLikeUno.setImageDrawable(getContext().getResources().getDrawable(R.mipmap.ic_like));
                        } else if (model.getReaccion().equals("feliz")) {
                            holder.imageLikeUno.setImageDrawable(getContext().getResources().getDrawable(R.mipmap.ic_feliz));
                        } else if (model.getReaccion().equals("enfadado")) {
                            holder.imageLikeUno.setImageDrawable(getContext().getResources().getDrawable(R.mipmap.ic_enfadado));
                        }
                    } else {
                        holder.imageLikeUno.setVisibility(View.GONE);
                    }

                    if (model.getMensaje().contains("https://firebasestorage.googleapis.com/")) {

                        Glide.with(getContext())
                                .load(model.getMensaje())
                                .asBitmap()
                                .placeholder(R.drawable.place_holder_image)
                                .error(R.drawable.place_holder_image)
                                .into(new SimpleTarget<Bitmap>() {
                                    @Override
                                    public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                        Bitmap bitmap=resource;
                                        Bitmap myBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth()/4, bitmap.getHeight()/4, false);
                                        Spannable span = new SpannableString("I");
                                        ImageSpan image = new ImageSpan(getContext(), myBitmap);
                                        span.setSpan(image, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

                                        holder.mensajeTextoUno.setText(span, TextView.BufferType.SPANNABLE);

                                    }
                                    @Override
                                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                        super.onLoadFailed(e, errorDrawable);
                                    }
                                });
                    } else {
                        holder.mensajeTextoUno.setText(model.getMensaje()); //Pongo mi mezu
                    }

                    holder.mensajeHoraUno.setText(model.getHora());
                    Picasso.get().load(pFotoPerfil).into(holder.mensajeFotoPerfilUno); //Muestro la foto del otro
                    holder.mensajeTextoUno.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            //POPUP MENU
                            //Creating the instance of PopupMenu
                            PopupMenu popup = new PopupMenu(getContext(), holder.mensajeTextoDos);
                            //Inflating the Popup using xml file
                            popup.getMenuInflater().inflate(R.menu.popup_menu_mensaje, popup.getMenu());

                            //registering popup with OnMenuItemClickListener
                            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                public boolean onMenuItemClick(MenuItem item) {
                                    int id = item.getItemId();
                                    if (id == R.id.popupMenu_EliminarMensaje) {  //Si clicamos en rutinas, abriremos con navigation, la ventana donde se muestran las rutinas. he igual con todos
                                        Log.d("Logs", "popupMenu_EliminarMensaje");
                                        //abrir_chatAmigos();
                                        //Preguntamos si quiere eliminar el mensaje

                                        AlertDialog.Builder dialogo = new AlertDialog.Builder(v.getContext());
                                        dialogo.setTitle("Eliminar mensaje");
                                        dialogo.setMessage("Quieres eliminar el mensaje " + model.getMensaje() + "?");

                                        dialogo.setPositiveButton("Solo para mi", new DialogInterface.OnClickListener() {  //Botón si. es decir, queremos eliminar la rutina
                                            public void onClick(DialogInterface dialogo1, int id) {
                                                //Si dice que si quiere eliminar. Actualizamos la lista y lo borramos de la base de datos
                                                //Cogemos el id del elemento seleccionado por el usuario
                                                Toast.makeText(getContext(), "Has eliminado el mensaje " + model.getMensaje() + "solo para ti", Toast.LENGTH_SHORT).show();

                                                mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                            Mensaje mensaje = d.getValue(Mensaje.class);
                                                            if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).removeValue();
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                    }
                                                });
                                                Log.d("Logs", "mensaje eliminado");
                                            }
                                        });

                                        //En el caso de que el usuario diga que no quiere borrarlo, pues no hará nada. se cerrará el dialogo
                                        dialogo.setNeutralButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialogo1, int id) {
                                                Log.d("Logs", "no se eliminara el mensaje");
                                            }
                                        });
                                        dialogo.show();

                                    } else if (id == R.id.popupMenu_emoji) {

                                        //POPUP MENU
                                        //Creating the instance of PopupMenu
                                        PopupMenu popupReaccionar = new PopupMenu(getContext(), holder.mensajeTextoDos);
                                        //Inflating the Popup using xml file
                                        popupReaccionar.getMenuInflater().inflate(R.menu.popup_reaccionar, popupReaccionar.getMenu());

                                        //registering popup with OnMenuItemClickListener
                                        popupReaccionar.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                            public boolean onMenuItemClick(MenuItem item) {
                                                int id = item.getItemId();
                                                if (id == R.id.popupReaccionar_like) {
                                                    if (model.getReaccion().equals("like")) {

                                                        Toast.makeText(getContext(), "Has quitado el like", Toast.LENGTH_SHORT).show();
                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("reaccion").setValue("");
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });
                                                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("reaccion").setValue("");
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });
                                                    } else {

                                                        Toast.makeText(getContext(), "Te ha gustado el mensaje", Toast.LENGTH_SHORT).show();
                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("reaccion").setValue("like");
                                                                    }
                                                                }
                                                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                            Mensaje mensaje = d.getValue(Mensaje.class);
                                                                            if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("reaccion").setValue("like");
                                                                            }
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                                    }
                                                                });
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });

                                                    }
                                                } else if (id == R.id.popupReaccionar_feliz) {
                                                    if (model.getReaccion().equals("feliz")) {

                                                        Toast.makeText(getContext(), "Has quitado el estado feliz", Toast.LENGTH_SHORT).show();
                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("reaccion").setValue("");
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });
                                                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("reaccion").setValue("");
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });
                                                    } else {

                                                        Toast.makeText(getContext(), "REaccion feliz", Toast.LENGTH_SHORT).show();
                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("reaccion").setValue("feliz");
                                                                    }
                                                                }
                                                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                            Mensaje mensaje = d.getValue(Mensaje.class);
                                                                            if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("reaccion").setValue("feliz");
                                                                            }
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                                    }
                                                                });
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });

                                                    }
                                                } else if (id == R.id.popupReaccionar_enfadado) {
                                                    if (model.getReaccion().equals("enfadado")) {

                                                        Toast.makeText(getContext(), "Has quitado el estado enfadado", Toast.LENGTH_SHORT).show();
                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("reaccion").setValue("");
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });
                                                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("reaccion").setValue("");
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });
                                                    } else {

                                                        Toast.makeText(getContext(), "estado enfadado", Toast.LENGTH_SHORT).show();
                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("reaccion").setValue("enfadado");
                                                                    }
                                                                }
                                                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                            Mensaje mensaje = d.getValue(Mensaje.class);
                                                                            if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("reaccion").setValue("enfadado");
                                                                            }
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                                    }
                                                                });
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                            }
                                                        });

                                                    }
                                                }
                                                return true;
                                            }
                                        });
                                        MenuPopupHelper menuHelperReaccionar = new MenuPopupHelper(getContext(), (MenuBuilder) popupReaccionar.getMenu(), holder.mensajeFotoPerfilUno);
                                        menuHelperReaccionar.setForceShowIcon(true);
                                        menuHelperReaccionar.show();
                                        //popupReaccionar.show();//showing popup menu
                                        //abrir_mapa();
                                    } else if (id == R.id.popupMenu_editarMensaje) {


                                    }
                                    return true;
                                }
                            });

                            MenuPopupHelper menuHelper = new MenuPopupHelper(getContext(), (MenuBuilder) popup.getMenu(), holder.mensajeFotoPerfilUno);
                            menuHelper.setForceShowIcon(true);
                            popup.getMenu().findItem(R.id.popupMenu_editarMensaje).setVisible(false);
                            menuHelper.show();
                            //popup.show();//showing popup menu


                        }
                    });
                    holder.mensajeTextoUno.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            Log.d("Logs", "popupMenu_like");
                            if (model.getReaccion().equals("like")) {

                                Toast.makeText(getContext(), "Has quitado el like", Toast.LENGTH_SHORT).show();
                                mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                            Mensaje mensaje = d.getValue(Mensaje.class);
                                            if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("reaccion").setValue("");
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                    }
                                });
                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                            Mensaje mensaje = d.getValue(Mensaje.class);
                                            if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("reaccion").setValue("");
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                    }
                                });
                            } else {

                                Toast.makeText(getContext(), "Te ha gustado el mensaje", Toast.LENGTH_SHORT).show();
                                mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                            Mensaje mensaje = d.getValue(Mensaje.class);
                                            if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("reaccion").setValue("like");
                                            }
                                        }
                                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                    Mensaje mensaje = d.getValue(Mensaje.class);
                                                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                                                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("reaccion").setValue("like");
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                    }
                                });

                            }
                            return false;
                        }
                    });
                }


            }

            @NonNull
            @Override
            public ViewHolderMensaje onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mensaje, parent, false);


                return new ViewHolderMensaje(view);
            }
        };
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            public void onItemRangeInserted(int positionStart, int itemCount) {
                //messagesList.smoothScrollToPosition(adapter.getItemCount());
                Log.d("Logs", "cantidad adapter: " + adapter.getItemCount());
                cambiarMensajesALeido();
                ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(adapter.getItemCount() - 1, 200);
            }
        });

        adapter.startListening();

        recyclerView.setAdapter(adapter);
    }


    public void mandarMensaje(Uri uri) {
        //Mandar un mensaje.
        String mensaj = mensaje.getText().toString();
        if ((!mensaj.equals("") || uri != null)) { //El mensaje debe ser distinto de "", sino no se mandara
            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
            mensaje.setText(""); //Actualizamos el valor de mensaje a "" para q el usuario escriba otro mensaje


            //Creamos un hashMap que subiremos a la base de datos en la tabla MensajesChat, y guardaremos el mensaje enviado y el Uid del usuario que lo ha mandado, en este caso yo
            HashMap hashMap = new HashMap();
            if (uri != null) {
                hashMap.put("mensaje", uri.toString());
            } else {
                hashMap.put("mensaje", mensaj);
            }
            hashMap.put("usuario", mUser.getUid());
            hashMap.put("hora", formatter.format(date));
            hashMap.put("reaccion", "");
            hashMap.put("leido", "no");
            //Tenemos que hacer dos cosas, por un lado, subirlo con el titulo de mi usuario y subtitulo del otro usuario y por otro lado con el titulo del otro usuario y subtitulo de mi usuario, para tener las referencias con las dos personas
            mDatabaseRefMensajes.child(pId).child(mUser.getUid()).push().updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener() {
                @Override
                public void onSuccess(Object o) {
                    //Primero subido corectamente
                    mDatabaseRefMensajes.child(mUser.getUid()).child(pId).push().updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener() {
                        @Override
                        public void onSuccess(Object o) {
                            //Segundo subido correctamente

                            mandarNotificacion(mensaj);
                        }
                    });
                }
            });

        }
    }

    public void mandarNotificacion(String mensaje) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("to", "/topics/" + pId);
            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("title", "Mensaje de: " + miNombreUsuario);
            jsonObject1.put("body", mensaje);

            JSONObject jsonObject2 = new JSONObject();
            jsonObject2.put("userID", mUser.getUid());
            jsonObject2.put("type", "sms");


            jsonObject.put("notification", jsonObject1);
            jsonObject.put("data", jsonObject2);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, UrlNotif, jsonObject, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> map = new HashMap<>();
                    map.put("content-type", "application/json");
                    map.put("authorization", "key=AAAAiLCQj0o:APA91bEjfbUQE8VxAzxMDJZ8RpftfBB0qalOOiL-y-BMhY45Tk6pMakAtwxaDoI2SSeIjN4AYKPQLYt_yuL7Wy4N2KnbRb6IJ7IQB-iyOxPjGpnzcKCYdgWf0CyZcvMO-O7guX_KOjq6");
                    return map;
                }
            };
            requestQueue.add(request);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void cambiarEstado(String estado) {
        mDatabaseRef.child(mUser.getUid()).child("conectado").setValue(estado);
    }

    public void cambiarMensajesALeido() {
//Cambio los mensajes que no sean enviados por mi a leido
        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                    Mensaje mensaje = d.getValue(Mensaje.class);
                    String usuario=mensaje.getUsuario();
                    if(!usuario.equals(mUser.getUid())){
                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("leido").setValue("si");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                    Mensaje mensaje = d.getValue(Mensaje.class);
                    String usuario=mensaje.getUsuario();
                    if(!usuario.equals(mUser.getUid())){ //Cambio los mensajes que no sean enviados por mi a leido
                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("leido").setValue("si");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

    }
    ValueEventListener eventListenerEstado=null;
    public void mantenerEstadoActualizado(){
        eventListenerEstado=mDatabaseRef.child(pId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    pEstado = snapshot.child("conectado").getValue().toString();
                    barraStado.setText(pEstado); //no estan bloqueados asiq mostramos
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    @Override
    public void onDetach() {
        adapter.stopListening();
        recyclerView=null;
        adapter=null;
        mDatabaseRef.removeEventListener(eventListenerEstado);
        super.onDetach();
    }
}
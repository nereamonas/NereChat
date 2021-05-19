package com.example.nerechat.ui.chats;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
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
import androidx.constraintlayout.widget.ConstraintLayout;
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
import com.google.android.gms.tasks.OnFailureListener;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

public class ChatFragment extends Fragment {

    private BaseViewModel chatViewModel;

    //Se muestra el chat con una persona. es un poco monstruo esta clase ya que hace muchas cosas

    Toolbar toolbar;
    RecyclerView recyclerView;
    EditText mensaje;
    ImageView send, seleccionarImagen;
    CircleImageView barraPerfilImg;
    TextView barraUsername, barraStado;

    ConstraintLayout constraintLayout;
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
    ProgressDialog progressDialog;

    String tema="";
    boolean toastActivadas=false;

    int codigoRequestGaleria = 4;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        chatViewModel =
                new ViewModelProvider(this).get(BaseViewModel.class);
        View root = inflater.inflate(R.layout.fragment_chat, container, false);

        //Cogemos el extra que le hemos pasado. Hace referencia al uidDelOtroUsuario
        pId = getArguments().getString("usuario");

        //Hasieratuamos todos los valores
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

        progressDialog=new ProgressDialog(getContext()); //Inicializamos una barra de progreso que se activará cuando se envie una foto, ya que entre subir a firebase la imagen y luego enviarla tarda un poquito

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext()); //Cogemos las preferencias
        if (prefs.contains("notiftoast")) { //Comprobamos si existe notif
            toastActivadas = prefs.getBoolean("notiftoast", true);  //Comprobamos si las notificaciones estan activadas
        }
        if (prefs.contains("tema")) {
            tema = prefs.getString("tema", null); //Cogemos el tema
        }

        cambiarEstado("Conectado"); //Ponemos el estado a conectado

        requestQueue = Volley.newRequestQueue(getContext()); //será para mandar las notificaciones
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide(); //Ocultaremos la barra de estado y pondremos la barra personalizada
        //Cargamos el toolbar y cargamos la informacion del otro usaurio en el toolbar
        toolbar = root.findViewById(R.id.chat_toolbar);
        //setSupportActionBar(findViewById(R.id.chat_toolbar));
        constraintLayout=root.findViewById(R.id.toolbarChatLayout);

        //Ocultamos el buttonnavigationview ya que quita espacio al chat y no nos interesa en este fragmento
        BottomNavigationView nv = ((AppCompatActivity) getActivity()).findViewById(R.id.nav_view);
        nv.setVisibility(View.GONE);

        comprobarColores(); //Comprobamos los colores del toolbar con el tema seleccionado
        cargarInfoBarra(); //Cargamos la informacion del otro usuario en la barra de estado
        cargarMiFotoPerfil(); //Buscamos mi foto de perfil porque se usara en los mensajes;
        cambiarMensajesALeido(); //Todos los mensajes que ha mandado el otro usuario se pondran en leido ya que hemos entrado en la conversacion
        //Cargamos todos los mensajes que se han mandado con ese usuario
        cargarMensajes();

        send.setOnClickListener(new View.OnClickListener() { //Cuando clickemos en el boton send será que queremos mandar un mensaje. Asique mandamos el mensaje
            @Override
            public void onClick(View v) {
                mandarMensaje(null);
            }
        });

        seleccionarImagen.setOnClickListener(new View.OnClickListener() { //Cuando clickemos al boton de imagen de abajo a la izq, nos abrira la galeria para seleccionar una foto y despues mandarsela al otro usuario
            @Override
            public void onClick(View v) {
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);  //Creamos un intent de la galeria. para abrir la galeria
                startActivityForResult(gallery, codigoRequestGaleria); //Abrimos la actividad y pasamos el cogido de resultado para recibir cuando se finalice el intent

            }
        });

        barraPerfilImg.setOnClickListener(new View.OnClickListener() { //Si clicamos en el toolbar a la foto de perfil del otro usuario nos abrira el fragmento para ver la info de ese usuario
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle(); //Con el bundle podemos pasar datos
                bundle.putString("usuario", pId); //Tendresmo q pasarle el uuid del otro usuario para despues saber que datos coger
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_chatFragment_to_perfilOtroUsFragment, bundle, options);
            }
        });
        barraUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//Si clicamos en el toolbar a el nombre de usuario del otro usuario nos abrira el fragmento para ver la info de ese usuario
                Bundle bundle = new Bundle(); //Con el bundle podemos pasar datos
                bundle.putString("usuario", pId);//Tendresmo q pasarle el uuid del otro usuario para despues saber que datos coger
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

                //Abriremos un progressdialog para que el usuario no se altere si ve que tarda mucho en enviarse su foto. ya que entre que se sube a firebase la imagen, se coge su url y se añade el mensaje en la base de datos tarda un poquito
                progressDialog.setTitle(getString(R.string.progressDialog_enviandoImagen));
                progressDialog.setMessage(getString(R.string.progressDialog_porfavorespere));
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();

                if (uri != null) { //Y se debe haber seleccionado una foto de la galeria
                    String uuid = UUID.randomUUID().toString(); //Crearemos un identificador random para subir la foto
                    mStorageRef.child(uuid).putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) { //Si se ha subido correctamente
                            //Cogemos la url
                            mStorageRef.child(uuid).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    //Cogemos la fecha de hoy
                                    mandarMensaje(uri);
                                    progressDialog.dismiss(); //Cancelamos la barra de proceso
                                }
                            });

                        }
                    });
                    mStorageRef.child(uuid).putFile(uri).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss(); //Cancelamos la barra de proceso
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

                    //Mirar si tiene la ultima hora para mostrarla
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext()); //Cogemos las preferencias
                    if (prefs.contains("ultimaHora")) { //Comprobamos si existe notif
                        Boolean bloqueado = prefs.getBoolean("ultimaHora", true);  //Comprobamos si las notificaciones estan activadas
                        Log.d("Logs", "estado visulizar ultimaHora: " + bloqueado);
                        if (bloqueado) { //Si esta bloqueado no mostramos nada
                            barraStado.setText("");
                        } else {
                            mantenerEstadoActualizado(); //Queremos q el estado se mantenga actualizado
                            barraStado.setText(pEstado); //no estan bloqueados asiq mostramos
                        }
                    } else {
                        mantenerEstadoActualizado(); //Queremos q el estado se mantenga actualizado
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

                    if (!model.getReaccion().equals("")) { //Vemos si tiene alguna reaccion o no. si es !="" es que si
                        holder.imageLikeDos.setVisibility(View.VISIBLE); //Ponemos visible la imagen
                        if (model.getReaccion().equals("like")) { //Si la reaccion es like ponemos la imagen de like
                            holder.imageLikeDos.setImageDrawable(getContext().getResources().getDrawable(R.mipmap.ic_like));
                        } else if (model.getReaccion().equals("feliz")) { //Si la reaccione s feliz ponemos la imagen de feliz
                            holder.imageLikeDos.setImageDrawable(getContext().getResources().getDrawable(R.mipmap.ic_feliz));
                        } else if (model.getReaccion().equals("enfadado")) { //si la reaccion es enfadado ponemos la imagen de enfaddao
                            holder.imageLikeDos.setImageDrawable(getContext().getResources().getDrawable(R.mipmap.ic_enfadado));
                        }
                    } else { //Si no hay reaccion ponemos invisible la imgenview de reaccion
                        holder.imageLikeDos.setVisibility(View.GONE);
                    }
                    if (model.getLeido().equals("si")) { //Si el mensaje ha sido leido, ponemos el icono de doblecheck azul
                        holder.imageDobleCheckDos.setImageDrawable(getContext().getResources().getDrawable(R.mipmap.ic_doblecheck_azul));
                    }

                    if (model.getMensaje().contains("https://firebasestorage.googleapis.com/")) { //Si el mensaje contiene ese enlace es que es una foto, por lo que en vez de mostrar el mensaje normal, tenemos que mostrar la foto
                        //Para ello, haciendo uso de glide, conseguiremos coger la imagen mediante url y guardarla en un Bitmap, con ese bitmap crearemos un Spannable añadiendole la foto bitmap y se añadira al textview. asi podremos mostrar una imagen dentro de un textview
                        Glide.with(getContext())
                                .load(model.getMensaje())
                                .asBitmap()
                                .placeholder(R.drawable.place_holder_image)
                                .error(R.drawable.place_holder_image)
                                .into(new SimpleTarget<Bitmap>() {
                                    @Override
                                    public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                        Bitmap bitmap=resource; //Cogemos el bitmap
                                        Bitmap myBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth()/4, bitmap.getHeight()/4, false); //Lo escalamos con las medidas que nos interesa
                                        Spannable span = new SpannableString("I"); //Creamos un spanablestring
                                        ImageSpan image = new ImageSpan(getContext(), myBitmap); //Un imagespan con el bitmap
                                        span.setSpan(image, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE); //añadimos en el span la imagen en la posicion donde esta la i

                                        holder.mensajeTextoDos.setText(span, TextView.BufferType.SPANNABLE); //ponemos el span en el textview y asi se mostrara la imagen
                                    }
                                    @Override
                                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                        super.onLoadFailed(e, errorDrawable);
                                    }
                                });

                    } else {
                        holder.mensajeTextoDos.setText(model.getMensaje()); //si no es una foto pongo el mezu
                    }
                    holder.mensajeHoraDos.setText(model.getHora()); //Añadimos la hora
                    Picasso.get().load(miFotoPerfil).into(holder.mensajeFotoPerfilDos); //Y añadimos la foto de perfil
                    holder.mensajeTextoDos.setOnClickListener(new View.OnClickListener() { //Cuando se clicke encima del mensaje con un click simple
                        @Override
                        public void onClick(View v) {
                            //POPUP MENU. abrimos un pop up menu que nos da tres opciones: eliminar el mensaje, reaccionar a el o editarlo
                            PopupMenu popup = new PopupMenu(getContext(), holder.mensajeTextoDos); //El popup se pondra junto al mensaje
                            popup.getMenuInflater().inflate(R.menu.popup_menu_mensaje, popup.getMenu());

                            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { //Añadimos el click listener
                                public boolean onMenuItemClick(MenuItem item) {
                                    int id = item.getItemId();
                                    if (id == R.id.popupMenu_EliminarMensaje) {  //Si clicamos eliminar mensaje, mostramos una alerta que dara tres opciones, eliminar el mensaje solo para mi, eliminarlo para todos o cancelar
                                        Log.d("Logs", "popupMenu_EliminarMensaje");
                                        //Preguntamos si quiere eliminar el mensaje
                                        AlertDialog.Builder dialogo = new AlertDialog.Builder(v.getContext()); //Creamos la alerta
                                        dialogo.setTitle(getString(R.string.alerta_Eliminarmensaje));
                                        dialogo.setMessage(getString(R.string.alerta_Quiereseliminarelmensaje)+" '" + model.getMensaje() + "'?");

                                        dialogo.setPositiveButton(getString(R.string.alerta_Soloparami), new DialogInterface.OnClickListener() {  //Botón solo para mi. el mensaje se eliminara solo para mi, el otro usuario lo seguira viendo
                                            public void onClick(DialogInterface dialogo1, int id) {
                                                //Si dice que si quiere eliminar. Actualizamos la lista y lo borramos de la base de datos
                                                //Cogemos el id del elemento seleccionado por el usuario
                                                //Si las notificaciones toast estan activadas, mostramos un toast de q el mensaje se ha eliminado
                                                if (toastActivadas){Toast.makeText(getContext(), getString(R.string.toast_Haseliminadoelmensajesoloparati), Toast.LENGTH_SHORT).show();}
                                                eliminarMensajeParaMi(model); //Eliminar mensaje mi usuario - el otro
                                                Log.d("Logs", "mensaje eliminado");
                                            }
                                        });

                                        dialogo.setNegativeButton(getString(R.string.alerta_Paratodos), new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialogo1, int id) {
                                                if (toastActivadas){Toast.makeText(getContext(), getString(R.string.toast_haseliminadoelmensajeparatodos), Toast.LENGTH_SHORT).show();}
                                                eliminarMensajeParaMi(model); //Eliminar mensaje mi usuario - el otro
                                                eliminarMensajeParaElOtro(model);//Eliminar mensaje el otro - mi usuario
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
                                        PopupMenu popupReaccionar = new PopupMenu(getContext(), holder.mensajeTextoDos);
                                        popupReaccionar.getMenuInflater().inflate(R.menu.popup_reaccionar, popupReaccionar.getMenu());
                                        popupReaccionar.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                            public boolean onMenuItemClick(MenuItem item) {
                                                int id = item.getItemId();
                                                if (id == R.id.popupReaccionar_like) { //Quiere darle like
                                                    if (model.getReaccion().equals("like")) { //Si ya tiene la reaccion de like, la quitaremos
                                                        if (toastActivadas){Toast.makeText(getContext(), getString(R.string.toast_yanotegusta), Toast.LENGTH_SHORT).show();}
                                                        reaccionar(model,"");
                                                    } else { //Si no la tiene, la ponemos
                                                        if (toastActivadas){Toast.makeText(getContext(), getString(R.string.toast_Tehagustadoelmensaje), Toast.LENGTH_SHORT).show();}
                                                        reaccionar(model,"like");
                                                    }
                                                } else if (id == R.id.popupReaccionar_feliz) { //Quiere poner reaccion feliz
                                                    if (model.getReaccion().equals("feliz")) { //Si actualmente ya tiene la reaccion feliz, la quitamos
                                                        if (toastActivadas){Toast.makeText(getContext(), getString(R.string.toast_Hasquitadolareaccionfeliz), Toast.LENGTH_SHORT).show();}
                                                        reaccionar(model,"");
                                                    } else { //Si no tiene la reaccion feliz, la ponemos
                                                        if (toastActivadas) {Toast.makeText(getContext(), getString(R.string.toast_Reacciónfelizalmensaje), Toast.LENGTH_SHORT).show();}
                                                        reaccionar(model,"feliz");
                                                    }
                                                } else if (id == R.id.popupReaccionar_enfadado) { //Quiere poner reaccion enfadado
                                                    if (model.getReaccion().equals("enfadado")) { //Si ya la tiene puesta la quitara
                                                        if (toastActivadas){Toast.makeText(getContext(), getString(R.string.toast_Hasquitadolareacciónenfadado), Toast.LENGTH_SHORT).show();}
                                                        reaccionar(model,"");
                                                    } else { //Si no la tiene la pone
                                                        if (toastActivadas){Toast.makeText(getContext(), getString(R.string.toast_Reacciónenfadadoalmensaje), Toast.LENGTH_SHORT).show();}
                                                        reaccionar(model,"enfadado");
                                                    }
                                                }
                                                return true;
                                            }
                                        });
                                        MenuPopupHelper menuHelperReaccionar = new MenuPopupHelper(getContext(), (MenuBuilder) popupReaccionar.getMenu(), holder.mensajeFotoPerfilDos);
                                        menuHelperReaccionar.setForceShowIcon(true); //Para ponerle los iconos en la izquierda del texto
                                        menuHelperReaccionar.show(); //Lo mostramos

                                    } else if (id == R.id.popupMenu_editarMensaje) { //Quiere editar el mensaje
                                        Log.d("Logs", "popupMenu_editarMensaje");
                                        //Mostramos una alerta con un edittext para que el usuario ingrese el nuevo texto
                                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                        builder.setTitle(getString(R.string.alerta_Escribeelmensajeeditado));
                                        //Añadimos en la alerta un edit text
                                        final EditText input = new EditText(getContext());  //Creamos un edit text. para q el usuairo pueda insertar el titulo
                                        input.setText(model.getMensaje());
                                        builder.setView(input);
                                        //Si el usuario da al ok
                                        builder.setPositiveButton(getString(R.string.alerta_Guardar), new DialogInterface.OnClickListener() {  //Si el usuario acepta, mostramos otra alerta con los ejercicios que puede agregar
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                String mensajeNuevo = input.getText().toString();
                                                if (!mensajeNuevo.equals("")) {  //Comprobamos que haya ingresado un titulo, ya que si el titulo es nulo, no se creará la rutina
                                                    editarMensaje(model, mensajeNuevo); //Llamamos a editarmensaje pasandole el nuevo mensaje
                                                }
                                            }
                                        });

                                        //Si se cancela, no se actualizara nada
                                        builder.setNegativeButton(getString(R.string.alerta_Cancelar), new DialogInterface.OnClickListener() {
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

                            if (model.getMensaje().contains("https://firebasestorage.googleapis.com/")) { //En el caso de que sea una foto, desabilitaremos el boton editar, ya que no dejaremos editar una foto
                                popup.getMenu().findItem(R.id.popupMenu_editarMensaje).setEnabled(false);
                            }
                            menuHelper.show();
                        }
                    });
                    holder.mensajeTextoDos.setOnLongClickListener(new View.OnLongClickListener() { //cuando hacemos una pulsacion larga sobre el mensaje
                        @Override
                        public boolean onLongClick(View v) {
                            //Muchas veces queremos darle like al mensaje, para ponerle un acceso rapido, hacemos pulsacion larga sobre el mensaje y se pondra o quitara el like
                            Log.d("Logs", "popupMenu_like");
                            if (model.getReaccion().equals("like")) {
                                if(toastActivadas){ Toast.makeText(getContext(), getString(R.string.toast_yanotegusta), Toast.LENGTH_SHORT).show();}
                                reaccionar(model,"");
                            } else {
                                if(toastActivadas){Toast.makeText(getContext(), getString(R.string.toast_Tehagustadoelmensaje), Toast.LENGTH_SHORT).show();}
                                reaccionar(model,"like");
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
                            PopupMenu popup = new PopupMenu(getContext(), holder.mensajeTextoDos);
                            popup.getMenuInflater().inflate(R.menu.popup_menu_mensaje, popup.getMenu());

                            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                public boolean onMenuItemClick(MenuItem item) {
                                    int id = item.getItemId();
                                    if (id == R.id.popupMenu_EliminarMensaje) {  //Si clicamos en rutinas, abriremos con navigation, la ventana donde se muestran las rutinas. he igual con todos
                                        Log.d("Logs", "popupMenu_EliminarMensaje");
                                        //Preguntamos si quiere eliminar el mensaje
                                        AlertDialog.Builder dialogo = new AlertDialog.Builder(v.getContext());
                                        dialogo.setTitle(getString(R.string.alerta_Eliminarmensaje));
                                        dialogo.setMessage(getString(R.string.alerta_Quiereseliminarelmensaje)+" '" + model.getMensaje() + "'?");

                                        dialogo.setPositiveButton(getString(R.string.alerta_Soloparami), new DialogInterface.OnClickListener() {  //Botón si. es decir, queremos eliminar la rutina
                                            public void onClick(DialogInterface dialogo1, int id) {
                                                //Si dice que si quiere eliminar. Actualizamos la lista y lo borramos de la base de datos
                                                //Cogemos el id del elemento seleccionado por el usuario
                                                if(toastActivadas){Toast.makeText(getContext(), getString(R.string.toast_Haseliminadoelmensajesoloparati), Toast.LENGTH_SHORT).show();}
                                                eliminarMensajeParaMi(model); //Eliminar mensaje mi usuario - el otro
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
                                        PopupMenu popupReaccionar = new PopupMenu(getContext(), holder.mensajeTextoDos);
                                        popupReaccionar.getMenuInflater().inflate(R.menu.popup_reaccionar, popupReaccionar.getMenu());
                                        popupReaccionar.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                            public boolean onMenuItemClick(MenuItem item) {
                                                int id = item.getItemId();
                                                if (id == R.id.popupReaccionar_like) { //Reaccion like
                                                    if (model.getReaccion().equals("like")) {//Ya esta activo asique lo quitamos
                                                        if(toastActivadas){Toast.makeText(getContext(), getString(R.string.toast_yanotegusta), Toast.LENGTH_SHORT).show();}
                                                        reaccionar(model,"");
                                                    } else { //No tiene esa reaccion asique la ponemos
                                                        if(toastActivadas){Toast.makeText(getContext(), getString(R.string.toast_Tehagustadoelmensaje), Toast.LENGTH_SHORT).show();}
                                                        reaccionar(model,"like");
                                                    }
                                                } else if (id == R.id.popupReaccionar_feliz) { //reaccion feliz
                                                    if (model.getReaccion().equals("feliz")) {//Ya esta activo asique lo quitamos
                                                        if(toastActivadas){Toast.makeText(getContext(), getString(R.string.toast_Hasquitadolareaccionfeliz), Toast.LENGTH_SHORT).show();}
                                                        reaccionar(model,"");
                                                    } else {//No tiene esa reaccion asique la ponemos
                                                        if(toastActivadas){Toast.makeText(getContext(), getString(R.string.toast_Reacciónfelizalmensaje), Toast.LENGTH_SHORT).show();}
                                                        reaccionar(model,"feliz");
                                                    }
                                                } else if (id == R.id.popupReaccionar_enfadado) { //reaccion enfadado
                                                    if (model.getReaccion().equals("enfadado")) { //Ya esta activo asique lo quitamos
                                                        if(toastActivadas){Toast.makeText(getContext(), getString(R.string.toast_Hasquitadolareacciónenfadado), Toast.LENGTH_SHORT).show();}
                                                        reaccionar(model,"");
                                                    } else {//No tiene esa reaccion asique la ponemos
                                                        if(toastActivadas){Toast.makeText(getContext(), getString(R.string.toast_Reacciónenfadadoalmensaje), Toast.LENGTH_SHORT).show();}
                                                        reaccionar(model,"enfadado");
                                                    }
                                                }
                                                return true;
                                            }
                                        });
                                        MenuPopupHelper menuHelperReaccionar = new MenuPopupHelper(getContext(), (MenuBuilder) popupReaccionar.getMenu(), holder.mensajeFotoPerfilUno);
                                        menuHelperReaccionar.setForceShowIcon(true);
                                        menuHelperReaccionar.show();
                                    }
                                    return true;
                                }
                            });

                            MenuPopupHelper menuHelper = new MenuPopupHelper(getContext(), (MenuBuilder) popup.getMenu(), holder.mensajeFotoPerfilUno);
                            menuHelper.setForceShowIcon(true);
                            popup.getMenu().findItem(R.id.popupMenu_editarMensaje).setVisible(false);
                            menuHelper.show();
                        }
                    });
                    holder.mensajeTextoUno.setOnLongClickListener(new View.OnLongClickListener() { //Lo mismo, al hacer una pulsacion larga, ponemos o quitamos el like
                        @Override
                        public boolean onLongClick(View v) {
                            Log.d("Logs", "popupMenu_like");
                            if (model.getReaccion().equals("like")) {
                                if(toastActivadas)
                                Toast.makeText(getContext(), getString(R.string.toast_yanotegusta), Toast.LENGTH_SHORT).show();
                                reaccionar(model,"");
                            } else {
                                if(toastActivadas)
                                Toast.makeText(getContext(), getString(R.string.toast_Tehagustadoelmensaje), Toast.LENGTH_SHORT).show();
                                reaccionar(model,"like");
                            }
                            return false;
                        }
                    });
                }
                //Dependiendo del tema el fondo del mensaje será uno u otro. en el caso de ser azul seran azules y en caso contrario sera blanco y gris
                if(tema.equals("morado")||tema.equals("naranja")||tema.equals("verde")||tema.equals("verdeazul")){
                    holder.mensajeTextoDos.setBackground(getResources().getDrawable(R.drawable.fondo_mensaje_uno_blanco));
                    holder.mensajeTextoUno.setBackground(getResources().getDrawable(R.drawable.fondo_mensaje_dos_blanco));
                }else{
                    holder.mensajeTextoDos.setBackground(getResources().getDrawable(R.drawable.fondo_mensaje_uno));
                    holder.mensajeTextoUno.setBackground(getResources().getDrawable(R.drawable.fondo_mensaje_dos));
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
                cambiarMensajesALeido(); //Cada vez que haya algun cambio cambiamos a leido los mensajes
                ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(adapter.getItemCount() - 1, 200);
            }
        });

        adapter.startListening();
        recyclerView.setAdapter(adapter);
    }


    public void mandarMensaje(Uri uri) { //Mandar un mensaje. ponemos uri como parametro  y cuando se envie una foto este parametro ira informado
        //Mandar un mensaje.
        String mensaj = mensaje.getText().toString(); //Cogemos el valor del mensaje
        if ((!mensaj.equals("") || uri != null)) { //El mensaje debe ser distinto de "", o la uri debe ser distinto de null sino no se mandara
            Date date = new Date(); //Cargamos la hora exacta en la que estamos para enviar el mensaje
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
            mensaje.setText(""); //Actualizamos el valor de mensaje a "" para q el usuario escriba otro mensaje

            //Creamos un hashMap que subiremos a la base de datos en la tabla MensajesChat, y guardaremos el mensaje enviado y el Uid del usuario que lo ha mandado, en este caso yo
            HashMap hashMap = new HashMap();
            if (uri != null) { //Si la uri es !=null es porq lo que hay que subir es una foto
                hashMap.put("mensaje", uri.toString()); //ponemos su url
            } else { //Si no es un mensaje normal
                hashMap.put("mensaje", mensaj); //ponemos el mensaje normal
            }
            hashMap.put("usuario", mUser.getUid()); //ponemos el uid del usuario
            hashMap.put("hora", formatter.format(date)); //la hora
            hashMap.put("reaccion", ""); //En principio no tiene ninguna reaccion
            hashMap.put("leido", "no"); //Y el mensaje no ha sido leido
            //Tenemos que hacer dos cosas, por un lado, subirlo con el titulo de mi usuario y subtitulo del otro usuario y por otro lado con el titulo del otro usuario y subtitulo de mi usuario, para tener las referencias con las dos personas
            mDatabaseRefMensajes.child(pId).child(mUser.getUid()).push().updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener() {
                @Override
                public void onSuccess(Object o) {
                    //Primero subido corectamente
                    mDatabaseRefMensajes.child(mUser.getUid()).child(pId).push().updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener() {
                        @Override
                        public void onSuccess(Object o) {
                            //Segundo subido correctamente
                            mandarNotificacion(mensaj); //Asique como se ha subido bien, mandamos la notificacion al otro usuariod e que tiene un nuevo mensaje
                        }
                    });
                }
            });

        }
    }

    public void mandarNotificacion(String mensaje) { //Mandaremos la notificacion a firebasecloud message
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("to", "/topics/" + pId); //Se enviara solo al usuario suscrito a pid. por lo cual solo está suscrito un usuario a cada tema
            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("title", "Mensaje de: " + miNombreUsuario); //Como titulo: tienees un mensaje de: mi nombre de usuario
            jsonObject1.put("body", mensaje); //Como cuerpo el contenido del mensaje

            JSONObject jsonObject2 = new JSONObject(); //Como objeto dos, añadiremos mi uuid, para que cuando se habra la notificacion navege directamente hasta el chat conmigo
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


    public void cambiarEstado(String estado) { //Cambiamos el estado a conectado
        mDatabaseRef.child(mUser.getUid()).child("conectado").setValue(estado);
    }

    public void cambiarMensajesALeido() {
//Cambio los mensajes que no sean enviados por mi a leido
        //En los dos casos, cuando es mi usuario - el otro usuario y cuando es el otro usuario-mi usuario
        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                    Mensaje mensaje = d.getValue(Mensaje.class); //Cogemos el mensaje
                    String usuario=mensaje.getUsuario(); //Cogemos su usuario
                    if(!usuario.equals(mUser.getUid())){ //Si no lo he enviado yo, lo ponemos a leido si
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
    public void mantenerEstadoActualizado(){ //Mantendremos el estado siempre activo. para ello se añade un addValueEventListener en vez de el que solo lo hace una vez
        //Ya que si el usuario entra en la conversacion cuando tu ya estas, sino no se actualizaria y saldria que no esta conectado, y lo mismo al reves
        eventListenerEstado=mDatabaseRef.child(pId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) { //Si existe el usuario cogemos el valor del parametro conectado
                    pEstado = snapshot.child("conectado").getValue().toString();
                    barraStado.setText(pEstado); //no estan bloqueados asiq mostramos
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    public void eliminarMensajeParaMi(Mensaje model){
        //Cogemos la referencia de la base de datos donde esta el mensaje. mi usuario - el otro usuario.
        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                    Mensaje mensaje = d.getValue(Mensaje.class); //Cogemos el mensaje
                    //Si el mensaje es igual al que queremos borrar
                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).removeValue(); //eliminamos el mensaje
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
    public void eliminarMensajeParaElOtro(Mensaje model){
        //Cogemos la referencia de la base de datos donde esta el mensaje. el otro usuario - mi usuario
        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                    Mensaje mensaje = d.getValue(Mensaje.class); //Cogemos el mensaje
                    //Si el mensaje es igual al que queremos borrar
                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) {
                        mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).removeValue(); //eliminamos el mensaje
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    public void reaccionar(Mensaje model, String reaccion){ //Daremos like a un mensaje
        //Cogemos la referencia del mensaje. tenemos q editarlo de mi usuario y del otro usuario
        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada mensaje )
                    Mensaje mensaje = d.getValue(Mensaje.class);
                    if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) { //Si es igual al mensaje que hemos reaccionado
                        mDatabaseRefMensajes.child(mUser.getUid()).child(pId).child(d.getKey()).child("reaccion").setValue(reaccion); //Cambiamos su reaccion a like
                    }
                }
                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                            Mensaje mensaje = d.getValue(Mensaje.class);
                            if (mensaje.getUsuario().equals(model.getUsuario()) && mensaje.getHora().equals(model.getHora()) && mensaje.getMensaje().equals(model.getMensaje())) { //Si es igual al mensaje que hemos reaccionado
                                mDatabaseRefMensajes.child(pId).child(mUser.getUid()).child(d.getKey()).child("reaccion").setValue(reaccion); //Cambiamos su reaccion a like
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

    public void editarMensaje(Mensaje model, String mensajeNuevo){
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

    @Override
    public void onDetach() {
        //Cuando le demeos atras tenemos que parar to do para que no nos de errores
        adapter.stopListening(); //El adaptador parara de escuchar
        recyclerView=null; //ponemos a null la recycler view
        adapter=null;//Ponemos a null el adaptador
        if(eventListenerEstado!=null) //Si el eventlistener para mantener el estado activado es distinto de null
            mDatabaseRef.removeEventListener(eventListenerEstado); //Eliminamos ese listener para q no de problemas
        super.onDetach();
    }

    public void comprobarColores(){
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
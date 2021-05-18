package com.example.nerechat.ui.mapa;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;


import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.example.nerechat.R;
import com.example.nerechat.base.BaseFragment;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.android.ui.IconGenerator;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MapsFragment extends BaseFragment implements GoogleMap.OnPolylineClickListener {
    //Clase google maps. Se ha inplementado un mapa donde desde tu ubicación actual puedes calcular la distancia a cualquier ubicacion que elijas en el mapa

    //Inicializamos todas las variables que usaremos
    // private MutableLiveData<String> mText;
    private GoogleMap map;
    private GeoApiContext mGeoApiContext = null;
    private LatLng posUsuario;
    private ArrayList<PolylineData> mPolylineData = new ArrayList<>();
    private Marker selectMarker = null;
    private LocationCallback actualizador = null;
    private LocationRequest peticion = null;
    private FusedLocationProviderClient proveedordelocalizacion = null;
    private SupportMapFragment mapFragment;
    private Bundle mapBundle = null;
    private boolean continua = false;


    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRef,mDatabaseRefMapa;


    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        @Override
        public void onMapReady(GoogleMap googleMap) {
            map = googleMap;

            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    //Cuando clicamos encima de la marca, preguntaremos a ver si quiere calcular la distancia a ese punto

                    AlertDialog.Builder dialogo = new AlertDialog.Builder(getContext()); //Abrimos un dialogo para preguntar si quiere calcular la distancia
                    dialogo.setTitle(getString(R.string.maps_calculardistancia));
                    dialogo.setMessage(getString(R.string.maps_quierescalcularelcaminohastaestepunto));
                    //dialogo.setCancelable(false);
                    dialogo.setPositiveButton(getString(R.string.si), new DialogInterface.OnClickListener() {  //En el caso de SI
                        public void onClick(DialogInterface dialogo1, int id) {
                            selectMarker=marker; //el marcador actual lo guardamos
                            calculateDirections(marker); //Calcularemos las direcciones

                        }
                    });
                    dialogo.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {  //En el caso de no, no haremos nada
                        public void onClick(DialogInterface dialogo1, int id) {

                        }
                    });
                    dialogo.show(); //Mostramos el dialogo
                }

            });
            Double[] cordenadaActual = getCordenadas();  //cogemos las cordenadas actuales del usuario
            posUsuario= new LatLng(cordenadaActual[0],cordenadaActual[1]); //Guardamos las cordenaddas como la posicion del usuario

            guardarFirebasePosUsuario(posUsuario);

            Log.d("Logs", "lo q recibe: " + cordenadaActual[0] + "  " + cordenadaActual[1]);

            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            //Poner el circulito azul de mi localizacion
            map.setMyLocationEnabled(true);

        }
    };


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        //mText = new MutableLiveData<>();
        View root=inflater.inflate(R.layout.fragment_maps, container, false);

        Button IW_vista=root.findViewById(R.id.IW_vista);
       // IW_vista.setTextColor(getResources().getColor(R.color.white));
       // IW_vista.setBackgroundColor(getResources().getColor(R.color.azul_oscuro));

        IW_vista.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //POPUP MENU
                //Creating the instance of PopupMenu
                PopupMenu popup = new PopupMenu(getContext(), IW_vista);
                //Inflating the Popup using xml file
                popup.getMenuInflater().inflate(R.menu.popup_maps, popup.getMenu());

                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        if (id == R.id.popupMaps_relieve) {
                            map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                        } else if (id == R.id.popupMaps_hibrido) {
                            map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        } else if (id == R.id.popupMaps_satelite) {
                            map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        }else if (id == R.id.popupMaps_normal) {
                            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        }
                        return true;
                    }
                });
                MenuPopupHelper menuHelper = new MenuPopupHelper(getContext(), (MenuBuilder) popup.getMenu(),IW_vista);
                menuHelper.setForceShowIcon(true);
                menuHelper.show();

            }
        });

        return root;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapBundle = null;
        if (savedInstanceState != null) {
            mapBundle = savedInstanceState.getBundle("MapViewBundleKey");
        }
        //Primero pedimos permiso para leer las cordenadas
        permisoCordenadas();
        if (continua) { //Si el permiso esta concedido, podemos empezar a cargar las cosas
            empezar();

        }

    }

    public void empezar() {
        mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        mapFragment.onCreate(mapBundle);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
        if (mGeoApiContext == null) {
            mGeoApiContext = new GeoApiContext.Builder().apiKey("AIzaSyACvDLNNsrOwo4oqDuBRUoW2eedc6DL5R8").build();  //Le pasamos nuestro apikey de maps
        }
        //Cogemos toda la lista de mapas de usuarios y creamos una marca por cada uno
        mAuth= FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser();
        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Perfil"); //La base de datos perfil
        mDatabaseRefMapa= FirebaseDatabase.getInstance().getReference().child("Mapa"); //Y la base de datos mensajeChat donde se almacenarán todos los mensajes

        cargarPosicionesUsuarios();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);
        Bundle mapBundle = outState.getBundle("MapViewBundleKey"); //Cogemos el almacenado
        if (mapBundle == null) { //Si es null creamos uno nuevo
            mapBundle = new Bundle();
            outState.putBundle("MapViewBundleKey", mapBundle);
        }
        if (continua) {
            mapFragment.onSaveInstanceState(mapBundle);
        }
    }

    public void permisoCordenadas() { //Pedimos permiso para acceder a la ubicacion del usuario
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            Log.d("Logs", "NO ESTA CONCEDIDO");
            //EL PERMISO NO ESTÁ CONCEDIDO, PEDIRLO
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                // MOSTRAR AL USUARIO UNA EXPLICACIÓN DE POR QUÉ ES NECESARIO EL PERMISO
                Log.d("Logs", "DECIR XQ ES NECESARIO");
            } else {
                //EL PERMISO NO ESTÁ CONCEDIDO TODAVÍA O EL USUARIO HA INDICADO
                //QUE NO QUIERE QUE SE LE VUELVA A SOLICITAR
                Log.d("Logs", "NO SE HA CONCEDIDO");
            }
            //PEDIR EL PERMISO
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);  //ponemos request code para recibir cuando se realice la accion
            Log.d("Logs", "PEDIR PERMISO");

        } else {
            Log.d("Logs", "YA LO TIENE");
            continua = true;
            //EL PERMISO ESTÁ CONCEDIDO, EJECUTAR LA FUNCIONALIDAD
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == 1) { //Ya se ha respondido al permiso
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) { //Si se ha aceptado el permiso
                continua = true; //Continuar=true.
                empezar(); //cargamos los datos iniciales
            }
            return;
        }
    }

    public Double[] getCordenadas() {  //Metodo que returnea las cordenadas actuales del usu
        Double[] cordenadas = new Double[2];
        cordenadas[0] = Double.valueOf(43.0504629);  //Ponemos unas por defecto
        cordenadas[1] = Double.valueOf(-3.0070026);  //Ponemos una x defecto

        proveedordelocalizacion =
                LocationServices.getFusedLocationProviderClient(getContext());

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return cordenadas;
        }
        proveedordelocalizacion.getLastLocation()
                .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            Log.d("Logs", "Localización conseguida");
                            LatLng posicion = new LatLng(location.getLatitude(), location.getLongitude());
                            crearPuntoIni(posicion);
                        } else {
                            Log.d("Logs", "No se ha coonseguido la localizacion");
                        }
                    }
                })
                .addOnFailureListener(getActivity(), new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("Logs", "FATAL");
                        getCordenadas();
                    }
                });


        //cambios de posicion)

        peticion = LocationRequest.create();
        peticion.setInterval(1000);
        peticion.setFastestInterval(5000);

        peticion.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        actualizador = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null) {
                    cordenadas[0] = locationResult.getLastLocation().getLatitude();
                    cordenadas[1] = locationResult.getLastLocation().getLongitude();
                    Log.d("Logs", "" + cordenadas[0] + "  " + cordenadas[1]);
                    actualizarPunto(cordenadas);
                } else {
                    Log.d("Logs", "NO se ha actualizado la loc");
                }
            }
        };

        proveedordelocalizacion.requestLocationUpdates(peticion, actualizador, null);

        return cordenadas;

    }

    public void crearPuntoIni(LatLng posicion) {  //Creamos el punto inicial desde donde se van a calcular todas las distancias a los marcadores puestos posteriormente
       // map.addMarker(new MarkerOptions().position(posicion).title(getString(R.string.maps_puntoInicial)));  //Añadimos marcador con el nombre punto inicial al clicar sobre el
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(posicion, 15)); //Movemos la camara, para que se centre en el punto
    }

    public void crearPunto(LatLng posicion, String usuario, String foto) { //Creamos un punto. igual q el punto inicial, pero tendra otro titulo diferente


        Glide.with(getContext())
                .load(foto)
                .asBitmap()
                .placeholder(R.drawable.place_holder_image)
                .error(R.drawable.place_holder_image)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        Bitmap bitmap=resource;

                        ImageView imagen = new ImageView(getContext());
                        imagen.setImageBitmap(bitmap);
                        IconGenerator mIconGenerator = new IconGenerator(getActivity().getApplicationContext());
                        imagen.setLayoutParams(new ViewGroup.LayoutParams((int)getContext().getResources().getDimension(R.dimen.marker_imagen),(int)getContext().getResources().getDimension(R.dimen.marker_imagen)));
                        int padding=(int) getContext().getResources().getDimension(R.dimen.marker_padding);
                        imagen.setPadding(padding,padding,padding,padding);
                        mIconGenerator.setContentView(imagen);

                        Bitmap icon=mIconGenerator.makeIcon();
                        map.addMarker(new MarkerOptions().position(posicion).title(usuario).snippet("Toca para calcular el trayecto hasta "+usuario).icon(BitmapDescriptorFactory.fromBitmap(icon)));

                    }
                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        super.onLoadFailed(e, errorDrawable);
                    }
                });




        Log.d("Logs","Añadir marca"+ usuario+"imagen: "+foto);
       // map.addMarker(new MarkerOptions().position(posicion).title("Usuario: "+usuario));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(posicion, 15));
    }

    public void actualizarPunto(Double[] cordenadas) {
        posUsuario=new LatLng(cordenadas[0],cordenadas[1]);
        guardarFirebasePosUsuario(posUsuario);
        CameraUpdate actualizar = CameraUpdateFactory.newLatLngZoom(new LatLng(cordenadas[0], cordenadas[1]), 15);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
    }

    private void calculateDirections(Marker marker){ //Calcular la direcciones entre el punto del usuario hasta el marcador seleccionado
        Log.d("Logs", "calculateDirections: calculating directions.");

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                marker.getPosition().latitude,
                marker.getPosition().longitude
        );  //LagLng destino
        DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext); //Creamos las direcciones

        directions.alternatives(true); //Queremos tambien rutas alternaticas
        directions.origin(
                new com.google.maps.model.LatLng(
                        posUsuario.latitude,
                        posUsuario.longitude
                ));  //Marcamos cual es el origen

        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) { //Printeamos todos los datos para ver la informacion del polyline
                Log.d("Logs", "Direccion: ruta: " + result.routes[0].toString());
                Log.d("Logs", "Direccion: duración: " + result.routes[0].legs[0].duration);
                Log.d("Logs", "Direccion: distancia: " + result.routes[0].legs[0].distance);
                addPolylinesToMap(result); //Añadimos el polyline
            }

            @Override
            public void onFailure(Throwable e) { //si falla mostramos log
                Log.e("Logs", "No se ha podido conseguir la direccion " + e.getMessage() );
            }
        });
    }

    private void addPolylinesToMap(final DirectionsResult result){ //Añadimos la linea al mapa
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d("Logs", "Rutas encontradas: " + result.routes.length);

                //if(mPolylineData.size()>0){ //Si tenemos ya guardadas x lineas, eliminamos las guardadas actualmente y creamos un nuevo array.
                //    mPolylineData= new ArrayList<>();
                //}
                int cont=1;
                for(DirectionsRoute route: result.routes){  //Recorremos todas las rutas posibles que tengamos
                    List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                    List<LatLng> newDecodedPath = new ArrayList<>();

                    for(com.google.maps.model.LatLng latLng: decodedPath){
                        newDecodedPath.add(new LatLng(
                                latLng.lat,
                                latLng.lng
                        ));
                    }
                    Polyline polyline = map.addPolyline(new PolylineOptions().addAll(newDecodedPath)); //Añadimos al mapa la linea
                    polyline.setColor(ContextCompat.getColor(getActivity(), R.color.grey)); //Sera de color gris
                    polyline.setClickable(true); //Es clicable, para hacer que cuando se clicke se ponga de color azul
                    mPolylineData.add(new PolylineData(polyline,route.legs[0]));
                    onPolylineClick(polyline);
                    //selectMarker.setVisible(false);
                    if(cont==1){  //En la primera que nos ofrezca la cogeremos por defecto y lo ponemos de color azul para q resalte mas
                        polyline.setColor(ContextCompat.getColor(getActivity(), R.color.blue));
                        polyline.setZIndex(1);
                    }
                    cont++;

                }
            }
        });
    }


    @Override
    public void onPolylineClick(Polyline polyline) {  //Cuando clickemos una de las lineas, cambiaremos los estados. Por un lado pondremos la linea seleccionada en azul y el resto en gris, y en el titulo del marcador, pondremos la distancia que tienes por esa nueva ruta seleccionada y el tiempo que llevará
        Log.d("Logs", "click polyline");
        int cont=1;
        for(PolylineData polylineData: mPolylineData){ //Recorremos todas las lineas q tenemos
            Log.d("Logs", "onPolylineClick: toString: " + polylineData.toString());
            if(polyline.getId().equals(polylineData.getPolyline().getId())){ //Si es la linea seleccionada
                polylineData.getPolyline().setColor(ContextCompat.getColor(getActivity(), R.color.blue)); //La pintamos en azul
                polylineData.getPolyline().setZIndex(1);

                LatLng endLoc= new LatLng(polylineData.getLeg().endLocation.lat, polylineData.getLeg().endLocation.lng);
                selectMarker.setTitle(getString(R.string.maps_trayecto)+" "+cont);
                selectMarker.setSnippet((getString(R.string.maps_duracion)+": "+polylineData.getLeg().duration+", "+getString(R.string.maps_kilometros)+": "+polylineData.getLeg().distance));
                //Marker marker=map.addMarker(new MarkerOptions().position(endLoc).title(getString(R.string.maps_trayecto)+" "+cont).snippet(getString(R.string.maps_duracion)+": "+polylineData.getLeg().duration+", "+getString(R.string.maps_kilometros)+": "+polylineData.getLeg().distance));//Cambiamos el titulo del marcador

                //marker.showInfoWindow(); //Lo visualizamos
            }
            else{ //Si no es la linea seleccionada
                polylineData.getPolyline().setColor(ContextCompat.getColor(getActivity(), R.color.grey)); //Lo ponemos de color gris
                polylineData.getPolyline().setZIndex(0);
            }
            cont++;
        }
    }


    @Override
    public void onDetach() {  //Este metodo detecta cuando pulsamos la tecla atras en el mvl. Es importante cancelar el temporizador, ya que si al darle atras no se cancela, cuando este acabe peta la aplicacion
        Log.d("Logs","DETACH"); //Añadimos un log para comprobar
        cancelarTodo();
        super.onDetach();
    }

    public void cancelarTodo(){ //Cancelamos to dos los elementos que tenemos inicializados para que no salten errores
        map=null;
        mGeoApiContext=null;
        posUsuario=null;
        mPolylineData=null;
        selectMarker=null;
        if(proveedordelocalizacion!=null) {
            proveedordelocalizacion.removeLocationUpdates(actualizador);
        }
        proveedordelocalizacion=null;
        this.actualizador=null;
        peticion=null;

    }

    public void guardarFirebasePosUsuario(LatLng posUsuario){
        HashMap hashMap=new HashMap();
        hashMap.put("usuario",mUser.getUid());
        hashMap.put("latitude",posUsuario.latitude);
        hashMap.put("longitude",posUsuario.longitude);

        mDatabaseRefMapa.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild(mUser.getUid())){ //Si ya existe editamos los valores
                    mDatabaseRefMapa.child(mUser.getUid()).child("latitude").setValue(posUsuario.latitude);
                    mDatabaseRefMapa.child(mUser.getUid()).child("longitude").setValue(posUsuario.longitude);
                }else{//Si no existe lo añadimos
                    mDatabaseRefMapa.child(mUser.getUid()).updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener() {
                        @Override
                        public void onSuccess(Object o) {
                        }
                    });

                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void cargarPosicionesUsuarios(){
        mDatabaseRefMapa.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot d: snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                    MapaElement mapa=d.getValue(MapaElement.class);
                    //Ahora cogemos el nombre del usuario y su foto
                    //Tenemos que coger de la base de datos la informacion del otro usuario. como tenemos su UID es sencillo
                    mDatabaseRef.child(mapa.getUsuario()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists()){
                                //Si existe el usuario
                                String fotoPerfil=snapshot.child("fotoPerfil").getValue().toString(); //Cogemos mi foto de perfil
                                String nombreUsuario=snapshot.child("nombreUsuario").getValue().toString();

                                crearPunto(new LatLng(mapa.latitude,mapa.longitude),nombreUsuario,fotoPerfil);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }
}
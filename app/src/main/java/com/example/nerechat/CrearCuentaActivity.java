package com.example.nerechat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nerechat.base.BaseActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class CrearCuentaActivity extends BaseActivity {
    //Ventana para crear una nueva cuenta

    private TextInputLayout inputemail, inputpass, inputrepPass;
    FirebaseAuth mAuth;
    ProgressDialog progressDialog;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_cuenta);

        //Inicializamos las variables
        inputemail=findViewById(R.id.inputLCrearEmail);
        inputpass=findViewById(R.id.inputLCrearPass);
        inputrepPass=findViewById(R.id.inputLCrearRepPass);

        mAuth=FirebaseAuth.getInstance();
        progressDialog=new ProgressDialog(this);

        TextView text_iniciarSesion= findViewById(R.id.text_iniciarSesion);
        text_iniciarSesion.setOnClickListener(new View.OnClickListener() {  //Cuando clickemos en el boton camara. pediremos permisos para abrir la camara y guardar las fotos
            @Override
            public void onClick(View v) {
                abrirIniciarSesion();
            }
        });



    }

    public void abrirIniciarSesion(){ //Abrimos la pantalla de inicio de sesion
        Intent i = new Intent(this, IniciarSesionActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    public void abrirCrearPerfil(){ //Abrimos la pantalla de crear un perfil
        Intent i = new Intent(this, CrearPerfilActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    public void registrar(View v){
        //Click en registrar. comprobamos los datos introducidos, y si son correctos registramos el usuario
        
        String email=inputemail.getEditText().getText().toString();
        String pass=inputpass.getEditText().getText().toString();
        String repPass=inputrepPass.getEditText().getText().toString();

        if (email.equals("")||!email.contains("@")) {//Para que el email sea valido tiene q ser !="" y contener un @. Si no es asi saltamos un error
            inputemail.setError("El email no es correcto");
            inputemail.requestFocus();
        }else if(pass.equals("")||pass.length()<6){ //La contraseña tiene que tener un minimo de 6 caracteres (Es una restriccion de las pass de firebase). Si no se cumple mostramos mensaje de error
            inputpass.setError("La contraseña tiene que tener un minimo de 6 caracteres");
            inputpass.requestFocus();
        }else if(repPass.equals("")||repPass.length()<6){ //La contraseña tiene que tener un minimo de 6 caracteres (Es una restriccion de las pass de firebase). Si no se cumple mostramos mensaje de error
            inputrepPass.setError("La contraseña tiene que tener un minimo de 6 caracteres");
            inputrepPass.requestFocus();
        }else if (!pass.equals(repPass)) { //Las contraseñas tienen que ser iguales
            inputrepPass.setError("Las contraseñas no coinciden");
            inputrepPass.requestFocus();
        }else{//To do bien, registramos el usuario

            //Mostramos un progrssDialog de registrando...
            progressDialog.setTitle("Registrando...");
            progressDialog.setMessage("Porfavor espera mientras se registra");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

                mAuth.createUserWithEmailAndPassword(email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            //Se ha registrado correctamente
                            progressDialog.dismiss(); //Cancelamos el dialogo de progreso
                            abrirCrearPerfil(); //Vamos a la ventana de crear el perfil
                        }else{
                            progressDialog.dismiss();//Cancelamos el dialogo de progreso
                            //Mostrar alerta de q no se ha podido registrar
                            Toast.makeText(CrearCuentaActivity.this,"No se ha podido registrar",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        }

    }
}
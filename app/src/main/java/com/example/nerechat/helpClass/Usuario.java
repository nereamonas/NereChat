package com.example.nerechat.helpClass;

import android.net.Uri;
import android.widget.ImageView;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;

public class Usuario {
    private String nombreUsuario;
    private String fotoPerfil;
    private String estado;
    private String uid;

    public Usuario() {
    }

    public Usuario(String nombreUsuario,  String fotoPerfil,String estado,String uid) {
        this.nombreUsuario = nombreUsuario;
        this.fotoPerfil = fotoPerfil;
        this.estado = estado;
        this.uid=uid;

    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getEstado() {
        return estado;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public String getUid(){ return this.uid;}

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public void setEstado(String info) {
        this.estado = info;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }

    public void setUid(String uid){ this.uid=uid;}
}

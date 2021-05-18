package com.example.nerechat.helpClass;

public class Usuario {
    private String nombreUsuario;
    private String fotoPerfil;
    private String conectado;
    private String estado;
    private String fechaCreacion;
    private String uid;

    public Usuario() {
    }

    public Usuario(String nombreUsuario,  String fotoPerfil,String conectado, String estado,String fechaCreacion, String uid) {
        this.nombreUsuario = nombreUsuario;
        this.fotoPerfil = fotoPerfil;
        this.conectado=conectado;
        this.estado = estado;
        this.uid=uid;
        this.fechaCreacion=fechaCreacion;

    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getEstado() {
        return estado;
    }

    public String getConectado(){ return conectado;}

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public String getUid(){ return this.uid;}

    public String getFechaCreacion(){ return this.fechaCreacion;}

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public void setEstado(String info) {
        this.estado = info;
    }

    public void setConectado(String conectado){ this.conectado=conectado;}

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }

    public void setUid(String uid){ this.uid=uid;}

    public void setFechaCreacion(String fechaCreacion){ this.fechaCreacion=fechaCreacion;}
}

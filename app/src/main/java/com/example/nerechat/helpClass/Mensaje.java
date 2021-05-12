package com.example.nerechat.helpClass;

public class Mensaje {

    private String mensaje;
    private String usuario;
    private String hora;
    private String like;
    public Mensaje(){}

    public Mensaje(String mensaje, String usuario, String hora, String like) {
        this.mensaje = mensaje;
        this.usuario = usuario;
        this.hora=hora;
        this.like=like;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getHora(){ return hora;}

    public String getLike(){ return like;}

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public void setHora(String hora){ this.hora=hora;}

    public void setLike(String like){ this.like=like;}
}

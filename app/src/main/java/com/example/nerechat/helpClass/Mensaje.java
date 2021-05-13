package com.example.nerechat.helpClass;

public class Mensaje {

    private String mensaje;
    private String usuario;
    private String hora;
    private String reaccion;
    public Mensaje(){}

    public Mensaje(String mensaje, String usuario, String hora, String reaccion) {
        this.mensaje = mensaje;
        this.usuario = usuario;
        this.hora=hora;
        this.reaccion=reaccion;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getHora(){ return hora;}

    public String getReaccion(){ return reaccion;}

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public void setHora(String hora){ this.hora=hora;}

    public void setReaccion(String reaccion){ this.reaccion=reaccion;}
}

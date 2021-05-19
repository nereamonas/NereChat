package com.example.nerechat.helpClass;

public class Comment {

    private String username;
    private String texto;
    private String hora;

    public Comment(){}
    public Comment(String username,  String texto, String hora) {
        this.username = username;
        this.texto = texto;
        this.hora = hora;
    }

    public String getUsername() {
        return username;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }
}

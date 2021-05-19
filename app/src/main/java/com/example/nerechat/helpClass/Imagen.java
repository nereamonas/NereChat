package com.example.nerechat.helpClass;

public class Imagen {
    private String descripcion;
    private String id;
    private String likes;
    private String uid;
    private String uri;
    //private String username;

    public Imagen(){}
    public Imagen(String descripcion, String id, String likes, String uid, String uri/*, String username*/) {
        this.descripcion = descripcion;
        this.id = id;
        this.uri = uri;
        this.uid = uid;
        this.likes =likes;
        //this.username = username;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLikes() {
        return likes;
    }

    public void setLikes(String likes) {
        this.likes = likes;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    /*public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }*/
}

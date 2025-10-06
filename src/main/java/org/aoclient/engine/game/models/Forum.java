package org.aoclient.engine.game.models;

public class Forum {
    private int alignment;
    private String title;
    private String author;
    private String message;
    private boolean sticky;

    public Forum(int alignment, String title, String author, String message, boolean sticky) {
        System.out.println("Creando mensaje - Título: " + title + ", Sticky: " + sticky + ", Alineación: " + alignment);
        this.alignment = alignment;
        this.title = title;
        this.author = author;
        this.message = message;
        this.sticky = sticky;
    }

    public int getAlignment() {
        return alignment;
    }

    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSticky() {
        return sticky;
    }

    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }
}

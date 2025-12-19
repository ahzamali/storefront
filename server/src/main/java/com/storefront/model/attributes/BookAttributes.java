package com.storefront.model.attributes;

public class BookAttributes implements ProductAttributes {
    private String author;
    private String publicationDate;
    private String description;
    private String isbn;
    private String genre;
    private String publisher;

    public BookAttributes() {
    }

    public BookAttributes(String author, String publicationDate, String description, String isbn, String genre,
            String publisher) {
        this.author = author;
        this.publicationDate = publicationDate;
        this.description = description;
        this.isbn = isbn;
        this.genre = genre;
        this.publisher = publisher;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }
}

package com.mcq.problems;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"id", "title"})
class Book {
  private final int id;
  String title;

  Book(int id, String title) {
    this.id = id;
    this.title = title;
  }

@JsonGetter
  public int getId() {
      return this.id;
  }
  
  @JsonGetter
  public String getTitle() {
      return this.title;
  }
}

class BookManager {
  private List<Book> books;

  BookManager() {
    this.books = new ArrayList<>();
  }

  boolean createBook(int id, String title) {
    // TODO: return false if the book id already exists
    
    if(findBookById(id) != null) {
      return false;
    } else {
      Book book = new Book(id, title);
      books.add(book);
      return true;
    }
  
  }

  boolean updateBook(int id, String title) {
    // TODO: return false if the book id does not exist
    
    Book book = findBookById(id);
    if (book != null) {
      book.title = title;
      return true;
    } else {
      return false;
    }
    
  }

  boolean deleteBook(int id) {
    // TODO: return false if the book does not exist

    Book book = findBookById(id);
    
    if(book!=null){
      return books.remove(book);
    }
    
    return false;
  }

  Book findBookById(int id) {
    // book or null
    
    for(Book book : books) {
      System.out.println(book.getId());
      if(book.getId() == id)
      {
        return book;
      }
    }
    
    // for (int i = 0; i < books.size(); i++) {
    //   Book curBook = books.get(i);
    //   if (curBook.getId() == id) {
    //     return curBook;
    //   }
    // }
    return null;
  }

  Book findBookByTitle(String title) {
    // book or null
    
    for(Book book : books) {
      if(book.getTitle().equals(title))
      {
        return book;
      }
    }
    
    // for (int i = 0; i < books.size(); i++) {
    //   Book curBook = books.get(i);
    //   if (curBook.getTitle().equals(title)) {
    //     return curBook;
    //   }
    // }
    return null;
  }
}

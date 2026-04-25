package com.example.stockServer;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String productId;

    private String name;
    private int quantity;
    private String category;

    public Product() {}

    public Product(String productId, String name, int quantity, String category) {
        this.productId = productId;
        this.name      = name;
        this.quantity  = quantity;
        this.category  = category;
    }

    public Long getId()                   { return id; }
    public String getProductId()          { return productId; }
    public void setProductId(String pid)  { this.productId = pid; }
    public String getName()               { return name; }
    public void setName(String name)      { this.name = name; }
    public int getQuantity()              { return quantity; }
    public void setQuantity(int q)        { this.quantity = q; }
    public String getCategory()           { return category; }
    public void setCategory(String c)     { this.category = c; }

    @Override
    public String toString() {
        return "Product{id='" + productId + "', name='" + name + "', qty=" + quantity + "}";
    }
}

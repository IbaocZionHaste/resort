package com.example.resort.accommodation.data;

import java.io.Serializable;

public class Accommodation implements Serializable {
    private String id;
    private String category;
    private String description;
    private String imageUrl;
    private String name;
    private String price;
    private String specification;
    private String status;
    private String location;
    private String amenities;


    // Category-specific fields
    private String capacity; // Boats, Cottages
    private String design; // Boats, Cottages
    private String food1, food2, food3, food4, food5, pieceNameFood; // Food, Package
    private String beverageFlavor, beverageOccasions, beverageServing, beverageSize; // Beverages
    private String flavorToppings, perfectFor, pieceNameDessert; // Desserts
    private String alcoholContent, alcoholType, alcoholSize; // Alcohol
    private String capacityCottage; // Packages

    // New fields added
    private String cottage;   // Additional cottage field
    private String food6;     // Additional food6 field
    private String beverage;  // Additional beverage field

    // New field for available date (e.g., when the product becomes available again)
    private String availableDate;

    // No-argument constructor (required for Firebase)
    public Accommodation() {
    }

    // Getters and Setters for common fields
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }

    // Category-Specific Getters and Setters
    public String getCapacity() { return capacity; }
    public void setCapacity(String capacity) { this.capacity = capacity; }

    public String getDesign() { return design; }
    public void setDesign(String design) { this.design = design; }

    public String getFood1() { return food1; }
    public void setFood1(String food1) { this.food1 = food1; }

    public String getFood2() { return food2; }
    public void setFood2(String food2) { this.food2 = food2; }

    public String getFood3() { return food3; }
    public void setFood3(String food3) { this.food3 = food3; }

    public String getFood4() { return food4; }
    public void setFood4(String food4) { this.food4 = food4; }

    public String getFood5() { return food5; }
    public void setFood5(String food5) { this.food5 = food5; }

    public String getPieceNameFood() {
        return pieceNameFood;
    }
    public void setPieceNameFood(String pieceNameFood) {
        this.pieceNameFood = pieceNameFood;
    }

    public String getBeverageFlavor() { return beverageFlavor; }
    public void setBeverageFlavor(String beverageFlavor) { this.beverageFlavor = beverageFlavor; }

    public String getBeverageOccasions() { return beverageOccasions; }
    public void setBeverageOccasions(String beverageOccasions) { this.beverageOccasions = beverageOccasions; }

    public String getBeverageServing() { return beverageServing; }
    public void setBeverageServing(String beverageServing) { this.beverageServing = beverageServing; }

    public String getBeverageSize() {
        return beverageSize;
    }
    public void setBeverageSize(String beverageSize) {
        this.beverageSize = beverageSize;
    }

    public String getFlavorToppings() {
        return flavorToppings;
    }
    public void setFlavorToppings(String flavorToppings) {
        this.flavorToppings = flavorToppings;
    }

    public String getPerfectFor() { return perfectFor; }
    public void setPerfectFor(String perfectFor) { this.perfectFor = perfectFor; }

    public String getPieceNameDessert() {
        return pieceNameDessert;
    }
    public void setPieceNameDessert(String pieceNameDessert) {
        this.pieceNameDessert = pieceNameDessert;
    }

    public String getAlcoholContent() { return alcoholContent; }
    public void setAlcoholContent(String alcoholContent) { this.alcoholContent = alcoholContent; }

    public String getAlcoholType() { return alcoholType; }
    public void setAlcoholType(String alcoholType) { this.alcoholType = alcoholType; }

    public String getAlcoholSize() {
        return alcoholSize;
    }
    public void setAlcoholSize(String alcoholSize) {
        this.alcoholSize = alcoholSize;
    }

    public String getCapacityCottage() { return capacityCottage; }
    public void setCapacityCottage(String capacityCottage) { this.capacityCottage = capacityCottage; }

    public String getCottage() {
        return cottage;
    }
    public void setCottage(String cottage) {
        this.cottage = cottage;
    }

    public String getFood6() {
        return food6;
    }
    public void setFood6(String food6) {
        this.food6 = food6;
    }

    public String getBeverage() {
        return beverage;
    }
    public void setBeverage(String beverage) {
        this.beverage = beverage;
    }

    public String getAvailableDate() {
        return availableDate;
    }
    public void setAvailableDate(String availableDate) {
        this.availableDate = availableDate;
    }
}





///package com.example.resort.accommodation.data;
//
//import java.io.Serializable;
//
//public class Accommodation implements Serializable {
//    private String id;
//    private String category;
//    private String description;
//    private String imageUrl;
//    private String name;
//    private String price;
//    private String specification;
//    private String status;
//    private String location;
//
//    private String amenities;
//
//    // Additional fields for specific categories
//    private String capacity; // Boats, Cottages
//    private String design; // Boats, Cottages
//    private String food1, food2, food3, food4, food5, pieceNameFood; // Food, Package
//    private String beverageFlavor, beverageOccasions, beverageServing, beverageSize; // Beverages
//    private String flavorToppings, perfectFor, pieceNameDessert; // Desserts (renamed flavorToppigs to flavorToppings)
//    private String alcoholContent, alcoholType, alcoholSize; // Alcohol
//    private String capacityCottage; // Packages
//
//    // New fields added
//    private String cottage;   // Additional cottage field
//    private String food6;     // Additional food6 field
//    private String beverage;  // Additional beverage field
//
//    // No-argument constructor (required for Firebase)
//    public Accommodation() {
//    }
//
//    // Getters and Setters for common fields
//    public String getId() { return id; }
//    public void setId(String id) { this.id = id; }
//
//    public String getCategory() { return category; }
//    public void setCategory(String category) { this.category = category; }
//
//    public String getDescription() { return description; }
//    public void setDescription(String description) { this.description = description; }
//
//    public String getImageUrl() { return imageUrl; }
//    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
//
//    public String getName() { return name; }
//    public void setName(String name) { this.name = name; }
//
//    public String getPrice() { return price; }
//    public void setPrice(String price) { this.price = price; }
//
//    public String getSpecification() { return specification; }
//    public void setSpecification(String specification) { this.specification = specification; }
//
//    public String getStatus() { return status; }
//    public void setStatus(String status) { this.status = status; }
//
//    public String getLocation() { return location; }
//    public void setLocation(String location) { this.location = location; }
//
//    public String getAmenities() { return amenities; }
//    public void setAmenities(String amenities) { this.amenities = amenities; }
//
//    // Category-Specific Getters and Setters
//    public String getCapacity() { return capacity; }
//    public void setCapacity(String capacity) { this.capacity = capacity; }
//
//    public String getDesign() { return design; }
//    public void setDesign(String design) { this.design = design; }
//
//    public String getFood1() { return food1; }
//    public void setFood1(String food1) { this.food1 = food1; }
//
//    public String getFood2() { return food2; }
//    public void setFood2(String food2) { this.food2 = food2; }
//
//    public String getFood3() { return food3; }
//    public void setFood3(String food3) { this.food3 = food3; }
//
//    public String getFood4() { return food4; }
//    public void setFood4(String food4) { this.food4 = food4; }
//
//    public String getFood5() { return food5; }
//    public void setFood5(String food5) { this.food5 = food5; }
//
//    // New Getter and Setter for pieceNameFood
//    public String getPieceNameFood() {
//        return pieceNameFood;
//    }
//    public void setPieceNameFood(String pieceNameFood) {
//        this.pieceNameFood = pieceNameFood;
//    }
//
//    public String getBeverageFlavor() { return beverageFlavor; }
//    public void setBeverageFlavor(String beverageFlavor) { this.beverageFlavor = beverageFlavor; }
//
//    public String getBeverageOccasions() { return beverageOccasions; }
//    public void setBeverageOccasions(String beverageOccasions) { this.beverageOccasions = beverageOccasions; }
//
//    public String getBeverageServing() { return beverageServing; }
//    public void setBeverageServing(String beverageServing) { this.beverageServing = beverageServing; }
//
//    // New Getter and Setter for beverageSize
//    public String getBeverageSize() {
//        return beverageSize;
//    }
//    public void setBeverageSize(String beverageSize) {
//        this.beverageSize = beverageSize;
//    }
//
//    // Renamed for consistency: flavorToppings
//    public String getFlavorToppings() {
//        return flavorToppings;
//    }
//    public void setFlavorToppings(String flavorToppings) {
//        this.flavorToppings = flavorToppings;
//    }
//
//    public String getPerfectFor() { return perfectFor; }
//    public void setPerfectFor(String perfectFor) { this.perfectFor = perfectFor; }
//
//    // New Getter and Setter for pieceNameDessert
//    public String getPieceNameDessert() {
//        return pieceNameDessert;
//    }
//    public void setPieceNameDessert(String pieceNameDessert) {
//        this.pieceNameDessert = pieceNameDessert;
//    }
//
//    public String getAlcoholContent() { return alcoholContent; }
//    public void setAlcoholContent(String alcoholContent) { this.alcoholContent = alcoholContent; }
//
//    public String getAlcoholType() { return alcoholType; }
//    public void setAlcoholType(String alcoholType) { this.alcoholType = alcoholType; }
//
//    // New Getter and Setter for alcoholSize
//    public String getAlcoholSize() {
//        return alcoholSize;
//    }
//    public void setAlcoholSize(String alcoholSize) {
//        this.alcoholSize = alcoholSize;
//    }
//
//    public String getCapacityCottage() { return capacityCottage; }
//    public void setCapacityCottage(String capacityCottage) { this.capacityCottage = capacityCottage; }
//
//    // New Getters and Setters for added fields
//
//    public String getCottage() {
//        return cottage;
//    }
//    public void setCottage(String cottage) {
//        this.cottage = cottage;
//    }
//
//    public String getFood6() {
//        return food6;
//    }
//    public void setFood6(String food6) {
//        this.food6 = food6;
//    }
//
//    public String getBeverage() {
//        return beverage;
//    }
//    public void setBeverage(String beverage) {
//        this.beverage = beverage;
//    }
//}


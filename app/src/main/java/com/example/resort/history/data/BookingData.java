package com.example.resort.history.data;

import java.io.Serializable;
import java.util.Map;

public class BookingData implements Serializable {
    private String id;
    private BookingReview bookingReview;
    private PaymentMethod paymentMethod;
    private PaymentTransaction paymentTransaction;

    public BookingData() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public BookingReview getBookingReview() { return bookingReview; }
    public void setBookingReview(BookingReview bookingReview) { this.bookingReview = bookingReview; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public PaymentTransaction getPaymentTransaction() { return paymentTransaction; }
    public void setPaymentTransaction(PaymentTransaction paymentTransaction) { this.paymentTransaction = paymentTransaction; }

    public static class BookingReview implements Serializable {
        public String bookingDate, email, name, phone, refNo, statusReview;
        public Map<String, Object> orderItems;
        public BookingReview() {}
    }

    public static class PaymentMethod implements Serializable {
        public String Amount, Date, Firstname, Lastname, Payment, Phone, Reference, Status;
        public PaymentMethod() {}
    }

    public static class PaymentTransaction implements Serializable {
        public String PaymentDate, finalStatus, name, paymentStatus, refNo;
        public long amount, downPayment;
        public PaymentTransaction() {}
    }
}

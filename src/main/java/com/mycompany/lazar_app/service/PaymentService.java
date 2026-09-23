package com.mycompany.lazar_app.service;

import com.mycompany.lazar_app.model.Payment;
import com.mycompany.lazar_app.repository.PaymentRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        if (paymentRepository == null) {
            throw new IllegalArgumentException(
                    "PaymentRepository is required."
            );
        }

        this.paymentRepository = paymentRepository;
    }

    // =========================================================
    // CREATE
    // =========================================================

    public Payment addPayment(Payment payment) {

        validate(payment);

        try {
            return paymentRepository.save(payment);

        } catch (SQLException e) {
            throw databaseException(
                    "Unable to save payment.",
                    e
            );
        }
    }

    // =========================================================
    // UPDATE
    // =========================================================

    public void updatePayment(Payment payment) {

        validate(payment);

        if (payment.getId() <= 0) {
            throw new IllegalArgumentException(
                    "Invalid payment ID."
            );
        }

        try {
            paymentRepository.update(payment);

        } catch (SQLException e) {
            throw databaseException(
                    "Unable to update payment.",
                    e
            );
        }
    }

    // =========================================================
    // DELETE
    // =========================================================

    public void deletePayment(long id) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Invalid payment ID."
            );
        }

        try {
            paymentRepository.delete(id);

        } catch (SQLException e) {
            throw databaseException(
                    "Unable to delete payment.",
                    e
            );
        }
    }

    // =========================================================
    // FIND ALL
    // =========================================================

    public List<Payment> getAllPayments() {

        try {
            return paymentRepository.findAll();

        } catch (SQLException e) {
            throw databaseException(
                    "Unable to load payments.",
                    e
            );
        }
    }

    // =========================================================
    // FIND BY MEMBER
    // =========================================================

    public List<Payment> getPaymentsByMember(long memberId) {

        if (memberId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid member ID."
            );
        }

        try {
            return paymentRepository.findByMemberId(memberId);

        } catch (SQLException e) {
            throw databaseException(
                    "Unable to load member payments.",
                    e
            );
        }
    }

    // =========================================================
    // SEARCH
    // =========================================================

    public List<Payment> search(
            String keyword,
            String paymentMethod,
            String status) {

        try {
            return paymentRepository.search(
                    keyword,
                    paymentMethod,
                    status
            );

        } catch (SQLException e) {
            throw databaseException(
                    "Unable to search payments.",
                    e
            );
        }
    }

    // =========================================================
    // TOTAL PAYMENTS
    // =========================================================

    public BigDecimal getTotalPayments() {

        try {
            return paymentRepository.getTotalPayments();

        } catch (SQLException e) {
            throw databaseException(
                    "Unable to calculate total payments.",
                    e
            );
        }
    }

    // =========================================================
    // CURRENT MONTH
    // =========================================================

    public BigDecimal getCurrentMonthPayments() {

        LocalDate today = LocalDate.now();

        LocalDate start =
                today.withDayOfMonth(1);

        LocalDate end =
                today.withDayOfMonth(
                        today.lengthOfMonth()
                );

        try {
            return paymentRepository
                    .getTotalPaymentsBetween(
                            start,
                            end
                    );

        } catch (SQLException e) {
            throw databaseException(
                    "Unable to calculate current month payments.",
                    e
            );
        }
    }

    // =========================================================
    // PAID TOTAL
    // =========================================================

    public BigDecimal getPaidTotal() {

        try {
            return paymentRepository
                    .getTotalByStatus("PAID");

        } catch (SQLException e) {
            throw databaseException(
                    "Unable to calculate paid payments.",
                    e
            );
        }
    }

    // =========================================================
    // PENDING TOTAL
    // =========================================================

    public BigDecimal getPendingTotal() {

        try {
            return paymentRepository
                    .getTotalByStatus("PENDING");

        } catch (SQLException e) {
            throw databaseException(
                    "Unable to calculate pending payments.",
                    e
            );
        }
    }

    // =========================================================
    // TRANSACTION COUNT
    // =========================================================

    public long getTransactionCount() {

        try {
            return paymentRepository.countAll();

        } catch (SQLException e) {
            throw databaseException(
                    "Unable to count payment transactions.",
                    e
            );
        }
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private void validate(Payment payment) {

        if (payment == null) {
            throw new IllegalArgumentException(
                    "Payment is required."
            );
        }

        if (payment.getMemberId() <= 0) {
            throw new IllegalArgumentException(
                    "Member is required."
            );
        }

        if (payment.getAmount() == null
                || payment.getAmount()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero."
            );
        }

        if (payment.getPaymentMethod() == null
                || payment.getPaymentMethod().isBlank()) {

            throw new IllegalArgumentException(
                    "Payment method is required."
            );
        }

        if (payment.getStatus() == null
                || payment.getStatus().isBlank()) {

            throw new IllegalArgumentException(
                    "Payment status is required."
            );
        }

        if (payment.getPaymentDate() == null) {
            throw new IllegalArgumentException(
                    "Payment date is required."
            );
        }
    }

    // =========================================================
    // DATABASE ERROR
    // =========================================================

    private RuntimeException databaseException(
            String message,
            SQLException exception) {

        return new RuntimeException(
                message + " " + exception.getMessage(),
                exception
        );
    }
}
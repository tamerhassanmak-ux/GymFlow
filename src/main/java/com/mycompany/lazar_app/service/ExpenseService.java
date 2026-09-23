package com.mycompany.lazar_app.service;

import com.mycompany.lazar_app.model.Expense;
import com.mycompany.lazar_app.repository.ExpenseRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ExpenseService {

    private final ExpenseRepository repository;

    public ExpenseService(
            ExpenseRepository repository) {

        this.repository = repository;
    }

    public Expense addExpense(Expense expense) {

        validate(expense);

        return repository.save(expense);
    }

    public void updateExpense(Expense expense) {

        validate(expense);

        repository.update(expense);
    }

    public void deleteExpense(long id) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Invalid expense ID."
            );
        }

        repository.delete(id);
    }

    public List<Expense> getAllExpenses() {
        return repository.findAll();
    }

    public List<Expense> search(
            String keyword,
            String category,
            String status) {

        return repository.search(
                keyword,
                category,
                status
        );
    }

    public BigDecimal getTotalExpenses() {
        return repository.getTotalExpenses();
    }

    public BigDecimal getCurrentMonthExpenses() {

        LocalDate today = LocalDate.now();

        LocalDate start =
                today.withDayOfMonth(1);

        LocalDate end =
                today.withDayOfMonth(
                        today.lengthOfMonth()
                );

        return repository.getTotalExpensesBetween(
                start,
                end
        );
    }

    public BigDecimal getPendingAmount() {
        return repository.getPendingAmount();
    }

    public long getTransactionCount() {
        return repository.countAll();
    }

    private void validate(Expense expense) {

        if (expense == null) {
            throw new IllegalArgumentException(
                    "Expense is required."
            );
        }

        if (expense.getTitle() == null
                || expense.getTitle().isBlank()) {

            throw new IllegalArgumentException(
                    "Expense title is required."
            );
        }

        if (expense.getAmount() == null
                || expense.getAmount()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Expense amount must be greater than zero."
            );
        }

        if (expense.getExpenseDate() == null) {
            throw new IllegalArgumentException(
                    "Expense date is required."
            );
        }
    }
}
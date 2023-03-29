package com.example.budgetapp.services.impl;

import com.example.budgetapp.model.Transaction;
import com.example.budgetapp.services.BudgetService;
import com.example.budgetapp.services.FilesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;

@Service
public class BudgetServiceImpl implements BudgetService {

    final private FilesService filesService;

    public static final int SALARY = 30_000 - 9_750;

    public static final int SAVING = 3_000;

    public static final int DAILY_BUDGET = (SALARY - SAVING) / LocalDate.now().lengthOfMonth();

    public static int balance = 0;

    public static final int AVG_SALARY = SALARY;

    public static final double AVR_DAYS = 29.3;

    private TreeMap<Month, LinkedHashMap<Long, Transaction>> transactions = new TreeMap<>();

    private static long lastId = 0;

    public BudgetServiceImpl(FilesService filesService) {
        this.filesService = filesService;
    }

    @PostConstruct
    private void init() {
        readFromFile();
    }

    @Override
    public int getDailyBudget() {
        return DAILY_BUDGET;
    }

    @Override
    public int getBalance() {
        return SALARY - SAVING - getAllSpend();
    }

    @Override
    public long addTransaction(Transaction transaction) {
        LinkedHashMap<Long, Transaction> monthTransaction = transactions.getOrDefault(LocalDate.now().getMonth(), new LinkedHashMap<>());
        monthTransaction.put(lastId, transaction);
        transactions.put(LocalDate.now().getMonth(), monthTransaction);
        saveToFile();
        return lastId++;
    }

    @Override
    public Transaction getTransaction(long id) {
        for (Map<Long, Transaction> transactionsByMonth : transactions.values()) {
            Transaction transaction = transactionsByMonth.get(id);
            if (transaction != null) {
                saveToFile();
                return transaction;
            }
        }
        return null;
    }

    @Override
    public Transaction editTransaction(long id, Transaction transaction) {
        for (Map<Long, Transaction> transactionsByMonth : transactions.values()) {
            if (transactionsByMonth.containsKey(id)) {
                transactionsByMonth.put(id, transaction);
                saveToFile();
                return transaction;
            }
        }
        return null;
    }

    @Override
    public boolean deleteTransaction(long id) {
        for (Map<Long, Transaction> transactionsByMonth : transactions.values()) {
            if (transactionsByMonth.containsKey(id)) {
                transactionsByMonth.remove(id);
                return true;
            }
        }
        return false;
    }

    @Override
    public void deleteAllTransaction() {
        transactions = new TreeMap<>();
    }

    @Override
    public int getDailyBalance() {
        return DAILY_BUDGET * LocalDate.now().getDayOfMonth() - getAllSpend();
    }

    @Override
    public int getAllSpend() {
        Map<Long, Transaction> monthTransaction = transactions.getOrDefault(LocalDate.now().getMonth(), new LinkedHashMap<>());
        int sum = 0;
        for (Transaction transaction : monthTransaction.values()) {
            sum += transaction.getSum();
        }
        return sum;
    }

    @Override
    public int getVacationBonus(int daysCount) {
        double avrDaySalary = AVG_SALARY / AVR_DAYS;
        return (int) (daysCount * avrDaySalary);
    }

    @Override
    public int getSalaryWithVacation(int vacationDaysCount, int vacationWorkingDaysCount, int workingDaysInMonth) {
        int salary = SALARY / workingDaysInMonth * (workingDaysInMonth - vacationWorkingDaysCount);
        return salary + getVacationBonus(vacationDaysCount);
    }

    @Override
    public Path createMonthlyReport(Month month) throws IOException {
        LinkedHashMap<Long, Transaction> monthlyTransactions = transactions.getOrDefault(month, new LinkedHashMap<>());
        Path path = filesService.createTempFile("monthlyReport");
        for (Transaction transaction : monthlyTransactions.values()) {
            try (Writer writer = Files.newBufferedWriter(path, StandardOpenOption.APPEND)){
                writer.append(transaction.getCategory().getText() + ":" + transaction.getSum() + " руб. - " + transaction.getComment());
                writer.append("\n");
            }
        }
        return path;
    }

    private void saveToFile() {
        try {
            String json = new ObjectMapper().writeValueAsString(transactions);
            filesService.saveToFile(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void readFromFile() {
        String json = filesService.readFromFile();
        try {
            transactions = new ObjectMapper().readValue(json, new TypeReference<TreeMap<Month, LinkedHashMap<Long, Transaction>>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

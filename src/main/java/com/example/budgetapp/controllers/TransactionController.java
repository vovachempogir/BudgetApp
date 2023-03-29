package com.example.budgetapp.controllers;

import com.example.budgetapp.model.Category;
import com.example.budgetapp.model.Transaction;
import com.example.budgetapp.services.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Month;

@RestController
@RequestMapping("/transaction")
@Tag(name = "Транзакции", description = "CRUD-операции и другие эндпоинты для работы с транзакциями")
public class TransactionController {

    private final BudgetService budgetService;

    public TransactionController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<Long> addTransaction(@RequestBody Transaction transaction) {
        long id = budgetService.addTransaction(transaction);
        return ResponseEntity.ok(id);
    }

    @GetMapping
    @Operation(
            summary = "Поиск транзакции по месяцу и/или категории",
            description = "Можно искать по одному параметру, обоим или вообще без параметров"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Транзакции найдены",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = Transaction.class)
                                    )
                            )
                    }
            )
    })
    public ResponseEntity<Transaction> getAllTransaction(@RequestParam(required = false) Month month,
                                                         @RequestParam(required = false) Category category) {
        return null;
    }

    @GetMapping("/byMonth/{month}")
    public ResponseEntity<Object> getTransactionByMonth(@PathVariable Month month) {
        try {
            Path path = budgetService.createMonthlyReport(month);
            if (Files.size(path) == 0) {
                return ResponseEntity.noContent().build();
            }
            InputStreamResource resource = new InputStreamResource(new FileInputStream(path.toFile()));
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .contentLength(Files.size(path))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+month + " -report.txt\"")
                    .body(resource);

        } catch (
                IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.toString());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable long id) {
        Transaction transaction = budgetService.getTransaction(id);
        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(transaction);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transaction> editTransaction(@PathVariable long id, @RequestBody Transaction transaction) {
        Transaction transaction1 = budgetService.editTransaction(id, transaction);
        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(transaction1);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable long id) {
        if (budgetService.deleteTransaction(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }


}

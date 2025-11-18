package com.rybka.ticketing.controller;

import com.rybka.ticketing.model.dto.admin.AdminOrderDetailsDto;
import com.rybka.ticketing.model.dto.admin.AdminOrderListItemDto;
import com.rybka.ticketing.model.dto.admin.EventRevenueReportDto;
import com.rybka.ticketing.model.dto.admin.EventUtilizationReportDto;
import com.rybka.ticketing.model.dto.userOrder.RefundRequestDto;
import com.rybka.ticketing.model.dto.userOrder.RefundResponseDto;
import com.rybka.ticketing.model.enums.OrderStatus;
import com.rybka.ticketing.service.AdminOrderQueryService;
import com.rybka.ticketing.service.ExportService;
import com.rybka.ticketing.service.RefundService;
import com.rybka.ticketing.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOrderController {
    @Autowired
    private AdminOrderQueryService queryService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private ExportService exportService;

    @GetMapping("/orders")
    public Page<AdminOrderListItemDto> list(
            @RequestParam(required = false) List<OrderStatus> status,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort s = Sort.by(
                Arrays.stream(sort.split(","))
                        .map(String::trim).toList().size()==2
                        ? Sort.Order.by(sort.split(",")[0]).with("desc".equalsIgnoreCase(sort.split(",")[1])?Sort.Direction.DESC:Sort.Direction.ASC)
                        : Sort.Order.desc("createdAt")
        );
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), s);
        Set<OrderStatus> statuses = status == null ? Set.of() : new HashSet<>(status);
        return queryService.list(statuses, userEmail, eventId, from, to, pageable);
    }

    @GetMapping("/orders/{id}")
    public AdminOrderDetailsDto details(@PathVariable Long id){
        return queryService.getDetails(id);
    }

    @PostMapping("/orders/{id}/refund")
    public ResponseEntity<RefundResponseDto> refund(
            @PathVariable Long id,
            @RequestBody RefundRequestDto req,
            @RequestHeader(value="Idempotency-Key", required=false) String idemp,
            @RequestAttribute(name="userId", required=false) Long adminId
    ) {
        RefundResponseDto dto = refundService.refundOrder(adminId, id, req.getReason(),
                (req.getIdempotencyKey()!=null)?req.getIdempotencyKey():idemp);
        HttpStatus code = dto.isApplied() ? HttpStatus.OK : HttpStatus.OK;
        return new ResponseEntity<>(dto, code);
    }

    @GetMapping("/reports/events/{eventId}/revenue")
    public EventRevenueReportDto revenue(@PathVariable Long eventId){
        return reportService.getEventRevenue(eventId);
    }

    @GetMapping("/reports/events/{eventId}/utilization")
    public EventUtilizationReportDto util(@PathVariable Long eventId){
        return reportService.getEventUtilization(eventId);
    }

    @GetMapping(value="/exports/orders.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam List<Long> orderId
    ){
        byte[] csv = exportService.exportOrdersCsv(orderId);
        HttpHeaders h = new HttpHeaders();
        h.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders-export.csv");
        return new ResponseEntity<>(csv, h, HttpStatus.OK);
    }

}

package com.rybka.ticketing.service;

import com.rybka.ticketing.model.entity.OrderItem;
import com.rybka.ticketing.repository.OrderItemRepository;
import com.rybka.ticketing.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    public byte[] exportOrdersCsv(List<Long> orderIds){
        String header = String.join(",",
                "orderId","createdAt","userEmail","status",
                "eventTitle","seatRow","seatNumber","unitPrice","lineTotal","currency");

        DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;
        String rows = orderIds.stream().map(id -> {
            var o = orderRepository.findById(id).orElse(null);
            if (o==null) return null;
            String email = null;
            String eventTitle = o.getEvent().getTitle();
            List<OrderItem> items = orderItemRepository.findByOrder_Id(o.getId());
            return items.stream().map(i -> String.join(",",
                    safe(o.getId()), safe(fmt.format(o.getCreatedAt())), safe(email),
                    safe(o.getStatus().name()), safe(eventTitle),
                    safe(i.getRow()), safe(i.getNumber()),
                    safe(i.getUnitPrice()), safe(i.getLineTotal()), safe(o.getCurrency())
            )).collect(Collectors.joining("\n"));
        }).filter(Objects::nonNull).collect(Collectors.joining("\n"));

        String csv = header + "\n" + rows + "\n";
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    private String safe(Object o){
        if (o == null) return "";
        String s = String.valueOf(o);
        if (s.contains(",") || s.contains("\"")) {
            s = "\"" + s.replace("\"","\"\"") + "\"";
        }
        return s;
    }
}

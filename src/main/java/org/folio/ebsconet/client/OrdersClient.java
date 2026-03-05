package org.folio.ebsconet.client;

import org.folio.ebsconet.domain.dto.PoLine;
import org.folio.ebsconet.domain.dto.PoLineCollection;
import org.folio.ebsconet.domain.dto.PurchaseOrder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange("orders")
public interface OrdersClient {
  @GetExchange(value = "/composite-orders/{id}")
  PurchaseOrder getOrderById(@PathVariable("id") String id);

  @GetExchange(value = "/order-lines/{id}")
  PoLine getOrderLineById(@PathVariable("id") String id);

  @GetExchange(value = "/order-lines")
  PoLineCollection getOrderLinesByQuery(@RequestParam("query") String query);

  @PutExchange(value = "/order-lines/{id}")
  void putOrderLine(@PathVariable("id") String id, PoLine poLine);
}

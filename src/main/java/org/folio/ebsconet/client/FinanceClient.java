package org.folio.ebsconet.client;

import org.folio.ebsconet.domain.dto.ExpenseClass;
import org.folio.ebsconet.domain.dto.ExpenseClassCollection;
import org.folio.ebsconet.domain.dto.FundCollection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("finance")
public interface FinanceClient {
  @GetExchange(value = "/funds")
  FundCollection getFundsByQuery(@RequestParam("query") String query);

  @GetExchange(value = "/expense-classes")
  ExpenseClassCollection getExpenseClassesByQuery(@RequestParam("query") String query);

  @GetExchange(value = "/expense-classes/{id}")
  ExpenseClass getExpenseClassesById(@PathVariable("id") String id);
}

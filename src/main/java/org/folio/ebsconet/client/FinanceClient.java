package org.folio.ebsconet.client;

import org.folio.ebsconet.domain.dto.ExpenseClass;
import org.folio.ebsconet.domain.dto.ExpenseClassCollection;
import org.folio.ebsconet.domain.dto.FundCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("finance")
public interface FinanceClient {
  @GetMapping(value = "/funds")
  FundCollection getFundsByQuery(@RequestParam("query") String query);

  @GetMapping(value = "/expense-classes")
  ExpenseClassCollection getExpenseClassesByQuery(@RequestParam("query") String query);

  @GetMapping(value = "/expense-classes/{id}")
  ExpenseClass getExpenseClassesById(@PathVariable("id") String id);
}

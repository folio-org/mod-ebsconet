package org.folio.ebsconet.client;

import org.folio.ebsconet.domain.dto.Organization;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("organizations")
public interface OrganizationClient {
  @GetExchange(value = "/organizations/{id}")
  Organization getOrganizationById(@PathVariable("id") String id);
}

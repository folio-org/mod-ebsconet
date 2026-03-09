package org.folio.ebsconet.config;

import org.folio.ebsconet.client.FinanceClient;
import org.folio.ebsconet.client.NoteLinksClient;
import org.folio.ebsconet.client.NoteTypeClient;
import org.folio.ebsconet.client.NotesClient;
import org.folio.ebsconet.client.OrdersClient;
import org.folio.ebsconet.client.OrganizationClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import lombok.extern.log4j.Log4j2;

@Configuration
@Log4j2
public class HttpClientConfiguration {

  @Bean
  public FinanceClient financeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(FinanceClient.class);
  }

  @Bean
  public NoteLinksClient noteLinksClient(HttpServiceProxyFactory factory) {
    return factory.createClient(NoteLinksClient.class);
  }

  @Bean
  public NotesClient notesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(NotesClient.class);
  }

  @Bean
  public NoteTypeClient noteTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(NoteTypeClient.class);
  }

  @Bean
  public OrdersClient ordersClient(HttpServiceProxyFactory factory) {
    return factory.createClient(OrdersClient.class);
  }

  @Bean
  public OrganizationClient organizationClient(HttpServiceProxyFactory factory) {
    return factory.createClient(OrganizationClient.class);
  }

}

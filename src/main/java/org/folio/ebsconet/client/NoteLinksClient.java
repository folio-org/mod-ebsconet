package org.folio.ebsconet.client;

import org.folio.ebsconet.domain.dto.NoteCollection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("note-links")
public interface NoteLinksClient {
  @GetExchange(value = "/domain/orders/type/poLine/id/{id}?limit=100000&status=assigned")
  NoteCollection getNotesByPoLineId(@PathVariable("id") String id);

}

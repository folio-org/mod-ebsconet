package org.folio.ebsconet.client;

import org.folio.ebsconet.domain.dto.NoteCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("note-links")
public interface NoteLinksClient {
  @GetMapping(value = "/domain/orders/type/poLine/id/{id}?limit=100000&status=assigned")
  NoteCollection getNotesByPoLineId(@PathVariable("id") String id);

}

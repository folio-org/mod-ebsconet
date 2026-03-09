package org.folio.ebsconet.client;

import tools.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("note-types")
public interface NoteTypeClient {
  @GetExchange(accept = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getNoteTypesByQuery(@RequestParam("query") String query);

}

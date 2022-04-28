package org.folio.ebsconet.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("note-types")
public interface NoteTypeClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getNoteTypesByQuery(@RequestParam("query") String query);

}

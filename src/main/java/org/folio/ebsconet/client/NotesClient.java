package org.folio.ebsconet.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.folio.ebsconet.domain.dto.Note;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("notes")
public interface NotesClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getNoteByQuery(@RequestParam("query") String query);

  @PostMapping
  Note postNote(@RequestBody Note note);

  @PutMapping(value = "/{id}")
  void putNote(@PathVariable("id") String id, JsonNode note);
}

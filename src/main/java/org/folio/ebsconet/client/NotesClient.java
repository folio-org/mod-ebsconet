package org.folio.ebsconet.client;

import org.folio.ebsconet.domain.dto.Note;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("notes")
public interface NotesClient {

  @PostExchange
  Note postNote(@RequestBody Note note);

  @DeleteExchange("/{id}")
  Note deleteNote(@PathVariable String id);
}

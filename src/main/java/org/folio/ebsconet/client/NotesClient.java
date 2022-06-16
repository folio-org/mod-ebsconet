package org.folio.ebsconet.client;

import org.folio.ebsconet.domain.dto.Note;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("notes")
public interface NotesClient {

  @PostMapping
  Note postNote(@RequestBody Note note);

  @DeleteMapping("/{id}")
  Note deleteNote(@PathVariable String id);
}

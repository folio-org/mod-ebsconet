package org.folio.ebsconet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.ebsconet.client.NoteLinksClient;
import org.folio.ebsconet.client.NoteTypeClient;
import org.folio.ebsconet.client.NotesClient;
import org.folio.ebsconet.domain.dto.Link;
import org.folio.ebsconet.domain.dto.Note;
import org.folio.ebsconet.models.MappingDataHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class NotesService {
  private final NoteTypeClient noteTypeClient;
  private final NotesClient notesClient;
  private final NoteLinksClient noteLinksClient;
  public static final String GENERAL_NOTE_TYPE = "General note";
  public static final String EBSCONET_CUSTOMER_NOTE = "EBSCONET Customer Note";

  public Note createNote(Note note) {
    log.info("Create customer note {}", note);
    return notesClient.postNote(note);
  }

  public void linkCustomerNote(MappingDataHolder mappingDataHolder) {
    String generalNoteTypeId = noteTypeClient.getNoteTypesByQuery("name==" + GENERAL_NOTE_TYPE)
      .get("noteTypes")
      .get(0)
      .get("id")
      .asText();

    var note = this.getNoteByPoLineId(generalNoteTypeId, mappingDataHolder.getCompositePoLine().getId());
    if (note == null) {
      note = buildNewPoLineNote(mappingDataHolder.getCompositePoLine().getId(), generalNoteTypeId);
      createNote(note);
    }
  }

  public Note getNoteByPoLineId(String generalNoteTypeId, String polineId) {
    var notes = noteLinksClient.getNotesByPoLineId(polineId).getNotes();

    return notes.stream()
      .filter(note -> generalNoteTypeId.equals(note.getTypeId()) && GENERAL_NOTE_TYPE.equals(note.getType()))
      .findFirst()
      .orElse(null);
  }

  private Note buildNewPoLineNote(String polineId, String generalNoteTypeId) {

    var link = new Link().id(polineId).type("poLine");

    var links = Collections.singletonList(link);
    return new Note().id(UUID.randomUUID().toString())
      .domain("orders")
      .typeId(generalNoteTypeId)
      .title(EBSCONET_CUSTOMER_NOTE)
      .links(links);
  }
}

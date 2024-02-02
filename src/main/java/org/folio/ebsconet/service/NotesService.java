package org.folio.ebsconet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.StringUtils;
import org.folio.ebsconet.client.NoteLinksClient;
import org.folio.ebsconet.client.NoteTypeClient;
import org.folio.ebsconet.client.NotesClient;
import org.folio.ebsconet.domain.dto.Link;
import org.folio.ebsconet.domain.dto.Note;
import org.folio.ebsconet.error.ResourceNotFoundException;
import org.folio.ebsconet.models.MappingDataHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Objects;
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
    log.info("Creating customer note: {}", note);
    return notesClient.postNote(note);
  }

  public void deleteNote(String id) {
    log.info("Deleting customer note by id {}", id);
    notesClient.deleteNote(id);
  }

  public void linkCustomerNote(MappingDataHolder mappingDataHolder) {
    log.debug("Link customer note to poLine");
    var generalNote = noteTypeClient.getNoteTypesByQuery("name==" + GENERAL_NOTE_TYPE)
      .get("noteTypes")
      .get(0);
    if (generalNote == null) {
      throw new ResourceNotFoundException("Note not found with name==" + GENERAL_NOTE_TYPE);
    }
    String generalNoteTypeId = generalNote
      .get("id")
      .asText();
    var note = this.getNoteByPoLineId(generalNoteTypeId, mappingDataHolder.getCompositePoLine().getId());
    log.info("Note {}, generalNotTypeId: {}", note, generalNoteTypeId);
    String customerNote = mappingDataHolder.getEbsconetOrderLine().getCustomerNote();
    if (StringUtils.isNotBlank(customerNote)) {
      if (note == null) {
        log.warn("Customer note is not found for poLineId: {}", mappingDataHolder.getCompositePoLine().getId());
        note = buildNewPoLineNote(mappingDataHolder.getCompositePoLine().getId(),
          customerNote, generalNoteTypeId);
      } else {
        note.setContent(customerNote);
      }
      log.info("Creating note: {}", note);
      createNote(note);
    } else if (Objects.nonNull(note)) {
      log.warn("Ebsconet order line customer note is empty for poLineId: {}", mappingDataHolder.getCompositePoLine().getId());
      deleteNote(note.getId());
    }

  }

  public Note getNoteByPoLineId(String generalNoteTypeId, String polineId) {
    var notes = noteLinksClient.getNotesByPoLineId(polineId).getNotes();

    return notes.stream()
      .filter(note -> generalNoteTypeId.equals(note.getTypeId()) && GENERAL_NOTE_TYPE.equals(note.getType()))
      .findFirst()
      .orElse(null);
  }

  private Note buildNewPoLineNote(String polineId, String note, String generalNoteTypeId) {
    var link = new Link().id(polineId).type("poLine");

    var links = Collections.singletonList(link);
    return new Note().id(UUID.randomUUID().toString())
      .domain("orders")
      .content(note)
      .typeId(generalNoteTypeId)
      .title(EBSCONET_CUSTOMER_NOTE)
      .links(links);
  }
}

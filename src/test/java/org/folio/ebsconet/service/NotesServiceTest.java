package org.folio.ebsconet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.ebsconet.client.NoteLinksClient;
import org.folio.ebsconet.client.NoteTypeClient;
import org.folio.ebsconet.client.NotesClient;
import org.folio.ebsconet.domain.dto.EbsconetOrderLine;
import org.folio.ebsconet.domain.dto.Note;
import org.folio.ebsconet.domain.dto.NoteCollection;
import org.folio.ebsconet.domain.dto.PoLine;
import org.folio.ebsconet.error.ResourceNotFoundException;
import org.folio.ebsconet.models.MappingDataHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotesServiceTest {

  @Mock
  private NoteTypeClient noteTypeClient;
  @Mock
  private NotesClient notesClient;
  @Mock
  private NoteLinksClient noteLinksClient;
  @InjectMocks
  private NotesService notesService;

  @Test
  void testLinkingNoteToPoline() throws IOException {
    var holder = new MappingDataHolder();
    holder.setEbsconetOrderLine(new EbsconetOrderLine());
    holder.getEbsconetOrderLine().setCustomerNote("TestNote");
    holder.setPoLine(new PoLine().id(UUID.randomUUID().toString()));
    NoteCollection sampleNoteCollection = new ObjectMapper().readValue(new ClassPathResource("/mockdata/notes.json").getFile(), NoteCollection.class);
    Note sampleNote = sampleNoteCollection.getNotes().getFirst();
    JsonNode sampleNoteTypes = new ObjectMapper().readValue(new ClassPathResource("/mockdata/note-types.json").getFile(), JsonNode.class);

    when(noteTypeClient.getNoteTypesByQuery(anyString())).thenReturn(sampleNoteTypes);
    when(noteLinksClient.getNotesByPoLineId(anyString())).thenReturn(new NoteCollection().notes(new ArrayList<>()));
    when(notesClient.postNote(any())).thenReturn(sampleNote);

    notesService.linkCustomerNote(holder);
    ArgumentCaptor<Note> argumentCaptor = ArgumentCaptor.forClass(Note.class);
    verify(notesClient).postNote(argumentCaptor.capture());
    Note note = argumentCaptor.getValue();

    assertNotNull(note.getTypeId());
  }

  @Test
  void missingGeneralNote() throws IOException {
    var holder = new MappingDataHolder();
    holder.setEbsconetOrderLine(new EbsconetOrderLine());
    holder.getEbsconetOrderLine().setCustomerNote("TestNote");
    holder.setPoLine(new PoLine().id(UUID.randomUUID().toString()));
    JsonNode sampleNoteTypes = new ObjectMapper().readValue("{\"noteTypes\": []}", JsonNode.class);

    when(noteTypeClient.getNoteTypesByQuery(anyString())).thenReturn(sampleNoteTypes);

    assertThrows(ResourceNotFoundException.class, () -> notesService.linkCustomerNote(holder));
  }

}

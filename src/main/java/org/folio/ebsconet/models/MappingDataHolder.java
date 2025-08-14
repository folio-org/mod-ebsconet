package org.folio.ebsconet.models;

import lombok.Data;
import org.folio.ebsconet.domain.dto.EbsconetOrderLine;
import org.folio.ebsconet.domain.dto.Fund;
import org.folio.ebsconet.domain.dto.PoLine;

@Data
public class MappingDataHolder {
  private PoLine poLine;
  private EbsconetOrderLine ebsconetOrderLine;
  private Fund fund;
  private String expenseClass;
}

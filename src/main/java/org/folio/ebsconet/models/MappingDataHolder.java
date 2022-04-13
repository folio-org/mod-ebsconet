package org.folio.ebsconet.models;

import org.folio.ebsconet.domain.dto.CompositePoLine;
import org.folio.ebsconet.domain.dto.EbsconetOrderLine;
import org.folio.ebsconet.domain.dto.Fund;

public class MappingDataHolder {
  CompositePoLine compositePoLine;
  EbsconetOrderLine ebsconetOrderLine;
  Fund fund;
  String expenseClass;

  public CompositePoLine getCompositePoLine() {
    return compositePoLine;
  }

  public void setCompositePoLine(CompositePoLine poLine) {
    this.compositePoLine = poLine;
  }

  public EbsconetOrderLine getEbsconetOrderLine() {
    return ebsconetOrderLine;
  }

  public void setEbsconetOrderLine(EbsconetOrderLine ebsconetOrderLine) {
    this.ebsconetOrderLine = ebsconetOrderLine;
  }

  public Fund getFund() {
    return fund;
  }

  public void setFund(Fund fund) {
    this.fund = fund;
  }

  public String getExpenseClass() {
    return expenseClass;
  }

  public void setExpenseClass(String expenseClass) {
    this.expenseClass = expenseClass;
  }

}

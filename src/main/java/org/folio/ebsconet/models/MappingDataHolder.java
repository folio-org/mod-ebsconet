package org.folio.ebsconet.models;

import org.folio.ebsconet.domain.dto.EbsconetOrderLine;
import org.folio.ebsconet.domain.dto.Fund;
import org.folio.ebsconet.domain.dto.PoLine;

public class MappingDataHolder {
  PoLine poLine;
  EbsconetOrderLine ebsconetOrderLine;
  Fund fund;
  String expenseClass;

  public PoLine getPoLine() {
    return poLine;
  }

  public void setPoLine(PoLine poLine) {
    this.poLine = poLine;
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

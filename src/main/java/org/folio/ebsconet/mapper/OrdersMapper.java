package org.folio.ebsconet.mapper;

import org.folio.ebsconet.domain.dto.CompositePoLine;
import org.folio.ebsconet.domain.dto.Cost;
import org.folio.ebsconet.domain.dto.EbsconetOrderLine;
import org.folio.ebsconet.domain.dto.Fund;
import org.folio.ebsconet.domain.dto.FundDistribution;
import org.folio.ebsconet.domain.dto.Organization;
import org.folio.ebsconet.domain.dto.PoLine;
import org.folio.ebsconet.domain.dto.PurchaseOrder;
import org.folio.ebsconet.domain.dto.Source;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class OrdersMapper {
  @Mapping(target = "vendor", source = "vendor.code")
  @Mapping(target = "cancellationRestriction", source = "line.cancellationRestriction")
  @Mapping(target = "cancellationRestrictionNote", source = "line.cancellationRestrictionNote")
  @Mapping(target = "unitPrice", source = "line", qualifiedByName = "getUnitPrice")
  @Mapping(target = "currency", source = "line.cost.currency")
  @Mapping(target = "vendorReferenceNumbers", source = "line.vendorDetail.referenceNumbers")
  @Mapping(target = "poLineNumber", source = "line.poLineNumber")
  @Mapping(target = "subscriptionToDate", source = "line.details.subscriptionTo")
  @Mapping(target = "subscriptionFromDate", source = "line.details.subscriptionFrom")
  @Mapping(target = "quantity", source = "line", qualifiedByName = "getQuantity")
  @Mapping(target = "fundCode", source = "line", qualifiedByName = "getFundCode")
  @Mapping(target = "publisherName", source = "line.publisher")
  @Mapping(target = "vendorAccountNumber", source = "line.vendorDetail.vendorAccount")
  @Mapping(target = "workflowStatus", source = "order.workflowStatus")
  public abstract EbsconetOrderLine folioToEbsconet(PurchaseOrder order, PoLine line, Organization vendor);

  @Named("getFundCode")
  public String getFundCode(PoLine line) {
    List<FundDistribution> distributions = line.getFundDistribution();
    if (distributions == null || distributions.isEmpty())
      return null;
    return distributions.get(0).getCode();
  }

  @Named("getQuantity")
  public Integer getQuantity(PoLine line) {
    Integer physical = Optional.ofNullable(line.getCost()).map(Cost::getQuantityPhysical).orElse(0);
    Integer electronic = Optional.ofNullable(line.getCost()).map(Cost::getQuantityElectronic).orElse(0);
    return physical + electronic;
  }

  @Named("getUnitPrice")
  public BigDecimal getUnitPrice(PoLine line) {
    BigDecimal physical = Optional.ofNullable(line.getCost()).map(Cost::getListUnitPrice).orElse(BigDecimal.ZERO);
    BigDecimal electronic = Optional.ofNullable(line.getCost()).map(Cost::getListUnitPriceElectronic).orElse(BigDecimal.ZERO);
    return physical.add(electronic);
  }

  public void ebsconetToFolio(CompositePoLine poLine, EbsconetOrderLine ebsconetOrderLine, Fund fund) {
    poLine.setSource(Source.EBSCONET);
    poLine.setCancellationRestriction(ebsconetOrderLine.getCancellationRestriction());
    poLine.setCancellationRestrictionNote(ebsconetOrderLine.getCancellationRestrictionNote());
    poLine.getCost().setCurrency(ebsconetOrderLine.getCurrency());
    poLine.getVendorDetail().setReferenceNumbers(ebsconetOrderLine.getVendorReferenceNumbers());
    poLine.getDetails().setSubscriptionTo(ebsconetOrderLine.getSubscriptionToDate());
    poLine.getDetails().setSubscriptionFrom(ebsconetOrderLine.getSubscriptionFromDate());
    poLine.getVendorDetail().setVendorAccount(ebsconetOrderLine.getVendorAccountNumber());
    poLine.setPublisher(ebsconetOrderLine.getPublisherName());

    populateCostAndLocations(poLine, ebsconetOrderLine);

    if (fund != null) {
      poLine.getFundDistribution().get(0).setCode(fund.getCode());
      poLine.getFundDistribution().get(0).setFundId(fund.getId());
    }
  }

  private void populateCostAndLocations(CompositePoLine poLine, EbsconetOrderLine ebsconetOrderLine) {
    if (poLine.getLocations().size() == 1) {
      switch (poLine.getOrderFormat()) {
      case ELECTRONIC_RESOURCE:
        populateElectronicCostAndLocation(poLine, ebsconetOrderLine);
        break;
      case OTHER:
      case PHYSICAL_RESOURCE:
        populatePhysicalCostAndLocation(poLine, ebsconetOrderLine);
        break;
      case P_E_MIX:
        populateCostAndLocationPEMix(poLine, ebsconetOrderLine);
        break;
      }

    }
  }

  private void populatePhysicalCostAndLocation(CompositePoLine poLine, EbsconetOrderLine ebsconetOrderLine) {
    poLine.getCost().setQuantityPhysical(ebsconetOrderLine.getQuantity());
    poLine.getCost().setListUnitPrice(ebsconetOrderLine.getUnitPrice());
    poLine.getLocations().get(0).setQuantityPhysical(ebsconetOrderLine.getQuantity());
    poLine.getLocations().get(0).setQuantity(ebsconetOrderLine.getQuantity());
  }

  private void populateElectronicCostAndLocation(CompositePoLine poLine, EbsconetOrderLine ebsconetOrderLine) {
    poLine.getCost().setQuantityElectronic(ebsconetOrderLine.getQuantity());
    poLine.getCost().setListUnitPriceElectronic(ebsconetOrderLine.getUnitPrice());

    poLine.getLocations().get(0).setQuantityElectronic(ebsconetOrderLine.getQuantity());
    poLine.getLocations().get(0).setQuantity(ebsconetOrderLine.getQuantity());
  }

  private void populateCostAndLocationPEMix(CompositePoLine poLine, EbsconetOrderLine ebsconetOrderLine) {
    // Price problem
    // physical = 0 && electronic > 0
    if (poLine.getCost().getListUnitPrice().equals(BigDecimal.ZERO) && poLine.getCost().getListUnitPriceElectronic().signum() > 0) {
      populateElectronicCostAndLocation(poLine, ebsconetOrderLine);
    }
    // physical > 0 && electronic = 0
    else if (poLine.getCost().getListUnitPriceElectronic().equals(BigDecimal.ZERO) && poLine.getCost().getListUnitPrice().signum() > 0) {
      populatePhysicalCostAndLocation(poLine, ebsconetOrderLine);
    }
    else if (poLine.getCost().getListUnitPriceElectronic().signum() > 0 && poLine.getCost().getListUnitPrice().signum() > 0) {
      // divide unit price
      BigDecimal newElectronicPrice = ebsconetOrderLine.getUnitPrice().divide(BigDecimal.valueOf(2));
      BigDecimal newPhysicalPrice = ebsconetOrderLine.getUnitPrice().subtract(newElectronicPrice);

      poLine.getCost().setListUnitPriceElectronic(newElectronicPrice);
      poLine.getCost().setListUnitPrice(newPhysicalPrice);
    }





    //
  }
}

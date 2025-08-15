package org.folio.ebsconet.controller;

import org.folio.tenant.rest.resource.TenantApi;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(value = "/_/")
@RestController("folioTenantController")
public class TenantController implements TenantApi {
}

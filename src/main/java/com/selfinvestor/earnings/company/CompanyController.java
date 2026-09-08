package com.selfinvestor.earnings.company;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {
    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    @GetMapping
    public List<CompanyResponse> list() {
        return service.allCompanies().stream().map(CompanyResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse add(@Valid @RequestBody CreateCompanyRequest request) {
        return CompanyResponse.from(service.add(request.ticker(), request.cik(), request.companyName(), request.enabled()));
    }

    @PatchMapping("/{id}/enabled")
    public CompanyResponse setEnabled(@PathVariable Long id, @RequestBody EnabledRequest request) {
        return CompanyResponse.from(service.setEnabled(id, request.enabled()));
    }

    public record CreateCompanyRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z.\\-]{1,16}") String ticker,
            @NotBlank @Pattern(regexp = "\\d{1,10}") String cik,
            @NotBlank String companyName,
            boolean enabled) {}
    public record EnabledRequest(boolean enabled) {}
    public record CompanyResponse(Long id, String ticker, String cik, String companyName, boolean enabled) {
        static CompanyResponse from(Company company) {
            return new CompanyResponse(company.getId(), company.getTicker(), company.getCik(), company.getCompanyName(), company.isEnabled());
        }
    }
}

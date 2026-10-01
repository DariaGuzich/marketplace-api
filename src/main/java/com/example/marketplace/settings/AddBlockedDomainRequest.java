package com.example.marketplace.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record AddBlockedDomainRequest(@JsonProperty("domain") @NotBlank String domain) {
}

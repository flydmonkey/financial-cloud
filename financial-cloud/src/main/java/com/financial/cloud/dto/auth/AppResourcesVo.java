package com.financial.cloud.dto.auth;

import java.util.Set;

import com.financial.cloud.domain.permissions.Resources;

public record AppResourcesVo(Set<Resources> functions) {

}

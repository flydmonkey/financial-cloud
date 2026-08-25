package com.jinbooks.dto.auth;

import java.util.Set;

import com.jinbooks.domain.permissions.Resources;

public record AppResourcesVo(Set<Resources> functions) {

}

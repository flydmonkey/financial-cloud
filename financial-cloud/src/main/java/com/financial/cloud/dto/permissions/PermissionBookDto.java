package com.financial.cloud.dto.permissions;

import java.util.ArrayList;

public record PermissionBookDto(String userId,ArrayList<String> bookIds) {

}

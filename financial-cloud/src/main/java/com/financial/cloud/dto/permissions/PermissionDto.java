package com.financial.cloud.dto.permissions;

import java.util.ArrayList;

public record PermissionDto(String appId,String roleId,ArrayList<String> resourceIds) {

}

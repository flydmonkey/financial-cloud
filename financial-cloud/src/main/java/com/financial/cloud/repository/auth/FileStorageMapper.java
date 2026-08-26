/**
 *
 */
package com.financial.cloud.repository.auth;

import com.financial.cloud.repository.auth.FileStorageMapper;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.auth.FileStorage;

/**
 * @author Crystal.sea
 *
 */

@Mapper
public  interface FileStorageMapper extends BaseMapper<FileStorage> {

}

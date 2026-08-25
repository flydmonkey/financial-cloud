/**
 *
 */
package com.jinbooks.repository.auth;

import com.jinbooks.repository.auth.FileStorageMapper;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinbooks.domain.auth.FileStorage;

/**
 * @author Crystal.sea
 *
 */

@Mapper
public  interface FileStorageMapper extends BaseMapper<FileStorage> {

}

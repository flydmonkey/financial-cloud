package com.financial.cloud.service.auth;


import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.domain.auth.FileStorage;
import com.financial.cloud.repository.auth.FileStorageMapper;
import com.financial.cloud.service.auth.FileStorageService;

import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class FileStorageService  extends ServiceImpl<FileStorageMapper,FileStorage>{

	private final FileStorageMapper fileStorageMapper;

	public FileStorageMapper getMapper() {
		return fileStorageMapper;
	}

}

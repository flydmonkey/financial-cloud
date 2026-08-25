package com.jinbooks.service.auth;


import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.domain.auth.FileStorage;
import com.jinbooks.repository.auth.FileStorageMapper;
import com.jinbooks.service.auth.FileStorageService;

import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class FileStorageService  extends ServiceImpl<FileStorageMapper,FileStorage>{

	private final FileStorageMapper fileStorageMapper;

	public FileStorageMapper getMapper() {
		return fileStorageMapper;
	}

}

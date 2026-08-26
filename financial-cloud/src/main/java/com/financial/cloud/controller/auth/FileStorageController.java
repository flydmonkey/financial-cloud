package com.financial.cloud.controller.auth;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.List;

import com.financial.cloud.enums.CommonErrorCode;
import com.financial.cloud.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.util.Base64ImageUtils;
import com.financial.cloud.domain.auth.FileStorage;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.auth.FileStorageService;
import com.financial.cloud.context.WebContext;

/**
 * 文件上次实现/filestorage/upload/
 *
 * <p>上传文件到表MXK_FILE_STORAGE，当前无法存储大文件，需要第三方存储支持</p>
 *
 * @author Crystal.Sea
 *
 */
@RequiredArgsConstructor
@Slf4j
@Controller
@RequestMapping(value = { "/api/filestorage" })
public class FileStorageController {


	private final FileStorageService fileUploadService;

 	@RequestMapping(value={"/api/upload/"})
 	@ResponseBody
 	public Message<Object> upload( HttpServletRequest request,
 	                            HttpServletResponse response,
 	                           @ModelAttribute FileStorage fileStorage,
 	                           @CurrentUser UserInfo currentUser){
 		log.debug("FileUpload");
 		fileStorage.setId(WebContext.genId());
 		fileStorage.setContentType(fileStorage.getUploadFile().getContentType());
 		fileStorage.setFileName(fileStorage.getUploadFile().getOriginalFilename());
 		fileStorage.setContentSize(fileStorage.getUploadFile().getSize());
 		fileStorage.setCreatedBy(currentUser.getUsername());
 		/*
		 * upload UploadFile MultipartFile  to Uploaded Bytes
		 */
		if(null!=fileStorage.getUploadFile()&&!fileStorage.getUploadFile().isEmpty()){
			try {
				fileStorage.setDataStored(fileStorage.getUploadFile().getBytes());
				fileUploadService.save(fileStorage);
				log.trace("FileUpload SUCCESS");
			} catch (IOException e) {
				log.error("FileUpload IOException",e);
			}
		}
 		return new Message<>(Message.SUCCESS,(Object)fileStorage.getId());
 	}

 	@GetMapping(value={"/image/{id}"})
 	@ResponseBody
 	public Message<String> view(@PathVariable("id") String id){
 		FileStorage fileStorage = fileUploadService.getById(id);
 		if(fileStorage != null && fileStorage.getDataStored() != null) {
 			return new Message<>(Base64ImageUtils.encodePngBytes(fileStorage.getDataStored()));
 		} else {
			 throw new BusinessException(CommonErrorCode.FILE_NOT_FOUND);
		}
 	}

 	@GetMapping(value={"/image/{id}.png"})
 	public String viewPng(@PathVariable("id") String id){
 		FileStorage fileStorage = fileUploadService.getById(id);
 		if(fileStorage != null && fileStorage.getDataStored() != null) {
 			return Base64ImageUtils.encodePngBytes(fileStorage.getDataStored());
 		}
 		return "";
 	}

 	@GetMapping(value={"/image/getByIds"})
 	@ResponseBody
 	public Message<List<FileStorage>> getByIds(@RequestParam("ids") List<String> ids){
 		List<FileStorage> fileStorageList = fileUploadService.listByIds(ids);
 		for(FileStorage fileStorage : fileStorageList) {
	 		if(fileStorage != null && fileStorage.getDataStored() != null) {
	 			fileStorage.setImageBase64(Base64ImageUtils.encodePngBytes(fileStorage.getDataStored()));
	 		}
 		}
 		return new Message<>(Message.SUCCESS,fileStorageList);
 	}

 	@DeleteMapping(value={"/image/delete"})
 	@ResponseBody
 	public Message<String> delete(@RequestParam("ids") List<String> ids){
 		fileUploadService.removeBatchByIds(ids);
 		return new Message<>(Message.SUCCESS,"");
 	}


}

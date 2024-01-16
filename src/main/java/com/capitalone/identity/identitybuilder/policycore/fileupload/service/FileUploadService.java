package com.capitalone.identity.identitybuilder.policycore.fileupload.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.chassis.engine.model.exception.RequestValidationException;
import com.capitalone.identity.identitybuilder.policycore.fileupload.camel.util.FileUploadCamelCacheUtil;
import com.capitalone.identity.identitybuilder.policycore.fileupload.model.FileUploadResponse;
import com.capitalone.identity.identitybuilder.policycore.fileupload.model.FileuploadData;
import com.capitalone.identity.identitybuilder.policycore.fileupload.onestream.CureFileuploadRawPayload;
import com.capitalone.identity.identitybuilder.policycore.fileupload.onestream.FileuploadCureSdpListener;

@Component
@ConditionalOnProperty(value = FileUploadService.FILE_UPLOAD_ENABLED_FLAG)
public class FileUploadService {

	private static final String TEMPFILE_NAME = "TEMPFILE_";
	private static final String DOCUMENT_FAILED_AT_VIRUS_SCAN_SERVICE = "Document failed at virus scan service";
	private static final String FILE_UPLOAD_PROCESS_FAILED = "fileUpload process failed at encryption handling";
	public static final String FILE_UPLOAD_ENABLED_FLAG = "identitybuilder.policycore.feature.fileupload.enabled";
	private static final String FILE_PREFIX = "FILE_";
	private static final int INTERNAL_SERVER_ERROR_ID = 200003;
	private static final int VALIDATION_ERROR_ID = 20000;

	private final Logger logger = LogManager.getLogger(getClass());

	@Autowired
	private FileUploadEncryptionKeyService keyService;

	@Autowired
	private FileUploadCamelCacheUtil cacheUtil;

	@Autowired
	private FileUploadVirusScanService scanService;

	@Autowired
	private FileuploadCureSdpListener fileuploadCureSdpListener;

	/**
	 * This method is responsible for encrypting and uploading document to domain
	 * cache
	 * 
	 * @param unencryptedFileUploadData
	 *
	 * @param encryptedFileUploadData
	 *
	 * @return FileUploadResponse
	 */
	public FileUploadResponse uploadDocument(FileuploadData unencryptedFileUploadData, FileuploadData encryptedFileUploadData) {
		String fileReferenceId = null;
		boolean virusDetected = true;
		fileReferenceId = UUID.randomUUID().toString();
		virusDetected = isVirusAffected(unencryptedFileUploadData);
		boolean isFileEncrypted = encryptedFileUploadData != null;
		if (virusDetected) {
			publishToOneStream(unencryptedFileUploadData, fileReferenceId, getScanResult(virusDetected), isFileEncrypted, false,
					VALIDATION_ERROR_ID, DOCUMENT_FAILED_AT_VIRUS_SCAN_SERVICE, HttpStatus.SC_BAD_REQUEST);
			throw new RequestValidationException(DOCUMENT_FAILED_AT_VIRUS_SCAN_SERVICE);
		}

		try {
			String encryptedData;
			if(isFileEncrypted){
				encryptedData = new String(encryptedFileUploadData.getDocument(), StandardCharsets.UTF_8);
			} else {
				encryptedData = keyService.encryptDocument(unencryptedFileUploadData.getProductId(),
						unencryptedFileUploadData.getDocument());
			}
			writeDataIntoCache(fileReferenceId, encryptedData);
		} catch (ChassisSystemException ex) {
			logger.error(FILE_UPLOAD_PROCESS_FAILED, ex);
			publishToOneStream(unencryptedFileUploadData, fileReferenceId, getScanResult(virusDetected), isFileEncrypted,false,
					INTERNAL_SERVER_ERROR_ID, ex.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
			throw new ChassisSystemException(ex.getApiError(), ex.getHttpStatusCode());
		}

		logger.info("File uploaded successfully with fileId {} ", fileReferenceId);
		publishToOneStream(unencryptedFileUploadData, fileReferenceId, getScanResult(virusDetected), isFileEncrypted,true, 0, null, HttpStatus.SC_OK);
		return FileUploadResponse.builder().fileId(fileReferenceId).build();
	}

	private String getScanResult(boolean isVirusAffected) {
		return isVirusAffected ? "infected" : "clean";
	}

	/**
	 * This method is responsible for writing the data to domain cache
	 * 
	 * @param fileReferenceId
	 * @param encryptedData
	 */
	private void writeDataIntoCache(String fileReferenceId, String encryptedData) {
		cacheUtil.populateCache(FILE_PREFIX + fileReferenceId, encryptedData);
	}

	/**
	 * This method is responsible for checking the virusScan
	 * 
	 * @param byte
	 *            [] document
	 * @return boolean isAffected
	 */
	private boolean isVirusAffected(FileuploadData fileuploadData) {
		boolean isAffected = false;
		FileOutputStream outputStream = null;
		File file = null;
		FileSystemResource fileResource = null;
		if (null != fileuploadData.getDocument()) {
			try {	    
				file = new File(TEMPFILE_NAME + UUID.randomUUID().toString());
				logger.info("creating temp file for scan {} ", file.toPath());
				outputStream = new FileOutputStream(file);
				outputStream.write(fileuploadData.getDocument());
				fileResource = new FileSystemResource(file);
				isAffected = scanService.isVirusAffected(fileResource);
			} catch (Exception ex) {
				logger.error(String.format("CCID: %s failed at virus scan",fileuploadData.getClientCorrelationId()));
				throw new ChassisSystemException("Exception while scanning files for virus", ex);
			} finally {
				try {
					if (null != outputStream && null != file) {
						outputStream.close();
						Files.delete(file.toPath());
					}

				} catch (IOException ex) {
					logger.error("Exception while cleaning up the files " + ex);
				}
			}
		}

		return isAffected;
	}

	private void publishToOneStream(FileuploadData fileuploadData, String fileReferenceId, String virusScanresult, boolean isFileEncrypted, //NOSONAR
			boolean isFileUploadSuccess, int errorId, String message, int httpStatatus) {
		try {

			long fileSize = 0;
			if (Optional.ofNullable(fileuploadData.getDocument()).isPresent()) {
				fileSize = fileuploadData.getDocument().length;
			}
			CureFileuploadRawPayload cureFileuploadRawPayload = new CureFileuploadRawPayload(
					fileuploadData.getClientCorrelationId(), fileuploadData.getCustomerIpAddress(),
					fileuploadData.getChannelType(), fileReferenceId, fileuploadData.getProductId(), fileSize,
					virusScanresult, isFileEncrypted, isFileUploadSuccess, errorId, message, httpStatatus);
			fileuploadCureSdpListener.invokeCureSDPStream(cureFileuploadRawPayload);

		} catch (Exception e) {
			logger.error("Exception occured when publishing to onestream {}", e);
		}
	}

}

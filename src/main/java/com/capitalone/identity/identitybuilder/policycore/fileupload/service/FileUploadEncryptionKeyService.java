package com.capitalone.identity.identitybuilder.policycore.fileupload.service;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.capitalone.chassis.engine.model.exception.ChassisErrorCode;
import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.identity.identitybuilder.policycore.fileupload.model.DevXPublicKeyServiceResponse;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.dao.impl.KeyserviceDao;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.util.FileuploadUtil;
import com.capitalone.identity.identitybuilder.policycore.service.util.CryptoUtil;
import com.capitalone.identity.identitybuilder.policycore.service.util.JWTKey;
import com.nimbusds.jose.JOSEException;

@Component
@ConditionalOnProperty(value = FileUploadService.FILE_UPLOAD_ENABLED_FLAG)
public class FileUploadEncryptionKeyService {

	private static final String ERROR_STRING = "exception occured when encrypting the Document";

	@Value("${identitybuilder.policycore.fileupload.krp.productId}")
	private String publicKeyProductId;

	private final Logger logger = LogManager.getLogger(getClass());

	@Autowired
	private KeyserviceDao keyserviceDao;
	
	@Autowired
	private FileuploadUtil fileuploadUtil;

	private Map<String, DevXPublicKeyServiceResponse> synchronizedKeysMap;

	@Autowired
	public FileUploadEncryptionKeyService(
			@Qualifier("multipartPublicKeysMap") PassiveExpiringMap<String, DevXPublicKeyServiceResponse> publicKeysMap) {
		synchronizedKeysMap = Collections.synchronizedMap(publicKeysMap);
	}

	/**
	 * This method is responsible for encrypting a document
	 * 
	 * @param String productId
	 * @param byte[] document
	 * @return A encrypted Data for the document
	 */
	public String encryptDocument(String productId, byte[] document) {

		String encryptedData = null;
		DevXPublicKeyServiceResponse response = getPublicKey(productId);
		JWTKey jwtKey = response.getKeys().get(0);
		try {
			encryptedData = CryptoUtil.encryptEcdh(jwtKey, document);
		} catch (IllegalArgumentException | JOSEException ex) {
			logger.error(ERROR_STRING, ex);
			ChassisErrorCode error =  fileuploadUtil.getChassisSystemError(ERROR_STRING);
			throw new ChassisSystemException(error);
		}
		return encryptedData;
	}

	/**
	 * This method is responsible for fetching pubicKey Object
	 * 
	 * @param keyRotationProductId
	 * @return DevXPublicKeyServiceResponse
	 */
	public DevXPublicKeyServiceResponse getPublicKey(String keyRotationProductId) {

		String productId = publicKeyProductId;
		if (null != keyRotationProductId) {
			productId = keyRotationProductId;
		}
		if (synchronizedKeysMap.get(productId) != null && synchronizedKeysMap.get(productId).getKeys() != null
			&& !synchronizedKeysMap.get(productId).getKeys().isEmpty()
				&& synchronizedKeysMap.get(productId).getKeys().get(0) != null
				&& !synchronizedKeysMap.get(productId).getKeys().get(0).isExpired()) {
			logger.info("Public Key retrieved from cache");
			return synchronizedKeysMap.get(productId);
		}

		DevXPublicKeyServiceResponse response = keyserviceDao.getDevxPublicKey(productId);
		synchronizedKeysMap.put(productId, response);
		return response;
	}

}

package com.capitalone.identity.identitybuilder.policycore.fileupload.rest.v1;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.nimbusds.jose.JWEObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.capitalone.chassis.engine.annotations.logging.Log;
import com.capitalone.chassis.engine.annotations.logging.Profile;
import com.capitalone.chassis.engine.core.resource.AbstractBaseResource;
import com.capitalone.chassis.engine.model.exception.RequestValidationException;
import com.capitalone.identity.identitybuilder.policycore.crypto.COSRequestProperties;
import com.capitalone.identity.identitybuilder.policycore.crypto.DeCryptService;
import com.capitalone.identity.identitybuilder.policycore.fileupload.model.FileUploadResponse;
import com.capitalone.identity.identitybuilder.policycore.fileupload.model.FileuploadData;
import com.capitalone.identity.identitybuilder.policycore.fileupload.onestream.CureFileuploadRawPayload;
import com.capitalone.identity.identitybuilder.policycore.fileupload.onestream.FileuploadCureSdpListener;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.FileUploadService;

import io.swagger.annotations.ApiParam;

@Log
@Profile
@Component
@Singleton
@Path("/private/189898/identity/domain/services")
@Consumes({ MediaType.MULTIPART_FORM_DATA })
@Produces({ "application/vnd.com.capitalone.api+v1+json", MediaType.APPLICATION_JSON })
@ConditionalOnProperty(value = FileUploadService.FILE_UPLOAD_ENABLED_FLAG)
public class FileUploadResource extends AbstractBaseResource {

	private static final int REQUEST_VALIDATION_ERROR_ID = 200000;
	private static final int CCID_MAX_LENGTH = 256;
	private static final long TEN_MB = 10 * 1024L * 1024L;
	private static final String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
			+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
			+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
	private static final Set<String> CHANNEL_TYPE = new HashSet<>(Arrays.asList("Web", "Mobile", "MobileWeb"));

	private static final String X_UPSTREAM_ENV = "verification";

	@Autowired
	private FileUploadService documentUploadService;

	@Autowired
	private FileuploadCureSdpListener fileuploadCureSdpListener;

	@Autowired
	private DeCryptService decryptService;

	@Autowired
	private Environment env;

	@Value("${identitybuilder.policycore.fileupload.request-decrypt.cos-root}")
	private String domainIdentifier;

	@Value("#{'${identitybuilder.policycore.fileupload.request-decrypt.product-id-allow-list}'}")
	private List<String> allowListOfProdIds;

	@POST
	@Path("/upload")
	public Response uploadImage(
			@ApiParam(value = "Clients who utilize this are responsible for generating the value, guaranteeing uniqueness, and providing it as a Header in every request.", required = true) @HeaderParam("Client-Correlation-Id") @NotNull(message = "CCID must not be null") String clientCorrelationId,
			@ApiParam(value = "IP address of the customer system") @HeaderParam("Customer-IP-Address") String customerIpAddress,
			@ApiParam(value = "Channel-Type of the customer System") @HeaderParam("Channel-Type") String channelType,
			@ApiParam(value = "X-Upstream-Env of the system") @HeaderParam("X-Upstream-Env") String xUpstreamEnv,
			@HeaderParam("productId") String productId, @FormDataParam("file") InputStream is,
			@FormDataParam("file") FormDataContentDisposition fileDetail, @FormDataParam("file") FormDataBodyPart body,
			@Context HttpServletRequest request, @Context HttpServletResponse responses) throws IOException {

		if (body != null && body.getParent() != null && body.getParent().getBodyParts() != null
				&& body.getParent().getBodyParts().size() > 1) {
			logger.error("file upload receives multiple files which is not valid",
					body.getParent().getBodyParts().size());
			throw new RequestValidationException(
					"File upload process received more than one file, please upload one file at a time");
		}

		if (null == is) {
			throw new RequestValidationException("Document is null or empty");
		}

		byte[] rawData = convertToByteArray(is);

		// Validate request data prior to processing
		FileuploadData initialRequestData = new FileuploadData(clientCorrelationId, customerIpAddress, channelType,
				xUpstreamEnv, productId, rawData);
		validateHeaders(initialRequestData);

		byte[] decryptedData = decryptFileData(rawData,productId, initialRequestData.getClientCorrelationId());
		FileUploadResponse response;

		// If client data from decryptFileData() returns null, no decryption occurred, so virus scan & encrypt data for upload.
		// Otherwise, virus scan decrypted data, upload original encrypted client data w/o additional encryption.
		if(decryptedData == null){
			response = documentUploadService.uploadDocument(initialRequestData, null);
		} else {
			FileuploadData decryptedPayloadData = new FileuploadData(clientCorrelationId,
					customerIpAddress,
					channelType,
					xUpstreamEnv,
					productId,
					decryptedData);

			response = documentUploadService.uploadDocument(decryptedPayloadData, initialRequestData);
		}
		logger.info("file uploaded successfully and fileId {} ", response.getFileId());
		return Response.status(Response.Status.OK).entity(response).build();
	}

	// Process the payload data using the data & the given Lockbox CoS details
	protected byte[] decryptFileData(byte[] clientData, String productId, String ccid)
			throws IOException {

		String decryptedDoc;
		byte[] document;

		// Checks to determine file status & if file is encrypted with Base64 encoding
		boolean fileCheck = validateIfFileIsImage(new ByteArrayInputStream(clientData));
		boolean isEncryptedCheck = validateIfFileIsEncrypted(new ByteArrayInputStream(clientData));

		// If encryption is present, but product ID doesn't match known domains w/ encryption input, error out
		if(isEncryptedCheck && (productId == null || !allowListOfProdIds.contains(productId))){
			logger.error("CCID: {}, Image is encrypted, but provided productID {} is not in whitelist", ccid,productId);

			throw new RequestValidationException(
					"ProductID not valid for decryption.");
		}

		// Check if either payload isn't encrypted, not associated with a productID for decryption, or an image
		if(!isEncryptedCheck || !allowListOfProdIds.contains(productId) || fileCheck){
			logger.info("CCID: {}, Submitted document is either a normal image or not encrypted",ccid);
			document = null;
		} else {

			// Initialize prefix to retrieve Chamber of Secret values from properties file for possible decryption
			String cosPropertyPrefix = buildCOSProperties(domainIdentifier, productId);

			COSRequestProperties config = COSRequestProperties.builder()
					.lockBoxId(env.getProperty(cosPropertyPrefix + "lockBoxId"))
					.productId(env.getProperty(cosPropertyPrefix + "productId"))
					.vaultRole(env.getProperty(cosPropertyPrefix + "vaultRole"))
					.vaultAddress(env.getProperty(cosPropertyPrefix + "vaultAddress"))
					.build();

			// Any COS config properties of value NULL mean that there were issues in the app properties set
			if(config.getLockBoxId() == null || config.getProductId() == null || config.getVaultRole() == null
					|| config.getVaultAddress() == null){
				logger.error("Missing properties to decrypt with productId [ccid={}, productID={}, cosPropertyPrefix={}, config={}] led to errors in getting Chamber of Secrets properties"
						,ccid,productId,cosPropertyPrefix,config);
				throw new ChassisSystemException("Missing application properties for decryption w/ given productID");
			}

			// convert the inputstream to a byte array format, then decrypt
			String encodedDocument = new String(clientData, StandardCharsets.UTF_8);

			// Check if errors during decryption
			try{
				decryptedDoc = decryptService.decrypt(encodedDocument, config);
			} catch(Exception e){
				logger.error(String.format("CCID: %s, Encoded document: %s, ProductID: %s", ccid,encodedDocument,productId),e);
				throw new RequestValidationException("Encrypted image contains invalid format");
			}

			if(decryptedDoc == null){
				logger.error("CCID: {}, Encoded document: {}, ProductID: {}, Decryption result is null, failure during decryption", ccid,encodedDocument,productId);
				throw new RequestValidationException("Encrypted image contains invalid format");
			}

			document = decryptedDoc.getBytes();
		}
		return document;
	}

	// Determine if given file is of standard file types (.jpeg, .png, etc)
	private boolean validateIfFileIsImage(InputStream is){
		try{
			ImageIO.read(is).flush();
		}catch(Exception e){
			return false;
		}
		return true;
	}

	// If files are encrypted, they are also encoded, so check if encode is present in file
	private boolean validateIfFileIsEncrypted(InputStream is) throws IOException {

		String file = IOUtils.toString(is, StandardCharsets.UTF_8);
		if(file.length() > 0){
			try{
				JWEObject.parse(file);
			} catch (Exception e){
				return false;
			}
		} else {
			return false;
		}
		return true;
	}

	// Construct prefix to get values from variables in .properties files relevant to CoS lockbox details
	private String buildCOSProperties(String domain, String productID){
		String propertyPrefix = domain + ".";
		if(productID != null){
			propertyPrefix = propertyPrefix + productID  + ".";
		}
		return propertyPrefix;
	}

	private void validateHeaders(FileuploadData fileuploadData) {
		checkDocumentSize(fileuploadData);
		validateCCID(fileuploadData);
		validateCustomerIPAddress(fileuploadData);
		validateChannelType(fileuploadData);
		validatexUpstreamEnv(fileuploadData);
	}

	private void validatexUpstreamEnv(FileuploadData fileuploadData) {
		String xUpstreamEnv = fileuploadData.getXUpstreamEnv();
		if (!StringUtils.isEmpty(xUpstreamEnv) && !X_UPSTREAM_ENV.equalsIgnoreCase(xUpstreamEnv)) {
			logger.error("xUpstreamEnv is not valid {}", xUpstreamEnv);
			publishToOneStream(fileuploadData, "xUpstreamEnv is not valid");
			throw new RequestValidationException("xUpstreamEnv is not valid");
		}
	}

	private void validateChannelType(FileuploadData fileuploadData) {
		String channelType = fileuploadData.getChannelType();
		if (!StringUtils.isEmpty(channelType) && !CHANNEL_TYPE.contains(channelType)) {
			logger.error("channel Type is not valid {}", channelType);
			publishToOneStream(fileuploadData, "channel Type is not valid");
			throw new RequestValidationException("channel Type is not valid ");

		}
	}

	private void validateCustomerIPAddress(FileuploadData fileuploadData) {
		String customerIpAddress = fileuploadData.getCustomerIpAddress();
		if (!StringUtils.isEmpty(customerIpAddress)) {
			Pattern regexPattern = Pattern.compile(IPADDRESS_PATTERN);
			Matcher regexMatcher = regexPattern.matcher(customerIpAddress);
			if (!regexMatcher.matches()) {
				logger.error("Customer IP address is not valid {} ", customerIpAddress);
				publishToOneStream(fileuploadData, "Customer IP address is not valid");
				throw new RequestValidationException("Customer IP address is not valid");

			}
		}
	}

	private void validateCCID(FileuploadData fileuploadData) {
		String clientCorrelationId = fileuploadData.getClientCorrelationId();
		if (StringUtils.isEmpty(clientCorrelationId)) {
			throw new RequestValidationException("Client Correlation Id is null or empty");
		} else {
			if (clientCorrelationId.length() > CCID_MAX_LENGTH) {
				logger.error("Client Correlation Id is not valid {} ", clientCorrelationId);
				publishToOneStream(fileuploadData, "Length of the Client Correlation Id exceeds 256 chars");
				throw new RequestValidationException("Length of the Client Correlation Id exceeds 256 chars");
			}
		}
	}

	private byte[] convertToByteArray(InputStream is) throws IOException {

		byte[] document = null;
		try {
			document = IOUtils.toByteArray(is);
		} catch (IOException e1) {
			logger.error("Exception occured while converting to byteArray", e1);
			throw new RequestValidationException("error occured while converting the image into byteArray");
		}
		return document;
	}

	private void checkDocumentSize(FileuploadData fileuploadData) {
		byte[] document = fileuploadData.getDocument();
		logger.info("size of the document in bytes {} ", document.length);
		if (null != document && document.length > TEN_MB) {
			logger.error("document size is greater than 10MB", document.length);
			publishToOneStream(fileuploadData, "document size is greater than 10MB");
			throw new RequestValidationException("document size is greater than 10MB");
		}
	}


	private void publishToOneStream(FileuploadData fileuploadData, String message) {
		try {
			long fileSize = 0;
			if (Optional.ofNullable(fileuploadData.getDocument()).isPresent()) {
				fileSize = fileuploadData.getDocument().length;
			}
			CureFileuploadRawPayload cureFileuploadRawPayload = new CureFileuploadRawPayload(
					fileuploadData.getClientCorrelationId(), fileuploadData.getCustomerIpAddress(),
					fileuploadData.getChannelType(), fileuploadData.getProductId(), fileSize, REQUEST_VALIDATION_ERROR_ID,
					message, HttpStatus.SC_BAD_REQUEST);
			fileuploadCureSdpListener.invokeCureSDPStream(cureFileuploadRawPayload);
		} catch (Exception e) {
			logger.error("Exception occured when publishing to onestream", e);
		}
	}
}

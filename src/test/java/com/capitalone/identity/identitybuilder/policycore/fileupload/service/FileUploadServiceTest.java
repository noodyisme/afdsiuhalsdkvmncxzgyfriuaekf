package com.capitalone.identity.identitybuilder.policycore.fileupload.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.FileSystemResource;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.chassis.engine.model.exception.RequestValidationException;
import com.capitalone.identity.identitybuilder.policycore.fileupload.camel.util.FileUploadCamelCacheUtil;
import com.capitalone.identity.identitybuilder.policycore.fileupload.model.FileuploadData;
import com.capitalone.identity.identitybuilder.policycore.fileupload.onestream.FileuploadCureSdpListener;
import com.symantec.scanengine.api.ScanException;

@ExtendWith(MockitoExtension.class)
public class FileUploadServiceTest {

	private static final String ANY_STRING_YOU_WANT = "Any String you want";

	private static final String ENCRYPTED_TEST_DATA = "eyJraWQiOiIxYjdhMDYxYy0wZTA0LTQwZjAtOTM3NS1kMDFhYTFkN2QzMTciLCJhbGciOiJFQ0RILUVTIiwiZW5jIjoiQTI1NkdDTSIsImVwayI6eyJ5IjoiWWo0YUpZYVQ0SjZIUGdDaFJ3TTdXU1I2X3JpYi1FSWJuVl9vLXRmR0VDbFh1T19TNHVUbDlBbURFdVFiNUtkRENvTmdDcmlWZWwwY0JjY19qU0hTMXl3IiwieCI6IkFMTnNOdVRYVF80NVhjQlpzQlVHTjlqZXV5RjZ5MHlCMDk0dkdWdHpjYWo5aHh1MmwyLS13RlNEZnpqUEEwSWtjRm03LTUtNEhLSTk2VElGbUNTQWQwaWUiLCJrdHkiOiJFQyIsImNydiI6IlAtNTIxIn19..iXH98LPjEW-IG_cq.a2wdWgy46sATQNXuNNM5BQ.FgZc6jBKTMeBdL-Km1tWEQ";

	private static final String ENCRYPTED_TEXT = "encryptedText";

	private static final String T2V_IVR_PRODUCT = "t2v-ivr-product";

	private static final String clientCorrelationId = "12345";

	private static final String customerIpAddress = "127.0.0.1";

	private static final String channelType = "Web";

	private static final String xUpstreamEnv = "verification";

	private static final byte[] document = ANY_STRING_YOU_WANT.getBytes();

	private static final byte[] encDocument = ENCRYPTED_TEST_DATA.getBytes();

	@InjectMocks
	private FileUploadService fileUploadService;

	@Mock
	private FileUploadEncryptionKeyService keyService;

	@Mock
	private FileUploadCamelCacheUtil cacheService;

	@Mock
	private FileUploadVirusScanService scanService;

	@Mock
	private ScanException scanException;
	
	@Mock
	private FileuploadCureSdpListener cureSdpListener;

	private FileuploadData fileuploadData;

	private FileuploadData encryptedFileUploadData;

	@BeforeEach
	public void setup() {
		fileuploadData = new FileuploadData(clientCorrelationId,customerIpAddress,channelType, xUpstreamEnv, T2V_IVR_PRODUCT, document);
		encryptedFileUploadData = new FileuploadData(clientCorrelationId,customerIpAddress,channelType, xUpstreamEnv, T2V_IVR_PRODUCT, encDocument);
	}

	@Test
	void testDocumentUpload_happyPath() throws IOException, ScanException {

		byte[] data = ANY_STRING_YOU_WANT.getBytes();
		when(scanService.isVirusAffected(Mockito.any())).thenReturn(false);
		when(keyService.encryptDocument(T2V_IVR_PRODUCT, data)).thenReturn(ENCRYPTED_TEXT);
		assertNotNull(fileUploadService.uploadDocument(fileuploadData,null));

	}

	@Test
	void testDocumentUpload_virusScanFailed() throws IOException, ScanException {
		when(scanService.isVirusAffected(Mockito.any())).thenReturn(true);
		assertThrows(RequestValidationException.class, () -> fileUploadService.uploadDocument(fileuploadData,null));
	}

	@Test
	void testDocumentUpload_virusScanError() throws IOException, ScanException {
		when(scanService.isVirusAffected(Mockito.any())).thenThrow(ChassisSystemException.class);
		assertThrows(ChassisSystemException.class, () -> fileUploadService.uploadDocument(fileuploadData,null));
	}
	
	@Test
	public void testDocumentupload_encryptionServiceThrowsError() {
		when(keyService.encryptDocument(Mockito.any(),Mockito.any())).thenThrow(ChassisSystemException.class);
		assertThrows(ChassisSystemException.class, () -> fileUploadService.uploadDocument(fileuploadData, null));
	}

	@Test
	public void testDocumentupload_VirusScanWithEncryptedPayload() {
		when(scanService.isVirusAffected(Mockito.any())).thenReturn(false);
		assertNotNull(fileUploadService.uploadDocument(encryptedFileUploadData,fileuploadData));
	}

}

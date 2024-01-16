package com.capitalone.identity.identitybuilder.policycore.fileupload.rest.v1;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.identity.identitybuilder.policycore.crypto.COSRequestProperties;
import com.capitalone.identity.identitybuilder.policycore.crypto.DeCryptService;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.capitalone.chassis.engine.model.exception.RequestValidationException;
import com.capitalone.identity.identitybuilder.policycore.fileupload.model.FileUploadResponse;
import com.capitalone.identity.identitybuilder.policycore.fileupload.onestream.FileuploadCureSdpListener;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.FileUploadService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = FileUploadResource.class)
public class FileUploadResourceTest {

	private static final String TEST_DATA = "test data";
	private static final String ENCRYPTED_TEST_DATA = "eyJraWQiOiIxYjdhMDYxYy0wZTA0LTQwZjAtOTM3NS1kMDFhYTFkN2QzMTciLCJhbGciOiJFQ0RILUVTIiwiZW5jIjoiQTI1NkdDTSIsImVwayI6eyJ5IjoiWWo0YUpZYVQ0SjZIUGdDaFJ3TTdXU1I2X3JpYi1FSWJuVl9vLXRmR0VDbFh1T19TNHVUbDlBbURFdVFiNUtkRENvTmdDcmlWZWwwY0JjY19qU0hTMXl3IiwieCI6IkFMTnNOdVRYVF80NVhjQlpzQlVHTjlqZXV5RjZ5MHlCMDk0dkdWdHpjYWo5aHh1MmwyLS13RlNEZnpqUEEwSWtjRm03LTUtNEhLSTk2VElGbUNTQWQwaWUiLCJrdHkiOiJFQyIsImNydiI6IlAtNTIxIn19..iXH98LPjEW-IG_cq.a2wdWgy46sATQNXuNNM5BQ.FgZc6jBKTMeBdL-Km1tWEQ";
	private static final String EMPTY_DATA = "";
	private static final String CLIENT_CORRELATION_ID = "SAMPLE_CLIENT_CORRELATION_ID";
	private static final String CUSTOMER_IP_ADDRESS = "127.0.0.1";
	private static final String PRODUCT_ID = "selfie-product";
	private static final String CHANNEL_TYPE = "Web";
	private static final String X_UPSTREAM_ENV = "verification";
	private static final String BAD_PRODUCT_ID = "bad_id";
	private static final @NotNull String CLIENT_CORRELATION_ID_MAX = "ckhsdfksajfhsdkfjdsfsdbfhksdckhsdfksajfhsdkfjdsfsdbfhksdckhsdfksajfhsdkfjdsfsdbfhksdckhsckhsdfksajfhsdkfjdsfsdbfhksdckhsdfksajfhsdkfjdsfsdbfhksddfksajfhsdkfjdsfsdbfhksddjsvmfsdikjfhdsvjfmasdvhfvhsdkjfdsvjkfdsvjfdsvjfsdvfjsdvjfcsdvjcvdsjhfvdsjfdsvjfdsvjfdsvjfdsvjfhdsvfhjsdvfdhjsjsvhvjsdhvbjdshvjsdhvbjsdhvbjsdhvjsdhvbjsdhvbdsjhjasfhgsvdjmfhvdsjfmgcvdhsyfgkcvhsjdcvdjcdjvdvjfmdvdjssdikvsdvsdjvsdjsdvdsjsdcsdjvisdjvsdijvdsifsdjfbdsjvdisfifisdfid";
	
	public FileUploadResponse fileUploadResponse;
	
	@InjectMocks
	private FileUploadResource fileUploadResource;

	@Mock
	private FileUploadService fileUploadServices;

	@Mock
	private DeCryptService deCryptService;

	@Mock
	private HttpServletRequest httpRequest;

	@Mock
	private HttpServletResponse httpResponse;

	@Mock
	private FormDataContentDisposition formDataContentDisposition;
	
	@Mock
	private FormDataBodyPart body;
	
	@Mock
	private MultiPart multiPart;
	
	@Mock
	private FileuploadCureSdpListener cureSdpListener;

	@Mock
	Environment environment;

	private InputStream unencrypted_Is;

	private InputStream encryptedIs;

	private InputStream emptyIs;

	@BeforeEach
	public void setup() throws IOException {
		fileUploadResponse = FileUploadResponse.builder().fileId("1234").build();
		unencrypted_Is = new ByteArrayInputStream(TEST_DATA.getBytes());
		encryptedIs = new ByteArrayInputStream(ENCRYPTED_TEST_DATA.getBytes());
		emptyIs = new ByteArrayInputStream(EMPTY_DATA.getBytes());
	}

	@Test
	public void test_uploadUnencryptedImage_ccid_null() {
		
		assertThrows(RequestValidationException.class, () -> fileUploadResource.uploadImage(null,
				CUSTOMER_IP_ADDRESS, CHANNEL_TYPE, X_UPSTREAM_ENV, PRODUCT_ID, unencrypted_Is, formDataContentDisposition, body, httpRequest, httpResponse));
	}
	
	@Test
	public void test_uploadUnencryptedImage_ccid_maxlength() {
		
		assertThrows(RequestValidationException.class, () -> fileUploadResource.uploadImage(CLIENT_CORRELATION_ID_MAX,
				CUSTOMER_IP_ADDRESS, CHANNEL_TYPE, X_UPSTREAM_ENV, PRODUCT_ID, unencrypted_Is, formDataContentDisposition, body, httpRequest, httpResponse));
	}
	
	@Test
	public void test_uploadUnencryptedImage_customerIPAddressschannelTypeAndxUpstreamIsnull() throws IOException {

		when(fileUploadServices.uploadDocument(any(),any())).thenReturn(fileUploadResponse);
		assertNotNull(fileUploadResource.uploadImage(CLIENT_CORRELATION_ID, null, null, null, PRODUCT_ID, unencrypted_Is,
				formDataContentDisposition, body, httpRequest, httpResponse));
	}
	
	@Test
	public void test_uploadUnencryptedImage_channelTypeIsNotValid() {
		assertThrows(RequestValidationException.class, () -> fileUploadResource.uploadImage(CLIENT_CORRELATION_ID,
				CUSTOMER_IP_ADDRESS, "abc", X_UPSTREAM_ENV, PRODUCT_ID, unencrypted_Is, formDataContentDisposition, body, httpRequest, httpResponse));
	}
	
	@Test
	public void test_uploadUnencryptedImage_xUpstreamEnvIsNotValid() {
		assertThrows(RequestValidationException.class, () -> fileUploadResource.uploadImage(CLIENT_CORRELATION_ID,
				CUSTOMER_IP_ADDRESS, CHANNEL_TYPE, "abc", PRODUCT_ID, unencrypted_Is, formDataContentDisposition, body, httpRequest, httpResponse));
	}
	
	@Test
	public void test_uploadUnencryptedImage_customerIpAddressIsNotValid() {
		assertThrows(RequestValidationException.class, () -> fileUploadResource.uploadImage(CLIENT_CORRELATION_ID,
				"123", CHANNEL_TYPE, X_UPSTREAM_ENV, PRODUCT_ID, unencrypted_Is, formDataContentDisposition, body, httpRequest, httpResponse));
	}

	@Test
	public void test_uploadImage_null() throws IOException {
		assertThrows(RequestValidationException.class, () -> fileUploadResource.uploadImage(CLIENT_CORRELATION_ID,
				CUSTOMER_IP_ADDRESS, CHANNEL_TYPE, X_UPSTREAM_ENV, PRODUCT_ID, null, formDataContentDisposition, body, httpRequest, httpResponse));
	}
	
	@Test
	public void test_uploadImage_multipleParts() throws IOException {
		List<BodyPart> list = new ArrayList<BodyPart>();
		list.add(new BodyPart());
		list.add(new BodyPart());
		when(body.getParent()).thenReturn(multiPart);
		when(multiPart.getBodyParts()).thenReturn(list);
		assertThrows(RequestValidationException.class, () -> fileUploadResource.uploadImage(CLIENT_CORRELATION_ID,
				CUSTOMER_IP_ADDRESS, CHANNEL_TYPE, X_UPSTREAM_ENV, PRODUCT_ID, null, formDataContentDisposition, body, httpRequest, httpResponse));
	}

	@Test
	public void test_uploadUnencryptedImage() throws IOException {
		when(fileUploadServices.uploadDocument(any(), eq(null))).thenReturn(fileUploadResponse);

		assertNotNull(fileUploadResource.uploadImage(CLIENT_CORRELATION_ID, CUSTOMER_IP_ADDRESS, CHANNEL_TYPE, X_UPSTREAM_ENV, PRODUCT_ID, unencrypted_Is,
				formDataContentDisposition, body, httpRequest, httpResponse));
	}

	@Test
	public void test_uploadImage_image_size_greater_than_10() throws IOException {
		char[] data = new char[100000000];
		String s1 = new String(data);
		assertThrows(RequestValidationException.class,
				() -> fileUploadResource.uploadImage(CLIENT_CORRELATION_ID, CUSTOMER_IP_ADDRESS, CHANNEL_TYPE, X_UPSTREAM_ENV, PRODUCT_ID,
						new ByteArrayInputStream(s1.getBytes()), formDataContentDisposition, body, httpRequest,
						httpResponse));
	}

	@Test
	public void test_upload_invalid_encrypted_file_on_upstream_env() throws IOException {
		ReflectionTestUtils.setField(fileUploadResource, "domainIdentifier", "identitybuilder.verification.domain");
		ReflectionTestUtils.setField(fileUploadResource, "allowListOfProdIds", Arrays.asList("selfie-product"));
		Mockito.when(environment.getProperty("identitybuilder.verification.domain.selfie-product.lockBoxId")).thenReturn("abc123");
		Mockito.when(environment.getProperty("identitybuilder.verification.domain.selfie-product.productId")).thenReturn("selfie-product");
		Mockito.when(environment.getProperty("identitybuilder.verification.domain.selfie-product.vaultRole")).thenReturn("abc123");
		Mockito.when(environment.getProperty("identitybuilder.verification.domain.selfie-product.vaultAddress")).thenReturn("abc123");
		assertThrows(RequestValidationException.class,
				() -> fileUploadResource.uploadImage(CLIENT_CORRELATION_ID, CUSTOMER_IP_ADDRESS, CHANNEL_TYPE, X_UPSTREAM_ENV, PRODUCT_ID, encryptedIs,
						formDataContentDisposition, body, httpRequest, httpResponse));
	}

	@Test
	public void test_upload_invalid_productId_on_upstream_env() throws IOException {
		ReflectionTestUtils.setField(fileUploadResource, "domainIdentifier", "verification.domain");
		ReflectionTestUtils.setField(fileUploadResource, "allowListOfProdIds", Arrays.asList("selfie-product"));
		assertThrows(RequestValidationException.class,
				() -> fileUploadResource.uploadImage(CLIENT_CORRELATION_ID, CUSTOMER_IP_ADDRESS, CHANNEL_TYPE, X_UPSTREAM_ENV, BAD_PRODUCT_ID, encryptedIs,
						formDataContentDisposition, body, httpRequest, httpResponse));
	}
	@Test
	public void test_upload_empty_productid_check() throws IOException {
		ReflectionTestUtils.setField(fileUploadResource, "domainIdentifier", "verification.domain");
		ReflectionTestUtils.setField(fileUploadResource, "allowListOfProdIds", Arrays.asList("selfie-product"));
		assertThrows(RequestValidationException.class,
				() -> fileUploadResource.uploadImage(CLIENT_CORRELATION_ID, CUSTOMER_IP_ADDRESS, CHANNEL_TYPE, X_UPSTREAM_ENV, null, encryptedIs,
						formDataContentDisposition, body, httpRequest, httpResponse));
	}

	@Test
	public void test_upload_empty_image_check() throws IOException {
		when(fileUploadServices.uploadDocument(any(), any())).thenReturn(fileUploadResponse);
		assertNotNull(fileUploadResource.uploadImage(CLIENT_CORRELATION_ID, CUSTOMER_IP_ADDRESS, CHANNEL_TYPE, X_UPSTREAM_ENV, PRODUCT_ID, emptyIs,
				formDataContentDisposition, body, httpRequest, httpResponse));
	}

	@Test
	public void test_upload_image_validation() throws IOException {
		String base64EncodedImage = "R0lGODlhAQABAIABAP///wAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==";
		byte[] stream = Base64.getDecoder().decode(base64EncodedImage);
		when(fileUploadServices.uploadDocument(any(), any())).thenReturn(fileUploadResponse);
		assertNotNull(fileUploadResource.uploadImage(CLIENT_CORRELATION_ID, CUSTOMER_IP_ADDRESS, CHANNEL_TYPE, X_UPSTREAM_ENV, PRODUCT_ID, new ByteArrayInputStream(stream),
				formDataContentDisposition, body, httpRequest, httpResponse));
	}

	@Test
	public void test_upload_decryption_failure() throws IOException {
		ReflectionTestUtils.setField(fileUploadResource, "domainIdentifier", "identitybuilder.verification.domain");
		ReflectionTestUtils.setField(fileUploadResource, "allowListOfProdIds", Arrays.asList("selfie-product"));
		Mockito.when(environment.getProperty("identitybuilder.verification.domain.selfie-product.lockBoxId")).thenReturn("abc123");
		Mockito.when(environment.getProperty("identitybuilder.verification.domain.selfie-product.productId")).thenReturn("selfie-product");
		Mockito.when(environment.getProperty("identitybuilder.verification.domain.selfie-product.vaultRole")).thenReturn("abc123");
		Mockito.when(environment.getProperty("identitybuilder.verification.domain.selfie-product.vaultAddress")).thenReturn("abc123");
		when(deCryptService.decrypt(ENCRYPTED_TEST_DATA, COSRequestProperties.builder().build())).thenReturn(null);
		assertThrows(RequestValidationException.class,
				() -> fileUploadResource.uploadImage(CLIENT_CORRELATION_ID, CUSTOMER_IP_ADDRESS, CHANNEL_TYPE, X_UPSTREAM_ENV, PRODUCT_ID, encryptedIs,
						formDataContentDisposition, body, httpRequest, httpResponse));
	}

	@Test
	public void test_upload_decrypted_image() throws IOException {
		ReflectionTestUtils.setField(fileUploadResource, "domainIdentifier", "identitybuilder.verification.domain");
		ReflectionTestUtils.setField(fileUploadResource, "allowListOfProdIds", Arrays.asList("selfie-product"));
		byte[] stream = ENCRYPTED_TEST_DATA.getBytes(StandardCharsets.UTF_8);
		Mockito.when(environment.getProperty("identitybuilder.verification.domain.selfie-product.lockBoxId")).thenReturn("abc123");
		Mockito.when(environment.getProperty("identitybuilder.verification.domain.selfie-product.productId")).thenReturn("selfie-product");
		Mockito.when(environment.getProperty("identitybuilder.verification.domain.selfie-product.vaultRole")).thenReturn("abc123");
		Mockito.when(environment.getProperty("identitybuilder.verification.domain.selfie-product.vaultAddress")).thenReturn("abc123");
		when(deCryptService.decrypt(eq(ENCRYPTED_TEST_DATA), any())).thenReturn("xyz");
		assertNotNull(fileUploadResource.decryptFileData(stream, PRODUCT_ID, CLIENT_CORRELATION_ID));
	}

	@Test
	public void test_upload_invalid_domain_identifier() throws IOException {
		ReflectionTestUtils.setField(fileUploadResource, "domainIdentifier", "identitybuilder.verification.domain");
		ReflectionTestUtils.setField(fileUploadResource, "allowListOfProdIds", Arrays.asList("selfie-product","bad_id"));
		ChassisSystemException e = assertThrows(ChassisSystemException.class,
				() -> fileUploadResource.uploadImage(CLIENT_CORRELATION_ID, CUSTOMER_IP_ADDRESS, CHANNEL_TYPE, X_UPSTREAM_ENV, BAD_PRODUCT_ID, encryptedIs,
						formDataContentDisposition, body, httpRequest, httpResponse));
		assertEquals("Missing application properties for decryption w/ given productID",e.getMessage());
		assertEquals(500,e.getHttpStatusCode());
	}
}

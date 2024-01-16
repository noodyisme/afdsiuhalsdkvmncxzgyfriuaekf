package com.capitalone.identity.identitybuilder.policycore.fileupload.onestream;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;

import com.capitalone.chassis.engine.dfs.emitter.core.model.StreamMessageContext;
import com.capitalone.chassis.engine.dfs.emitter.core.support.AuditClientHelper;
import com.capitalone.chassis.engine.dfs.emitter.sdp.SdpHttpStreamMessageEntryPoint;
import com.capitalone.chassis.engine.jackson.adapters.ChassisDeserializersModule;
import com.capitalone.chassis.engine.jackson.adapters.deserializer.ReferenceIdDeserializers;
import com.capitalone.chassis.engine.model.audit.EventStreamData;
import com.capitalone.chassis.engine.model.audit.ExceptionData;
import com.capitalone.chassis.engine.secureddatum.EncryptedDatum;
import com.capitalone.chassis.engine.secureddatum.security.DefaultCryptoSerializerDeserializer;

@ExtendWith(MockitoExtension.class)
public class CustomFileuploadCureMapBuilderTest {

	private CustomFileuploadCureMapBuilder customFileuploadCureMapBuilder;

	@Mock
	private ConversionService conversionService;

	@Mock
	private SdpHttpStreamMessageEntryPoint entryPoint;
	
	@Mock
	private ReferenceIdDeserializers referenceIdDeserializers;

	@Mock
	private DefaultCryptoSerializerDeserializer cryptoSerializerDeserializer;

	@Mock
	private @Nullable ExceptionData exceptionData;
	
	@Mock
	private StreamMessageContext streamMessageContext;

	@Mock
	private AuditClientHelper auditClientHelper;
	
	private EventStreamData eventStreamData;

	private ChassisDeserializersModule chassisDeserializersModule;

	Map<String, Object> args = new HashMap<String, Object>();
	
	private Map<String, Object> payload = new HashMap<String, Object>();

	public String businessApplication = "BAIDENTITYVERIFICATIONSERVICES";

	private CureFileuploadRawPayload cureRawPayload;

	@BeforeEach
	public void setup() {
		
		chassisDeserializersModule = new ChassisDeserializersModule(referenceIdDeserializers);
		cryptoSerializerDeserializer = new DefaultCryptoSerializerDeserializer(
				new EncryptedDatum("test", "test", 256, 1));

		cureRawPayload = createRawPayload();
		args.put("cureFileuploadRawPayload", cureRawPayload);
		eventStreamData = new EventStreamData(200, new Integer(200), 0, "cureFileuploadRawPayload", null, args, null);

		customFileuploadCureMapBuilder = new CustomFileuploadCureMapBuilder(businessApplication, conversionService, auditClientHelper,
				chassisDeserializersModule);
	}

	private CureFileuploadRawPayload createRawPayload() {
		cureRawPayload = new CureFileuploadRawPayload();

		cureRawPayload.setFileUploadProcessSuccess(true);
		cureRawPayload.setCureBusinessApplication(businessApplication);
		cureRawPayload.setFileId("123456");
		cureRawPayload.setIdentityChannelType("Web");
		cureRawPayload.setVirusScanResult("clean");
		cureRawPayload.setCureCorrelationId("cure_correlation_id");
		cureRawPayload.setIdentityCustomerIpAddress("127.0.0.1");
		cureRawPayload.setErrorCode(200000);
		cureRawPayload.setErrorDeveloperText("error_developer_text");
		cureRawPayload.setHttpStatusCode(201);
		cureRawPayload.setProductId("product_id");
		
		return cureRawPayload;
	}

	@Test
	public void addDomainFieldstest() {
		customFileuploadCureMapBuilder.addDomainFields(payload, entryPoint, eventStreamData, streamMessageContext,
				exceptionData);
		assertEquals("123456", payload.get("file_id"));
	}
	
	@Test
	public void testGetType() {
		assertEquals("customFileuploadCureMapBuilder",customFileuploadCureMapBuilder.getType());
	}

}

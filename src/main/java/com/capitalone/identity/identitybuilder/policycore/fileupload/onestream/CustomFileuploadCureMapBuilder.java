package com.capitalone.identity.identitybuilder.policycore.fileupload.onestream;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.capitalone.chassis.engine.annotations.logging.Log;
import com.capitalone.chassis.engine.dfs.emitter.core.model.StreamMessageContext;
import com.capitalone.chassis.engine.dfs.emitter.core.support.AuditClientHelper;
import com.capitalone.chassis.engine.dfs.emitter.sdp.SdpHttpStreamMessageEntryPoint;
import com.capitalone.chassis.engine.dfs.emitter.sdp.stream.CureMapEventStreamDataBuilder;
import com.capitalone.chassis.engine.jackson.adapters.ChassisDeserializersModule;
import com.capitalone.chassis.engine.model.audit.EventStreamData;
import com.capitalone.chassis.engine.model.audit.ExceptionData;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.FileUploadService;


@Component
@Log
@ConditionalOnProperty(value = FileUploadService.FILE_UPLOAD_ENABLED_FLAG)
public class CustomFileuploadCureMapBuilder extends CureMapEventStreamDataBuilder {
	
	public static final String FILE_ID = "file_id";
	public static final String PRODUCT_ID = "product_id";
	public static final String VIRUS_SCAN_RESULT = "virus_scan_result";
	public static final String IS_FILE_UPLOAD_PROCESS_SUCCESS = "is_file_upload_process_success";
	public static final String ERROR_CODE = "error_code";
	public static final String HTTP_STATUS_CODE = "http_status_code";
	public static final String ERROR_DEVELOPER_TEXT = "error_developer_text";
	public static final String IDENTITY_PRODUCER_APPLICATION_NAME_VALUE = "fileUpload";
    public static final String IDENTITY_CUSTOMER_IP_ADDRESS = "identity_customer_ip_address";
    public static final String IDENTITY_CHANNEL_TYPE = "identity_channel_type";
    public static final String IDENTITY_PRODUCER_APPLICATION_NAME = "identity_producer_application_name";
    
    private final Logger logger = LogManager.getLogger(getClass());

    public CustomFileuploadCureMapBuilder(@Value("${ba}") String businessApplication, @Qualifier("apiConversionService") ConversionService converter,
                                   AuditClientHelper auditClientHelper, ChassisDeserializersModule chassisDeserializersModule) {
        super(businessApplication, converter, auditClientHelper, chassisDeserializersModule);
    }

    @Override
    protected void addDomainFields(Map<String, Object> payload, SdpHttpStreamMessageEntryPoint entryPoint,
                                    EventStreamData eventStreamData, StreamMessageContext streamMessageContext,
                                    @Nullable ExceptionData exceptionData) {
        Optional<CureFileuploadRawPayload> cureFileuploadRawPayload = resolveArgFromEventStreamData(eventStreamData, "cureFileuploadRawPayload", CureFileuploadRawPayload.class);

        if(cureFileuploadRawPayload.isPresent()) {
            logger.info("Loading Payload Here**********");
            CureFileuploadRawPayload cureRawPayload = cureFileuploadRawPayload.get();
            
            payload.put(IDENTITY_CUSTOMER_IP_ADDRESS, cureRawPayload.getIdentityCustomerIpAddress());
            payload.put(IDENTITY_CHANNEL_TYPE, StringUtils.upperCase(cureRawPayload.getIdentityChannelType()));
            payload.put(IDENTITY_PRODUCER_APPLICATION_NAME, IDENTITY_PRODUCER_APPLICATION_NAME_VALUE);
            payload.put(FILE_ID, cureRawPayload.getFileId());
            payload.put(PRODUCT_ID, cureRawPayload.getProductId());
            payload.put(VIRUS_SCAN_RESULT, cureRawPayload.getVirusScanResult());
            payload.put(IS_FILE_UPLOAD_PROCESS_SUCCESS, cureRawPayload.isFileUploadProcessSuccess());
            payload.put(ERROR_CODE, cureRawPayload.getErrorCode());
            payload.put(ERROR_DEVELOPER_TEXT, cureRawPayload.getErrorDeveloperText());
            payload.put(HTTP_STATUS_CODE, cureRawPayload.getHttpStatusCode());
            
            
            logger.info("Done Loading, OneIngest payload Printing here**********: {}", payload);
        }
    }
    @Override
    public String getType() {
        return "customFileuploadCureMapBuilder";
    }
}

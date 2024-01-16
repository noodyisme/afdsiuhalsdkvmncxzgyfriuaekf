package com.capitalone.identity.identitybuilder.policycore.fileupload.onestream;

import com.capitalone.chassis.engine.annotations.logging.Log;
import com.capitalone.chassis.engine.annotations.logging.Profile;
import com.capitalone.chassis.engine.annotations.stream.EventStream;
import com.capitalone.chassis.engine.core.base.AbstractBaseService;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.FileUploadService;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAudit;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.FileUploadEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Log
@Profile
@ConditionalOnProperty(value = FileUploadService.FILE_UPLOAD_ENABLED_FLAG)
public class FileuploadCureSdpListener extends AbstractBaseService {

	public final String ONESTREAM_DATASET_NAME = "identity_verification_file_upload_process_requested_v1";

	public FileuploadCureSdpListener() {

	}

	@OperationalAudit(eventType = OperationalEventType.FILE_UPLOAD_PROCESS, mapperFactory = FileUploadEventMapper.Factory.class)
	@EventStream(eventName = ONESTREAM_DATASET_NAME, customEventStreams = {
			@EventStream.CustomEventStream(type = EventStream.EventStreamType.AUDIT, emitters = {
					"fileupload" }, eventStreamDataBuilder = "customFileuploadCureMapBuilder") })
	public void invokeCureSDPStream(CureFileuploadRawPayload cureFileuploadRawPayload) {
		logger.debug("#### Streaming fileupload Request using sdpv4audit emitter ");
	}

}
package com.capitalone.identity.identitybuilder.policycore.fileupload.model;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Builder
@Data
public class FileUploadResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	@NonNull
	private String fileId;

}

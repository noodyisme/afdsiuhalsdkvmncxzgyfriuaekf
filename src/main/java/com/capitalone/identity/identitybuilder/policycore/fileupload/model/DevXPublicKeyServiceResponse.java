package com.capitalone.identity.identitybuilder.policycore.fileupload.model;

import java.util.List;

import com.capitalone.identity.identitybuilder.policycore.service.util.JWTKey;

import lombok.Data;

@Data
public class DevXPublicKeyServiceResponse {

	 private List<JWTKey> keys;

}

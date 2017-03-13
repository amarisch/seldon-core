package io.seldon.clustermanager.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.protobuf.InvalidProtocolBufferException;

import io.seldon.clustermanager.component.ClusterManager;
import io.seldon.clustermanager.pb.ProtoBufUtils;
import io.seldon.protos.DeploymentProtos.CMResultDef;
import io.seldon.protos.DeploymentProtos.CMStatusDef;
import io.seldon.protos.DeploymentProtos.DockerRegistrySecretDef;

@RestController
public class DockerRegistrySecretsController {

    private final static Logger logger = LoggerFactory.getLogger(DockerRegistrySecretsController.class);

    @Autowired
    private ClusterManager clusterManager;

    @RequestMapping(value = "/docker-registry-secrets", method = RequestMethod.POST, consumes = "application/json; charset=utf-8", produces = "application/json; charset=utf-8")
    public ResponseEntity<String> dockerRegistrySecrets_post(RequestEntity<String> requestEntity) {

        String json = requestEntity.getBody();
        logger.debug(String.format("[%s] [%s] [%s]", "POST", requestEntity.getUrl().getPath(), json));

        CMResultDef cmResultDef = null;
        try {
            DockerRegistrySecretDef.Builder dockerRegistrySecretDefBuilder = DockerRegistrySecretDef.newBuilder();
            ProtoBufUtils.updateMessageBuilderFromJson(dockerRegistrySecretDefBuilder, json);
            cmResultDef = clusterManager.createOrReplaceDockerRegistrySecret(dockerRegistrySecretDefBuilder.build());
        } catch (InvalidProtocolBufferException e) {
            String info = org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e);
            //@formatter:off
            cmResultDef = CMResultDef.newBuilder()
                    .setCmstatus(CMStatusDef.newBuilder()
                            .setCode(500)
                            .setStatus(CMStatusDef.Status.FAILURE)
                            .setInfo(info))
                    .clearOneofData()
                    .build();
            //@formatter:on
        }

        return cmResultDefToResponseEntity(cmResultDef);
    }

    @RequestMapping(value = "/docker-registry-secrets/{name}", method = RequestMethod.DELETE, consumes = "application/json; charset=utf-8", produces = "application/json; charset=utf-8")
    public ResponseEntity<String> deployments_delete(@PathVariable("name") String name, RequestEntity<String> requestEntity) {

        logger.debug(String.format("[%s] [%s]", "DELETE", requestEntity.getUrl().getPath()));

        CMResultDef cmResultDef = clusterManager.deleteDockerRegistrySecret(name);

        return cmResultDefToResponseEntity(cmResultDef);
    }

    private static ResponseEntity<String> cmResultDefToResponseEntity(CMResultDef cmResultDef) {

        HttpStatus httpStatus = HttpStatus.valueOf(cmResultDef.getCmstatus().getCode());
        String json = null;
        try {
            json = ProtoBufUtils.toJson(cmResultDef);
        } catch (InvalidProtocolBufferException e) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            json = "Error writing json";
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(json, responseHeaders, httpStatus);

        return responseEntity;
    }
}
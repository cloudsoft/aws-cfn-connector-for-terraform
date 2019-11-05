package io.cloudsoft.terraform.template;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;

public abstract class TerraformBaseHandler<T> extends BaseHandler<T> {

    private static final String PREFIX = "cfn/terraform";
    private AWSSimpleSystemsManagement awsSimpleSystemsManagement;

    public TerraformBaseHandler() {
        awsSimpleSystemsManagement = AWSSimpleSystemsManagementClientBuilder.defaultClient();
    }

    protected String getHost() {
        return getParameterValue("ssh-host");
    }

    protected String getPort() {
        return getParameterValue("ssh-port");
    }

    protected String getUsername() {
        return getParameterValue("ssh-username");
    }

    protected String getSSHKey() {
        return getParameterValue("ssh-key");
    }

    protected String getFingerprint() {
        return getParameterValue("ssh-fingerprint");
    }

    private String getParameterValue(String id) {
        GetParameterResult getParameterResult = awsSimpleSystemsManagement.getParameter(new GetParameterRequest()
                .withName(String.format("%s/%s", PREFIX, id))
                .withWithDecryption(true));

        return getParameterResult.getParameter().getValue();
    }
}

package io.cloudsoft.terraform.template;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;

import java.io.Console;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CreateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        ResourceModel model = request.getDesiredResourceState();
        String s1, s2;
        OperationStatus ret = OperationStatus.PENDING;

        try {
            final SSHClient ssh = new SSHClient();
            // FIXME: keep the fingerprint(s) in an external state instead of hardcoding
            ssh.loadKnownHosts(); // has no effect even when run on the dev PC
            ssh.addHostKeyVerifier("3a:87:1f:a6:d8:6a:32:b7:47:fe:2d:e1:16:3a:bc:38");
            ssh.connect("localhost");
            Session session = null;
            try {
                ssh.authPublickey("denis", "src/main/resources/id_rsa_java");
                // ssh.authPublickey("denis", "/home/denis/.ssh/id_rsa"); // does not work (agent?)
                session = ssh.startSession();
                final Command cmd = session.exec("uname -a");
                s1 = IOUtils.readFully(cmd.getInputStream()).toString();
                cmd.join(5, TimeUnit.SECONDS);
                s2 = "exit status: " + cmd.getExitStatus(); // TBD
                // cmd.getErrorStream() // TDB
                //model.setMetricValue(s1); // does not compile
                System.out.println(s1);
            } finally {
                try {
                    if (session != null) {
                        session.close();
                    }
                } catch (IOException e) {
                    // do nothing
                }

                ssh.disconnect();
            }
            ret = OperationStatus.SUCCESS;

        } catch (IOException e) {
            ret = OperationStatus.FAILED;
        }
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(ret)
                .build();
    }
}

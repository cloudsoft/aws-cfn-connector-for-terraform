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
        OperationStatus ret = OperationStatus.PENDING;

        try {
            final SSHClient ssh = new SSHClient();

            // FIXME: keep the fingerprint(s) in an external state instead of hardcoding
            ssh.loadKnownHosts(); // has no effect even when run on the dev PC
            ssh.addHostKeyVerifier("3a:87:1f:a6:d8:6a:32:b7:47:fe:2d:e1:16:3a:bc:38");

            ssh.connect("localhost");
            Session session = null;
            try {
                // src/main/resources/privkey works.
                // /home/user/.ssh/privkey works if the file has no passphrase (sshj does
                // not support SSH agent, and there is no SSH agent in AWS anyway).
                // ~/.ssh/privkey does not work.
                // ~user/.ssh/privkey does not work.
                ssh.authPublickey("denis", "src/main/resources/id_rsa_java");

                session = ssh.startSession();
                final Command cmd = session.exec("figlet 'Here be Terraform'");
                String s1 = IOUtils.readFully(cmd.getInputStream()).toString();
                cmd.join(5, TimeUnit.SECONDS);
                String s2 = "exit status: " + cmd.getExitStatus(); // TBD
                // cmd.getErrorStream() // TBD
                //model.setMetricValue(s1); // does not compile
                System.out.println(s1);
                System.out.println(s2);
                ret = OperationStatus.SUCCESS;
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
        } catch (IOException e) {
            ret = OperationStatus.FAILED;
        }
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(ret)
                .build();
    }
}

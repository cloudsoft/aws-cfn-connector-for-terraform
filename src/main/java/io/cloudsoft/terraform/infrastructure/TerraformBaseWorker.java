package io.cloudsoft.terraform.infrastructure;

import com.google.common.base.Preconditions;
import io.cloudsoft.terraform.infrastructure.commands.RemoteSystemdUnit;
import io.cloudsoft.terraform.infrastructure.commands.TerraformSshCommands;
import lombok.Getter;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.cloudformation.proxy.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class TerraformBaseWorker<Steps extends Enum<?>> {

    // Mirror Terraform, which maxes its state checks at 10 seconds when working on long jobs
    private static final int MAX_CHECK_INTERVAL_SECONDS = 10;
    // Use YAML doc separator to separate logged messages
    public static final CharSequence LOG_MESSAGE_SEPARATOR = "---";

    @Getter
    protected AmazonWebServicesClientProxy proxy;
    @Getter
    protected ResourceHandlerRequest<ResourceModel> request;
    @Getter
    protected ResourceModel model;
    @Getter
    protected CallbackContext callbackContext;
    @Getter
    private Logger logger;

    protected TerraformParameters parameters;

    protected Steps currentStep;

    // === init and accessors ========================

    protected void init(
            @Nullable AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            @Nullable CallbackContext callbackContext,
            Logger logger) {
        if (this.request!=null) {
            throw new IllegalStateException("Handler can only be setup and used once, and request has already been initialized when attempting to re-initialize it");
        }
        this.proxy = proxy;
        this.request = Preconditions.checkNotNull(request, "request");
        this.model = request.getDesiredResourceState();
        this.callbackContext = callbackContext!=null ? callbackContext : new CallbackContext();
        this.logger = Preconditions.checkNotNull(logger, "logger");
    }

    public synchronized TerraformParameters getParameters() {
        if (parameters==null) {
            if (proxy==null) {
                throw new IllegalStateException("Parameters cannot be accessed before proxy set during init");
            }
            parameters = new TerraformParameters(proxy);
        }
        return parameters;
    }

    // for testing
    public synchronized void setParameters(TerraformParameters parameters) {
        if (this.parameters!=null) {
            throw new IllegalStateException("Handler can only be setup and used once, and parameters have already initialized when attempting to re-initializee them");
        }
        this.parameters = Preconditions.checkNotNull(parameters, "parameters");
    }

    // === lifecycle ========================

    public final ProgressEvent<ResourceModel, CallbackContext> runHandlingError() {
        try {
            logger.log(getClass().getName() + " lambda starting, model: "+model+", callback: "+callbackContext);
            ProgressEvent<ResourceModel, CallbackContext> result = runStep();
            logger.log(getClass().getName() + " lambda exiting, status: "+result.getStatus()+", callback: "+result.getCallbackContext()+", message: "+result.getMessage());
            return result;

        } catch (ConnectorHandlerFailures.Handled e) {
            logger.log(getClass().getName() + " lambda exiting with error");
            return statusFailed("FAILING: "+e.getMessage());

        } catch (ConnectorHandlerFailures.Unhandled e) {
            if (e.getCause()!=null) {
                logException("FAILING: "+e.getMessage(), e.getCause());
            } else {
                log("FAILING: "+e.getMessage());
            }
            logger.log(getClass().getName() + " lambda exiting with error");
            return statusFailed(e.getMessage());

        } catch (Exception e) {
            logException("FAILING: "+e, e);
            logger.log(getClass().getName() + " lambda exiting with error");
            return statusFailed((currentStep!=null ? currentStep+": " : "")+e);
        }
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> runStep() throws IOException;

    // === utils ========================

    protected void log(String message) {
        System.out.println(message);
        System.out.println(LOG_MESSAGE_SEPARATOR);
        if (logger!=null) {
            logger.log(message);
        }
    }

    protected final void logException(String message, Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        log(message + "\n" + sw.toString());
    }


    protected ProgressEvent<ResourceModel, CallbackContext> statusFailed(String message) {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.FAILED)
                .message(message)
                .build();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> statusSuccess() {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> statusInProgress() {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .callbackContext(callbackContext)
                .callbackDelaySeconds(nextDelay(callbackContext))
                .status(OperationStatus.IN_PROGRESS)
                .message(currentStep == null ? null : "Step: " + currentStep)
                .build();
    }

    int nextDelay(CallbackContext callbackContext) {
        if (callbackContext.lastDelaySeconds < 0) {
            callbackContext.lastDelaySeconds = 0;
        } else if (callbackContext.lastDelaySeconds == 0) {
            callbackContext.lastDelaySeconds = 1;
        } else if (callbackContext.lastDelaySeconds < MAX_CHECK_INTERVAL_SECONDS) {
            // exponential backoff
            callbackContext.lastDelaySeconds =
                    Math.min(MAX_CHECK_INTERVAL_SECONDS, 2 * callbackContext.lastDelaySeconds);
        }
        return callbackContext.lastDelaySeconds;
    }

    protected final void advanceTo(Steps nextStep) {
        logger.log("Entering step "+nextStep);
        callbackContext.stepId = nextStep.toString();
        callbackContext.lastDelaySeconds = -1;
    }

    protected final TerraformSshCommands tfSshCommands() {
        return TerraformSshCommands.of(this);
    }

    protected RemoteSystemdUnit tfInit() {
        return RemoteSystemdUnit.of(this, "terraform-init");
    }

    protected RemoteSystemdUnit tfApply() {
        return RemoteSystemdUnit.of(this, "terraform-apply");
    }

    protected RemoteSystemdUnit tfDestroy() {
        return RemoteSystemdUnit.of(this, "terraform-destroy");
    }

    private void drainPendingRemoteLogs(RemoteSystemdUnit process) throws IOException {
        String str;
        str = process.getIncrementalStdout();
        if (!str.isEmpty())
            logger.log("New standard output data:\n" + str);
        str = process.getIncrementalStderr();
        if (!str.isEmpty())
            logger.log("New standard error data:\n" + str);
    }

    protected boolean checkStillRunningOrError(RemoteSystemdUnit process) throws IOException {
        // Always drain pending log messages regardless of any other activity/conditions.
        // That said, do not drain _before_ establishing whether the remote process is still
        // running as that would be a race against short-lived processes and would require a
        // second drain in case the process has finished and would result in a short Terraform
        // log split across two CloudWatch messages for no obvious reason.
        final boolean isRunning = process.isRunning();
        drainPendingRemoteLogs(process);
        if (isRunning) {
            return true;
        }

        final String stdout = process.getFullStdout();
        final String stderr = process.getFullStderr();

        final String s3BucketName = getParameters().getLogsS3BucketName();
        if (s3BucketName != null) {
            // Implies the string is not empty (SSM does not allow that for parameter values).
            final String prefix = model.getIdentifier() + "/" + process.getUnitName();
            uploadFileToS3(s3BucketName, prefix + "-stdout.txt", stdout);
            uploadFileToS3(s3BucketName, prefix + "-stderr.txt", stderr);
        }

        // FIXME: instead of retrieving the full log files it would be faster to accumulate the
        //  incremental fragments already retrieved above.
        if (!process.wasFailure()) {
            if (!stderr.isEmpty()) {
                // Any stderr output is not the wanted result because usually it is a side
                // effect of the remote process' failure, but combined with a non-raised fault
                // flag it may mean a bug (a failure to fail) in Terraform or in the resource
                // provider code, hence report this separately to make it easier to relate.
                logger.log("Spurious remote stderr:\n" + stderr);
            }
        } else {
            String message = "Error in " + process.getUnitName()+": result "+process.getResult()+" ("+
                process.getMainExitCode()+")";
            logger.log(message);
            logger.log(stderr.isEmpty() ? "(Remote stderr is empty.)" : "Remote stderr:\n" + stderr);
            logger.log(stdout.isEmpty() ? "(Remote stdout is empty.)" : "Remote stdout:\n" + stdout);
            throw ConnectorHandlerFailures.handled(message+"; see logs for more detail.");
        }
        return false;
    }

    // This call actually consists of two network transfers, hence for large files is more
    // likely to time out. However, splitting it into two FSM states would require some place
    // to keep the downloaded file. The callback context isn't intended for that, neither is
    // the lambda's runtime filesystem.
    // There would be one more transfer if the CloudFormation template defines any Terraform
    // variables, so the above note would apply even more.
    protected final void getAndUploadConfiguration() throws IOException {
        tfSshCommands().uploadConfiguration(getParameters().getConfiguration(model), model.getVariables());
    }

    private void uploadFileToS3(String bucketName, String objectKey, String text) {
        S3Client s3Client = S3Client.create();
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType("text/plain")
                .build();
        try {
            proxy.injectCredentialsAndInvokeV2(putReq, request -> s3Client.putObject(request, RequestBody.fromString(text)));
            logger.log(String.format("Uploaded a file to s3://%s/%s", bucketName, objectKey));
        } catch (Exception e) {
            logger.log(String.format("Failed to put log file %s into S3 bucket %s: %s (%s)", objectKey, bucketName, e.getClass().getName(), e.getMessage()));
        }
    }
}

package com.amazonaws.logs.metricfilter;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.MetricTransformation;
import com.amazonaws.services.logs.model.PutMetricFilterRequest;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreateHandler extends BaseHandler<CallbackContext> {

    private List<MetricTransformation> getMetricTransformations(final ResourceModel model) {
        List<MetricTransformationProperty> metricTransformations =
                model.getMetricTransformations();
        if (metricTransformations == null) {
            return Collections.emptyList();
        }
        return metricTransformations.stream()
                .map(mtp -> new MetricTransformation()
                        .withMetricName(mtp.getMetricName())
                        .withMetricValue(mtp.getMetricValue())
                        .withMetricNamespace(mtp.getMetricNamespace())
                        .withDefaultValue(mtp.getDefaultValue()))
                .collect(Collectors.toList());
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        ResourceModel model = request.getDesiredResourceState();

        // Convert model to a form that works with your API
        PutMetricFilterRequest putMetricFilterRequest = new PutMetricFilterRequest()
                .withLogGroupName(model.getLogGroupName())
                .withFilterPattern(model.getFilterPattern())
                .withMetricTransformations(getMetricTransformations(model))
                .withFilterName(model.getFilterName());

        // Initialize client and send request
        AWSLogs client = AWSLogsClientBuilder.standard().withRegion("eu-central-1").build();
        // This is a minimal example, so we've kept it easy with no error handling
        // You should add more error handling in real-world handlers
        proxy.injectCredentialsAndInvoke(putMetricFilterRequest,
                client::putMetricFilter);
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}

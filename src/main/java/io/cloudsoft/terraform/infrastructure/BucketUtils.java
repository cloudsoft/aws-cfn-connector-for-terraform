package io.cloudsoft.terraform.infrastructure;

import org.bouncycastle.util.io.Streams;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;

public class BucketUtils {

    private AmazonWebServicesClientProxy proxy;
    private S3Client s3Client;

    public BucketUtils(AmazonWebServicesClientProxy proxy) {
        this(proxy, S3Client.create());
    }
    public BucketUtils(AmazonWebServicesClientProxy proxy, S3Client s3Client) {
        this.proxy = proxy;
        this.s3Client = s3Client;
    }
    
    public void createBucket(String bucketName) {
        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
            .bucket(bucketName)
            .build();
        proxy.injectCredentialsAndInvokeV2(createBucketRequest, request -> s3Client.createBucket(request));
    }
    
    public void deleteBucket(String bucketName) {
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
            .bucket(bucketName)
            .build();
        proxy.injectCredentialsAndInvokeV2(deleteBucketRequest, request -> s3Client.deleteBucket(request));
    }
    
    public byte[] download(final String bucket, final String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        byte result[][] = { new byte[0] };
        proxy.injectCredentialsAndInvokeV2(getObjectRequest, request -> s3Client.getObject(request, 
            (response, stream) -> {
                result[0] = Streams.readAll(stream);
                return response;
            }));
        return result[0];
    }

    public void upload(String bucketName, String objectKey, RequestBody contents, String mimeType) {
        final PutObjectRequest putReq = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(objectKey)
            .contentType(mimeType)
            .build();
        proxy.injectCredentialsAndInvokeV2(putReq, request -> s3Client.putObject(request, contents));
    }
    
}

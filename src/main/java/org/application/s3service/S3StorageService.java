package org.application.s3service;

import org.application.config.R2Properties;
import org.application.dto.PartSummary;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;


import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final R2Properties r2Properties;


    public S3StorageService(R2Properties r2Properties){
        this.r2Properties = r2Properties;
        String endPoint = (r2Properties.endpoint() != null && !r2Properties.endpoint().isBlank())
                ? r2Properties.endpoint()
                : String.format("https://%s.r2.cloudflarestorage.com",r2Properties.accountId());

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endPoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2Properties.accessKey(),r2Properties.secretKey())
                ))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .chunkedEncodingEnabled(false).build())
                .build();

        this.s3Presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endPoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2Properties.accessKey(),r2Properties.secretKey())))
                .region(Region.of("auto"))
                .build();

    }

    public String startMultipartUpload(String key){


        CreateMultipartUploadRequest request = CreateMultipartUploadRequest
                .builder()
                .bucket(r2Properties.bucketName())
                .key(key)
                .build();
        return s3Client.createMultipartUpload(request).uploadId();

    }

    public String generatePreSignedUrlForPart(String key,String uploadId,int partNumber){
        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket(r2Properties.bucketName())
                .key(key)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .build();

        UploadPartPresignRequest partPresignRequest = UploadPartPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .uploadPartRequest(uploadPartRequest)
                .build();

        PresignedUploadPartRequest preSignedRequest = s3Presigner.presignUploadPart(partPresignRequest);
        return preSignedRequest.url().toString();
    }


    public void completeMultipartUpload(String key,String uploadId,List<PartSummary> parts){
        List<CompletedPart> completedParts = parts.stream()
                .map(p -> CompletedPart.builder().partNumber(p.partNumber()).eTag(p.eTag()).build())
                .collect(Collectors.toList());

        CompletedMultipartUpload completedUpload = CompletedMultipartUpload.builder().parts(completedParts).build();

        CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
                .bucket(r2Properties.bucketName())
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(completedUpload)
                .build();

        s3Client.completeMultipartUpload(request);
    }

    public List<PartSummary> listParts(String key,String uploadId){
        List<PartSummary> allParts = new ArrayList<>();
        Integer partNumberMarker = null;
        boolean truncated = true;

        while(truncated){
            ListPartsRequest.Builder builder = ListPartsRequest.builder()
                    .bucket(r2Properties.bucketName())
                    .key(key)
                    .uploadId(uploadId);
            if(partNumberMarker != null){
                builder.partNumberMarker(Integer.valueOf(String.valueOf(partNumberMarker)));
            }
            ListPartsResponse response = s3Client.listParts(builder.build());
            response.parts().forEach(p->
                    allParts.add(new PartSummary(p.partNumber(),p.eTag(),p.size())));
            truncated = response.isTruncated();
            partNumberMarker = response.nextPartNumberMarker();
        }
        return allParts;
    }

    public String generatePresignedGetUrl(String key){
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(r2Properties.bucketName())
                .key(key)
                .build();
        GetObjectPresignRequest presignRequest= GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public void deleteObject(String key){
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(r2Properties.bucketName())
                .key(key)
                .build();
        s3Client.deleteObject(request);
    }


    public void abortMultipartUpload(String key, String uploadId){
        AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                .bucket(r2Properties.bucketName())
                .key(key)
                .uploadId(uploadId)
                .build();
        s3Client.abortMultipartUpload(request);
    }

}

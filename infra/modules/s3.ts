import * as aws from "@pulumi/aws";
import { config } from "../config";

export interface S3Result {
    deploymentBucket: aws.s3.BucketV2;
    appAssetsBucket: aws.s3.BucketV2;
}

// 기존에 남아있는 버킷이 있을 수 있으므로 protect:true로 실수 삭제를 방지한다.
// 버킷이 이미 AWS에 존재하면 `pulumi up` 전에
//   pulumi import aws:s3/bucketV2:BucketV2 deploymentBucket <bucket-name>
//   pulumi import aws:s3/bucketV2:BucketV2 appAssetsBucket <bucket-name>
// 로 state에 흡수한다 (infra/README.md 참고). 존재하지 않으면 그대로 pulumi up이 신규 생성한다.
export function createS3Buckets(): S3Result {
    const deploymentBucket = new aws.s3.BucketV2(
        "deploymentBucket",
        {
            bucket: config.deploymentBucketName,
            tags: { Name: config.deploymentBucketName },
        },
        { protect: true },
    );

    const appAssetsBucket = new aws.s3.BucketV2(
        "appAssetsBucket",
        {
            bucket: config.appAssetsBucketName,
            tags: { Name: config.appAssetsBucketName },
        },
        { protect: true },
    );

    return { deploymentBucket, appAssetsBucket };
}

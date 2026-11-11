import * as aws from "@pulumi/aws";
import { config } from "../config";

// 배포 아티팩트 버킷/앱 자산 버킷은 스토리지 비용만 발생하는 저비용 리소스로, 운영
// 사이클과 무관하게 상시 보존한다. 더 이상 Pulumi가 생성/삭제를 관리하지 않고
// 데이터 소스로만 참조한다 (infra/README.md 참고).
export function createS3Buckets() {
    const deploymentBucket = aws.s3.getBucketOutput({ bucket: config.deploymentBucketName });
    const appAssetsBucket = aws.s3.getBucketOutput({ bucket: config.appAssetsBucketName });

    return { deploymentBucket, appAssetsBucket };
}

export type S3Result = ReturnType<typeof createS3Buckets>;

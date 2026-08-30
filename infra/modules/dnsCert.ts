import * as aws from "@pulumi/aws";
import * as pulumi from "@pulumi/pulumi";
import { config, fqdn } from "../config";

export interface DnsCertResult {
    zone: pulumi.Output<aws.route53.GetZoneResult>;
    certificate: aws.acm.Certificate;
    certificateValidation: aws.acm.CertificateValidation;
}

// hellogsm.kr Hosted Zone은 AWS에 이미 존재하므로 데이터 소스로만 조회하고,
// Pulumi로 새로 생성하지 않는다.
export function createDnsAndCertificate(): DnsCertResult {
    const zone = aws.route53.getZoneOutput({ name: config.domainName });

    const certificate = new aws.acm.Certificate("api-prod-cert", {
        domainName: fqdn,
        validationMethod: "DNS",
        tags: { Name: fqdn },
    });

    const validationRecord = new aws.route53.Record("api-prod-cert-validation", {
        zoneId: zone.zoneId,
        name: certificate.domainValidationOptions[0].resourceRecordName,
        type: certificate.domainValidationOptions[0].resourceRecordType,
        records: [certificate.domainValidationOptions[0].resourceRecordValue],
        ttl: 60,
        allowOverwrite: true,
    });

    const certificateValidation = new aws.acm.CertificateValidation("api-prod-cert-validation", {
        certificateArn: certificate.arn,
        validationRecordFqdns: [validationRecord.fqdn],
    });

    return { zone, certificate, certificateValidation };
}

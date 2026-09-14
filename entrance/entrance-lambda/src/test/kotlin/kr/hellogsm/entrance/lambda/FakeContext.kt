package kr.hellogsm.entrance.lambda

import com.amazonaws.services.lambda.runtime.ClientContext
import com.amazonaws.services.lambda.runtime.CognitoIdentity
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger

/** 테스트 전용 최소 구현 — 로그는 표준출력으로 흘려보내기만 한다. */
class FakeContext : Context {
    override fun getAwsRequestId() = "test-request-id"
    override fun getLogGroupName() = "test-log-group"
    override fun getLogStreamName() = "test-log-stream"
    override fun getFunctionName() = "entrance-lambda-test"
    override fun getFunctionVersion() = "TEST"
    override fun getInvokedFunctionArn() = "arn:aws:lambda:test"
    override fun getIdentity(): CognitoIdentity? = null
    override fun getClientContext(): ClientContext? = null
    override fun getRemainingTimeInMillis() = 30_000
    override fun getMemoryLimitInMB() = 512
    override fun getLogger(): LambdaLogger = object : LambdaLogger {
        override fun log(message: String) {}
        override fun log(message: ByteArray) {}
    }
}

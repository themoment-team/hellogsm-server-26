package team.themoment.hellogsmv3.global.thirdParty.feign.client.lambda;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import team.themoment.hellogsmv3.domain.oneseo.dto.response.CalculatedScoreResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.request.LambdaScoreCalculatorReqDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.config.LambdaScoreCalculatorFeignConfig;

@FeignClient(name = "lambda-score-calculator-client", url = "${lambda-score-calculator.url}", configuration = LambdaScoreCalculatorFeignConfig.class)
public interface LambdaScoreCalculatorClient {
    @PostMapping
    CalculatedScoreResDto calculateScore(@RequestBody LambdaScoreCalculatorReqDto reqDto);
}

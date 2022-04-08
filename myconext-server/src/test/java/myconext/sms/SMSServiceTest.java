package myconext.sms;

import myconext.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

class SMSServiceTest {

    @RegisterExtension
    WireMockExtension mockServer = new WireMockExtension(8381);

    private SMSService smsService = new SMSServiceImpl("http://localhost:8381/sms", "secret");

    @Test
    void send() {
        stubFor(post(urlPathMatching("/sms")).willReturn(aResponse()
                .withStatus(200)));
        smsService.send("31639115008", "123456");
    }
}
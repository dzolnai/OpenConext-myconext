package myconext;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import myconext.api.MagicLinkResponse;
import myconext.model.*;
import myconext.repository.AuthenticationRequestRepository;
import myconext.repository.ChangeEmailHashRepository;
import myconext.repository.PasswordForgottenHashRepository;
import myconext.repository.UserRepository;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Override the @ActiveProfiles annotation if you don't want to have mock SAML authentication
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "mongodb_db=surf_id_test",
                "cron.node-cron-job-responsible=false",
                "email_guessing_sleep_millis=1",
                "sp_entity_id=https://engine.test.surfconext.nl/authentication/sp/metadata",
                "sp_entity_metadata_url=https://engine.test.surfconext.nl/authentication/sp/metadata",
                "spring.main.lazy-initialization=true",
                "cron.service-name-resolver-initial-delay-milliseconds=60000"
        })
@ActiveProfiles({"test"})
@SuppressWarnings("unchecked")
public abstract class AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected MongoTemplate mongoTemplate;

    @Qualifier("jsonMapper")
    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected AuthenticationRequestRepository authenticationRequestRepository;

    @Autowired
    protected PasswordForgottenHashRepository passwordForgottenHashRepository;

    @Autowired
    protected ChangeEmailHashRepository changeEmailHashRepository;

    private final SimpleDateFormat issueFormat = new SimpleDateFormat("yyyy-MM-dd'T'H:mm:ss");

    protected final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Before
    public void before() throws Exception {
        RestAssured.port = port;
        Arrays.asList(SamlAuthenticationRequest.class, User.class)
                .forEach(clazz -> mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, clazz)
                        .remove(new Query())
                        .insert(readFromFile(clazz))
                        .execute());
        Arrays.asList(PasswordForgottenHash.class, Challenge.class)
                .forEach(clazz -> mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, clazz)
                        .remove(new Query())
                        .execute());
    }

    protected String samlAuthnRequest() throws IOException {
        return samlAuthnRequest(null);
    }

    protected String samlAuthnRequest(String relayState) throws IOException {
        Response response = samlAuthnRequestResponse(null, relayState);
        return extractAuthenticationRequestIdFromAuthnResponse(response);
    }

    protected String extractAuthenticationRequestIdFromAuthnResponse(Response response) {
        assertEquals(302, response.getStatusCode());

        String location = response.getHeader("Location");
        assertTrue(location.startsWith("http://localhost:3000/login/"));

        return location.substring(location.lastIndexOf("/") + 1, location.lastIndexOf("?"));
    }

    protected Response samlAuthnRequestResponse(Cookie cookie, String relayState) throws IOException {
        return samlAuthnRequestResponseWithLoa(cookie, relayState, "");
    }

    protected MagicLinkResponse magicLinkRequest(User user, HttpMethod method) throws IOException {
        String authenticationRequestId = samlAuthnRequest();
        return magicLinkRequest(new MagicLinkRequest(authenticationRequestId, user, false, StringUtils.hasText(user.getPassword())), method);
    }

    protected MagicLinkResponse magicLinkRequest(MagicLinkRequest linkRequest, HttpMethod method) {
        RequestSpecification requestSpecification = given()
                .when()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(linkRequest);

        String path = "/myconext/api/idp/magic_link_request";
        Response response = method.equals(HttpMethod.POST) ? requestSpecification.post(path) : requestSpecification.put(path);
        return new MagicLinkResponse(linkRequest.getAuthenticationRequestId(), response.then());
    }

    protected Response samlAuthnRequestResponseWithLoa(Cookie cookie, String relayState, String loaLevel) throws IOException {
        String samlRequestTemplate = readFile("authn_request.xml");
        String samlRequest = String.format(samlRequestTemplate, UUID.randomUUID().toString(), issueFormat.format(new Date()), loaLevel);
        String samlRequestEncoded = deflatedBase64encoded(samlRequest);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("SAMLRequest", samlRequestEncoded);
        if (StringUtils.hasText(relayState)) {
            queryParams.put("RelayState", relayState);
        }
        return given().redirects().follow(false)
                .when()
                .queryParams(queryParams)
                .cookie(cookie != null ? cookie : new Cookie.Builder("dummy", "dummy").build())
                .get("/saml/guest-idp/SSO");
    }

    protected String readFile(String path) throws IOException {
        return IOUtils.toString(new ClassPathResource(path).getInputStream(), Charset.defaultCharset());
    }

    private String deflatedBase64encoded(String input) throws IOException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.DEFLATED, true);
        DeflaterOutputStream deflaterStream = new DeflaterOutputStream(bytesOut, deflater);
        deflaterStream.write(input.getBytes(Charset.defaultCharset()));
        deflaterStream.finish();
        return new String(Base64.encodeBase64(bytesOut.toByteArray()));
    }

    private <T> List<T> readFromFile(Class<T> clazz) {
        try {
            String name = AnnotationUtils.findAnnotation(clazz, Document.class).collection() + ".json";
            return objectMapper.readValue(new ClassPathResource(name).getInputStream(), new TypeReference<List<T>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> void doExpireWithFindProperty(Class<T> clazz, String property, String value) {
        T t = mongoTemplate.findOne(Query.query(Criteria.where(property).is(value)), clazz);
        Date expiresIn = Date.from(LocalDateTime.now().minusYears(10L).atZone(ZoneId.systemDefault()).toInstant());
        ReflectionTestUtils.setField(t, "expiresIn", expiresIn);
        mongoTemplate.save(t);
    }

    public User user(String email) {
        return user(email, "John", "Doe", "en");
    }

    public static User user(String email, String lang) {
        return user(email, "John", "Doe", lang);
    }

    public static User user(String email, String givenName, String familyName, String lang) {
        return new User(UUID.randomUUID().toString(), email, givenName, familyName, "surfguest.nl",
                "http://mock-sp", "Mock SP", "Mock SP NL", lang);
    }

    protected void userSetPassword(User user, String plainTextPassword) {
        ReflectionTestUtils.setField(user, "password", plainTextPassword);
    }
}

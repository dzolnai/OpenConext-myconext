package myconext.api;

import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.exception.Base64UrlException;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import myconext.exceptions.ExpiredAuthenticationException;
import myconext.exceptions.ForbiddenException;
import myconext.exceptions.UserNotFoundException;
import myconext.mail.MailBox;
import myconext.manage.ServiceNameResolver;
import myconext.model.Challenge;
import myconext.model.DeleteServiceTokens;
import myconext.model.EduID;
import myconext.model.LinkedAccount;
import myconext.model.MagicLinkRequest;
import myconext.model.PublicKeyCredentials;
import myconext.model.SamlAuthenticationRequest;
import myconext.model.TokenRepresentation;
import myconext.model.UpdateUserSecurityRequest;
import myconext.model.User;
import myconext.model.UserResponse;
import myconext.oidcng.OpenIDConnect;
import myconext.repository.AuthenticationRequestRepository;
import myconext.repository.ChallengeRepository;
import myconext.repository.UserRepository;
import myconext.security.EmailGuessingPrevention;
import myconext.webauthn.UserCredentialRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static myconext.log.MDCContext.mdcContext;

@RestController
@RequestMapping("/myconext/api")
public class UserController {

    private static final Log LOG = LogFactory.getLog(UserController.class);

    private final UserRepository userRepository;
    private final AuthenticationRequestRepository authenticationRequestRepository;
    private final MailBox mailBox;
    private final ServiceNameResolver serviceNameResolver;
    private final OpenIDConnect openIDConnect;
    private final String magicLinkUrl;
    private final String schacHomeOrganization;
    private final String guestIdpEntityId;
    private final String webAuthnSpRedirectUrl;
    private final String idpBaseUrl;
    private final RelyingParty relyingParty;
    private final UserCredentialRepository userCredentialRepository;
    private final ChallengeRepository challengeRepository;

    private final SecureRandom random = new SecureRandom();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(-1, random);
    private final EmailGuessingPrevention emailGuessingPreventor;

    public UserController(UserRepository userRepository,
                          UserCredentialRepository userCredentialRepository,
                          ChallengeRepository challengeRepository,
                          AuthenticationRequestRepository authenticationRequestRepository,
                          MailBox mailBox,
                          ServiceNameResolver serviceNameResolver,
                          OpenIDConnect openIDConnect,
                          @Value("${email.magic-link-url}") String magicLinkUrl,
                          @Value("${schac_home_organization}") String schacHomeOrganization,
                          @Value("${guest_idp_entity_id}") String guestIdpEntityId,
                          @Value("${email_guessing_sleep_millis}") int emailGuessingSleepMillis,
                          @Value("${sp_redirect_url}") String spBaseUrl,
                          @Value("${idp_redirect_url}") String idpBaseUrl,
                          @Value("${rp_origin}") String rpOrigin,
                          @Value("${rp_id}") String rpId) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.challengeRepository = challengeRepository;
        this.authenticationRequestRepository = authenticationRequestRepository;
        this.mailBox = mailBox;
        this.serviceNameResolver = serviceNameResolver;
        this.openIDConnect = openIDConnect;
        this.magicLinkUrl = magicLinkUrl;
        this.schacHomeOrganization = schacHomeOrganization;
        this.guestIdpEntityId = guestIdpEntityId;
        this.idpBaseUrl = idpBaseUrl;
        this.webAuthnSpRedirectUrl = String.format("%s/webauthn", spBaseUrl);
        this.relyingParty = relyingParty(rpId, rpOrigin);
        this.emailGuessingPreventor = new EmailGuessingPrevention(emailGuessingSleepMillis);
    }

    @PostMapping("/idp/magic_link_request")
    public ResponseEntity newMagicLinkRequest(@Valid @RequestBody MagicLinkRequest magicLinkRequest) {
        SamlAuthenticationRequest samlAuthenticationRequest = authenticationRequestRepository.findByIdAndNotExpired(magicLinkRequest.getAuthenticationRequestId())
                .orElseThrow(ExpiredAuthenticationException::new);

        User user = magicLinkRequest.getUser();

        emailGuessingPreventor.potentialUserEmailGuess();

        Optional<User> optionalUser = userRepository.findUserByEmailIgnoreCase(user.getEmail());
        if (optionalUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Collections.singletonMap("status", HttpStatus.CONFLICT.value()));
        }
        String preferredLanguage = LocaleContextHolder.getLocale().getLanguage();
        //prevent not-wanted attributes in the database
        String requesterEntityId = samlAuthenticationRequest.getRequesterEntityId();
        User userToSave = new User(UUID.randomUUID().toString(), user.getEmail(), user.getGivenName(),
                user.getFamilyName(), schacHomeOrganization, guestIdpEntityId, requesterEntityId,
                serviceNameResolver.resolve(requesterEntityId), preferredLanguage);
        userToSave = userRepository.save(userToSave);

        return this.doMagicLink(userToSave, samlAuthenticationRequest, magicLinkRequest.isRememberMe(), false);
    }

    @PutMapping("/idp/magic_link_request")
    public ResponseEntity magicLinkRequest(@Valid @RequestBody MagicLinkRequest magicLinkRequest) {
        SamlAuthenticationRequest samlAuthenticationRequest = authenticationRequestRepository.findByIdAndNotExpired(magicLinkRequest.getAuthenticationRequestId())
                .orElseThrow(ExpiredAuthenticationException::new);

        User providedUser = magicLinkRequest.getUser();

        emailGuessingPreventor.potentialUserEmailGuess();

        Optional<User> optionalUser = findUserStoreLanguage(providedUser.getEmail());
        if (!optionalUser.isPresent()) {
            return return404();
        }
        User user = optionalUser.get();
        String requesterEntityId = samlAuthenticationRequest.getRequesterEntityId();
        String serviceProviderName = serviceNameResolver.resolve(requesterEntityId);

        boolean needToSave = user.eduIdForServiceProviderNeedsUpdate(requesterEntityId, serviceProviderName);
        if (needToSave) {
            user.computeEduIdForServiceProviderIfAbsent(requesterEntityId, serviceProviderName);
            userRepository.save(user);
        }

        if (magicLinkRequest.isUsePassword()) {
            if (!passwordEncoder.matches(providedUser.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Collections.singletonMap("status", HttpStatus.FORBIDDEN.value()));
            }
            mdcContext(Optional.of(user), "action", "successful login with password");
            LOG.info("Successfully logged in with password");
        }
        return doMagicLink(user, samlAuthenticationRequest, magicLinkRequest.isRememberMe(), magicLinkRequest.isUsePassword());
    }

    @GetMapping(value = {"/sp/me", "sp/migrate/merge", "sp/migrate/proceed"})
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        User user = userRepository.findOneUserByEmailIgnoreCase(((User) authentication.getPrincipal()).getEmail());
        return userResponseRememberMe(user);
    }

    @DeleteMapping("/sp/forget")
    public ResponseEntity<Long> forgetMe(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String userId = user.getId();
        Long count = authenticationRequestRepository.deleteByUserId(userId);

        mdcContext(Optional.of(user), "action", "forget me");
        LOG.info("Do not remember user anymore");

        return ResponseEntity.ok(count);
    }

    @PutMapping("/sp/update")
    public ResponseEntity updateUserProfile(Authentication authentication, @RequestBody User deltaUser) {
        User user = verifyAndFetchUser(authentication, deltaUser);

        user.setFamilyName(deltaUser.getFamilyName());
        user.setGivenName(deltaUser.getGivenName());
        user.validate();

        userRepository.save(user);

        mdcContext(Optional.of(user), "action", "update user profile");
        LOG.info("Update user profile");

        return ResponseEntity.status(201).body(new UserResponse(user, convertEduIdPerServiceProvider(user), false));
    }

    @PutMapping("/sp/security")
    public ResponseEntity updateUserSecurity(Authentication authentication, @RequestBody UpdateUserSecurityRequest updateUserRequest) {
        User deltaUser = userRepository.findById(updateUserRequest.getUserId()).orElseThrow(UserNotFoundException::new);
        User user = verifyAndFetchUser(authentication, deltaUser);
        if (StringUtils.hasText(user.getPassword())) {
            if (!passwordEncoder.matches(updateUserRequest.getCurrentPassword(), user.getPassword())) {
                throw new ForbiddenException();
            }
        }
        user.encryptPassword(updateUserRequest.getNewPassword(), passwordEncoder);
        userRepository.save(user);

        LOG.info(String.format("Updates / set password for user %s", user.getUsername()));

        return ResponseEntity.status(201).body(new UserResponse(user, convertEduIdPerServiceProvider(user), false));
    }

    @PutMapping("/sp/institution")
    public ResponseEntity removeUserLinkedAccounts(Authentication authentication, @RequestBody LinkedAccount linkedAccount) {
        User user = userFromAuthentication(authentication);

        List<LinkedAccount> linkedAccounts = user.getLinkedAccounts().stream()
                .filter(la -> !la.getSchacHomeOrganization().equals(linkedAccount.getSchacHomeOrganization()))
                .collect(Collectors.toList());
        user.setLinkedAccounts(linkedAccounts);
        userRepository.save(user);

        LOG.info(String.format("Deleted linked account %s from user %s",
                linkedAccount.getSchacHomeOrganization(), user.getUsername()));

        return userResponseRememberMe(user);
    }

    @PutMapping("/sp/credential")
    public ResponseEntity removePublicKeyCredential(Authentication authentication,
                                                    @RequestBody Map<String, String> credential) {
        User user = userFromAuthentication(authentication);

        String identifier = credential.get("identifier");
        List<PublicKeyCredentials> publicKeyCredentials = user.getPublicKeyCredentials().stream()
                .filter(key -> !key.getIdentifier().equals(identifier))
                .collect(Collectors.toList());
        user.setPublicKeyCredentials(publicKeyCredentials);
        userRepository.save(user);

        LOG.info(String.format("Deleted publicKeyCredentials %s from user %s",
                identifier, user.getUsername()));

        return userResponseRememberMe(user);
    }

    @PutMapping("/sp/service")
    public ResponseEntity<UserResponse> removeUserService(Authentication authentication,
                                                          @RequestBody DeleteServiceTokens serviceAndTokens) {
        User user = userFromAuthentication(authentication);

        String eduId = serviceAndTokens.getEduId();
        Map<String, EduID> eduIdPerServiceProvider = user.getEduIdPerServiceProvider().entrySet().stream()
                .filter(entry -> !entry.getValue().getValue().equals(eduId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        user.setEduIdPerServiceProvider(eduIdPerServiceProvider);
        userRepository.save(user);

        LOG.info(String.format("Deleted eduID %s from user %s", eduId, user.getUsername()));

        return doRemoveTokens(serviceAndTokens, user);
    }

    @PutMapping("/sp/tokens")
    public ResponseEntity<UserResponse> removeTokens(Authentication authentication,
                                                          @RequestBody DeleteServiceTokens serviceAndTokens) {
        User user = userFromAuthentication(authentication);

        return doRemoveTokens(serviceAndTokens, user);
    }

    private ResponseEntity<UserResponse> doRemoveTokens(@RequestBody DeleteServiceTokens serviceAndTokens, User user) {
        LOG.info(String.format("Deleted tokens %s from user %s", serviceAndTokens.getTokens(), user.getUsername()));

        List<TokenRepresentation> tokens = serviceAndTokens.getTokens();
        if (!CollectionUtils.isEmpty(tokens)) {
            openIDConnect.deleteTokens(tokens);
        }

        return userResponseRememberMe(user);
    }

    @GetMapping("/sp/tokens")
    public ResponseEntity<List<Map<String, Object>>> tokens(Authentication authentication) {
        User user = userFromAuthentication(authentication);
        return ResponseEntity.ok(this.openIDConnect.tokens(user));
    }

    private User userFromAuthentication(Authentication authentication) {
        String id = ((User) authentication.getPrincipal()).getId();
        return userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    }

    private ResponseEntity<UserResponse> userResponseRememberMe(User user) {
        List<SamlAuthenticationRequest> samlAuthenticationRequests = authenticationRequestRepository.findByUserIdAndRememberMe(user.getId(), true);
        return ResponseEntity.ok(new UserResponse(user, convertEduIdPerServiceProvider(user), !samlAuthenticationRequests.isEmpty()));
    }


    @GetMapping("sp/security/webauthn")
    public ResponseEntity spStartWebAuthFlow(Authentication authentication) {
        //We need to go from the SP to the IdP and best is too do everything server side, but we need a temporary identifier for the user
        User user = userFromAuthentication(authentication);

        String webAuthnIdentifier = this.hash();
        user.setWebAuthnIdentifier(webAuthnIdentifier);
        userRepository.save(user);

        return ResponseEntity.status(200).body(Collections.singletonMap("token", webAuthnIdentifier));
    }

    @PostMapping("idp/security/webauthn/registration")
    public ResponseEntity idpWebAuthnRegistration(@RequestBody Map<String, String> body) throws Base64UrlException {
        String token = body.get("token");
        Optional<User> optionalUser = userRepository.findUserByWebAuthnIdentifier(token);
        if (!optionalUser.isPresent()) {
            return return404();
        }

        User user = optionalUser.get();
        if (StringUtils.isEmpty(user.getUserHandle())) {
            user.setUserHandle(this.hash());
            userRepository.save(user);
        }
        PublicKeyCredentialCreationOptions request = publicKeyCredentialCreationOptions(this.relyingParty, user);
        String challenge = request.getChallenge().getBase64Url();
        //we need to store the challenge to retrieve it later on the way back
        challengeRepository.save(new Challenge(token, challenge));
        return ResponseEntity.status(200).body(request);
    }

    /*
     * See https://github.com/Yubico/java-webauthn-server
     */
    @PutMapping("idp/security/webauthn/registration")
    public ResponseEntity idpWebAuthn(@RequestBody Map<String, Object> body) {
        try {
            return doIdpWebAuthn(body);
        } catch (Exception e) {
            //We always want to redirect back to the SP
            return ResponseEntity.status(201).body(Collections.singletonMap("location", webAuthnSpRedirectUrl));
        }
    }

    private ResponseEntity doIdpWebAuthn(Map<String, Object> body) throws IOException, Base64UrlException, RegistrationFailedException, NoSuchFieldException, IllegalAccessException {
        String token = (String) body.get("token");
        String name = (String) body.get("name");

        Optional<User> optionalUser = userRepository.findUserByWebAuthnIdentifier(token);
        if (!optionalUser.isPresent()) {
            return return404();
        }
        String credentials = (String) body.get("credentials");
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc =
                PublicKeyCredential.parseRegistrationResponseJson(credentials);

        User user = optionalUser.get();
        //remove the webAuthnIdentifier
        user.setWebAuthnIdentifier(null);

        PublicKeyCredentialCreationOptions request = this.publicKeyCredentialCreationOptions(this.relyingParty, user);
        //Needed to succeed the validation
        Challenge challenge = challengeRepository.findByToken(token).orElseThrow(ForbiddenException::new);
        this.restoreChallenge(request, ByteArray.fromBase64Url(challenge.getChallenge()));
        challengeRepository.delete(challenge);

        RegistrationResult result = this.relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                .request(request)
                .response(pkc)
                .build());
        PublicKeyCredentialDescriptor keyId = result.getKeyId();
        ByteArray publicKeyCose = result.getPublicKeyCose();

        user.addPublicKeyCredential(keyId, publicKeyCose, name);
        userRepository.save(user);

        return ResponseEntity.status(201).body(Collections.singletonMap("location", webAuthnSpRedirectUrl));
    }


    @PostMapping("idp/security/webauthn/authentication")
    public ResponseEntity idpWebAuthnStartAuthentication(@RequestBody Map<String, String> body) throws Base64UrlException {
        String email = body.get("email");
        emailGuessingPreventor.potentialUserEmailGuess();

        Optional<User> optionalUser = userRepository.findUserByEmailIgnoreCase(email);
        if (!optionalUser.isPresent()) {
            return return404();
        }
        User user = optionalUser.get();

        AssertionRequest request = this.relyingParty.startAssertion(StartAssertionOptions.builder()
                .username(Optional.of(user.getEmail()))
                .build());

        String authenticationRequestId = body.get("authenticationRequestId");
        String challenge = request.getPublicKeyCredentialRequestOptions().getChallenge().getBase64Url();
        // The user might have started webauthn already (and cancelled it). Need to prevent duplicates
        challengeRepository.findByToken(authenticationRequestId)
                .ifPresent(existingChallenge -> challengeRepository.delete(existingChallenge));
        challengeRepository.save(new Challenge(authenticationRequestId, challenge, user.getEmail()));

        return ResponseEntity.status(200).body(request);
    }

    @PutMapping("idp/security/webauthn/authentication")
    public ResponseEntity idpWebAuthnTryAuthentication(@RequestBody Map<String, Object> body) throws Base64UrlException, IOException, AssertionFailedException, NoSuchFieldException, IllegalAccessException {
        String authenticationRequestId = (String) body.get("authenticationRequestId");
        SamlAuthenticationRequest samlAuthenticationRequest = authenticationRequestRepository.findByIdAndNotExpired(authenticationRequestId)
                .orElseThrow(ExpiredAuthenticationException::new);

        boolean rememberMe = (boolean) body.get("rememberMe");

        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc =
                PublicKeyCredential.parseAssertionResponseJson((String) body.get("credentials"));

        Challenge challenge = challengeRepository.findByToken(authenticationRequestId).orElseThrow(ForbiddenException::new);
        AssertionRequest assertionRequest = this.relyingParty.startAssertion(StartAssertionOptions.builder()
                .username(Optional.of(challenge.getEmail()))
                .build());
        this.restoreChallenge(assertionRequest.getPublicKeyCredentialRequestOptions(), ByteArray.fromBase64Url(challenge.getChallenge()));

        AssertionResult result = this.relyingParty.finishAssertion(FinishAssertionOptions.builder()
                .request(assertionRequest)
                .response(pkc)
                .build());

        if (!result.isSuccess()) {
            throw new ForbiddenException();
        }
        challengeRepository.delete(challenge);

        Optional<User> optionalUser = findUserStoreLanguage(challenge.getEmail());
        if (!optionalUser.isPresent()) {
            return return404();
        }
        User user = optionalUser.get();

        mdcContext(Optional.of(user), "action", "successful login with authn");
        LOG.info("successfully logged in with AuthnWeb");

        return doMagicLink(user, samlAuthenticationRequest, rememberMe, true);
    }

    private PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions(RelyingParty relyingParty, User user) throws Base64UrlException {
        return relyingParty.startRegistration(StartRegistrationOptions.builder()
                .user(UserIdentity.builder()
                        .name(user.getEmail())
                        .displayName(String.format("%s %s", user.getGivenName(), user.getFamilyName()))
                        .id(ByteArray.fromBase64Url(user.getUserHandle()))
                        .build())
                .build());
    }

    private RelyingParty relyingParty(String rpId, String rpOrigin) {
        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                .id(rpId)
                .name("eduID")
                .build();
        return RelyingParty.builder()
                .identity(rpIdentity)
                .credentialRepository(this.userCredentialRepository)
                .origins(Collections.singleton(rpOrigin))
                .build();
    }

    protected static void restoreChallenge(Object challengeContainer, ByteArray challenge) throws NoSuchFieldException, IllegalAccessException {
        Field challengeField = challengeContainer.getClass().getDeclaredField("challenge");
        challengeField.setAccessible(true);
        challengeField.set(challengeContainer, challenge);
    }

    @GetMapping("/sp/logout")
    public ResponseEntity logout(HttpServletRequest request) throws URISyntaxException {
        return doLogout(request);
    }

    @DeleteMapping("/sp/delete")
    public ResponseEntity deleteUser(Authentication authentication, HttpServletRequest request) throws URISyntaxException {
        User principal = (User) authentication.getPrincipal();
        userRepository.deleteById(principal.getId());

        LOG.info(String.format("Deleted user %s", principal.getEmail()));

        return doLogout(request);
    }

    private ResponseEntity doLogout(HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.invalidate();
        SecurityContextHolder.getContext().setAuthentication(null);
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Collections.singletonMap("status", "ok"));
    }

    private User verifyAndFetchUser(Authentication authentication, User deltaUser) {
        return verifyAndFetchUser(authentication, deltaUser.getId());
    }

    private User verifyAndFetchUser(Authentication authentication, String id) {
        User principal = (User) authentication.getPrincipal();
        if (!principal.getId().equals(id)) {
            throw new ForbiddenException();
        }
        //Strictly not necessary, but mid-air collisions can occur in theory
        return userRepository.findOneUserByEmailIgnoreCase(principal.getEmail());
    }

    private ResponseEntity doMagicLink(User user, SamlAuthenticationRequest samlAuthenticationRequest, boolean rememberMe,
                                       boolean passwordOrWebAuthnFlow) {
        samlAuthenticationRequest.setHash(hash());
        samlAuthenticationRequest.setUserId(user.getId());
        samlAuthenticationRequest.setPasswordOrWebAuthnFlow(passwordOrWebAuthnFlow);
        samlAuthenticationRequest.setRememberMe(rememberMe);
        if (rememberMe) {
            samlAuthenticationRequest.setRememberMeValue(UUID.randomUUID().toString());
        }
        authenticationRequestRepository.save(samlAuthenticationRequest);
        String serviceName = serviceNameResolver.resolve(samlAuthenticationRequest.getRequesterEntityId());

        if (passwordOrWebAuthnFlow) {
            LOG.info(String.format("Returning passwordOrWebAuthnFlow magic link for existing user %s", user.getUsername()));
            return ResponseEntity.status(201).body(Collections.singletonMap("url", this.magicLinkUrl + "?h=" + samlAuthenticationRequest.getHash()));
        }

        if (user.isNewUser()) {
            LOG.info(String.format("Sending account verification mail with magic link for new user %s", user.getUsername()));
            mailBox.sendAccountVerification(user, samlAuthenticationRequest.getHash());
        } else {
            mdcContext(Optional.of(user), "action", "Sending magic link email", "service", serviceName);
            LOG.info("Sending magic link email for existing user");

            mailBox.sendMagicLink(user, samlAuthenticationRequest.getHash(), serviceName);
        }
        return ResponseEntity.status(201).body(Collections.singletonMap("result", "ok"));
    }

    private Map<String, EduID> convertEduIdPerServiceProvider(User user) {
        return user.getEduIdPerServiceProvider();
    }

    private Optional<User> findUserStoreLanguage(String email) {
        Optional<User> optionalUser = userRepository.findUserByEmailIgnoreCase(email);
        optionalUser.ifPresent(user -> {
            String preferredLanguage = user.getPreferredLanguage();
            String language = LocaleContextHolder.getLocale().getLanguage();

            if (StringUtils.isEmpty(preferredLanguage) || !preferredLanguage.equals(language)) {
                user.setPreferredLanguage(language);
                userRepository.save(user);
            }
        });
        return optionalUser;
    }

    private ResponseEntity return404() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("status", HttpStatus.NOT_FOUND.value()));
    }

    private String hash() {
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

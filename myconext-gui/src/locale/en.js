import I18n from "i18n-js";

I18n.translations.en = {
    sidebar: {
        home: "Home",
        personalInfo: "Personal info",
        dataActivity: "Data & activity",
        security: "Security",
        account: "Account"
    },
    start: {
        hi: "Hi {{name}}!",
        manage: "Manage your personal info, your privacy, and the security of your eduID account."
    },
    header: {
        title: "eduID",
        logout: "Logout"
    },
    landing: {
        logoutTitle: "You have been logged out",
        logoutStatus: "To finalise the logout process you must now close this browser.",
        deleteTitle: "Your eduID account has been deleted",
        deleteStatus: "To finalise the removal process you must now close this browser."
    },
    notFound: {
        title: "Whoops...",
        title2: "Something went wrong (404)."
    },
    profile: {
        title: "Personal information",
        info: "When you use eduID to logon to other websites, some of your personal information needs to be shared. Some services require that your personal information is validated by a third party.",
        basic: "Basic information",
        email: "Email address",
        name: "Name",
        validated: "Validated information",
        firstAndLastName: "First and last name",
        firstAndLastNameInfo: "Your first and lastname are not yet verified by a trusted party",
        verify: "Verify",
        student: "Prove of student",
        studentInfo: "You have not yet proven that you are following education in the Netherlands.",
        prove: "Prove",
        trusted: "Trusted party link",
        trustedInfo: "You eduID account is not yet linked to a trusted party",
        link: "Link",
        institution: "Institution",
        affiliations: "Affiliation(s)",
        expires: "Connection expires",
        expiresValue: "{{date}}",
    },
    edit: {
        title: "Name",
        info: "Please provide your full name",
        givenName: "Your first name",
        familyName: "Your last name",
        update: "Update",
        cancel: "Cancel",
        updated: "Your profile has been updated",
        back: "/personal"
    },
    email: {
        title: "Email",
        info: "Please enter your new email. A verification mail will be send to this address.",
        email: "Your new email",
        update: "Request",
        cancel: "Cancel",
        updated: "A mail has been send to {{email}}",
        back: "/personal"
    },
    security: {
        title: "Security settings",
        subTitle: "We provide different methods to sign in to your eduID account.",
        secondSubTitle: "Sign-in methods",
        usePassword: "Password",
        usePublicKey: "WebAuthn",
        notSet: "Not set",
        notSupported: "Not supported",
        useMagicLink: "Send magic link to",
        rememberMe: "Stay logged in",
        rememberMetrue: "Yes",
        rememberMefalse: "No"
    },
    home: {
        home: "Home",
        welcome: "Welcome {{name}}",
        personal: "Personal info",
        security: "Security",
        account: "Account",
        institutions: "Connections",
        services: "Services",
        favorites: "Favorites",
        settings: "Settings",
        links: {
            teams: "Teams",
            teamsHref: "https://teams.{{baseDomain}}"
        }
    },
    account: {
        title: "Your eduID account",
        personalTitle: "Your personal data",
        personalInfo: "Click the button below to download all your personal data.",
        download: "Download",
        deleteTitle: "Deleting your eduID account",
        info1: "You can delete your eduID whenever you want.",
        info2: "Proceed with care, as you will lose the unique eduID number currently associated with your email address. When you re-register for a new eduID with that same email address, you will receive a new eduID number. Some services use this unique number to identify you, so for those services you will be treated as a new user.",
        info3: "Please note that deleting your eduID account does not mean all services you accessed with that eduID account will also have your data removed.",
        info4: "To fully complete the process of deleting your eduID account you must close your browser after your account has been removed.",
        deleteAccount: "Delete my account",
        deleteAccountConfirmation: "Are you sure you want to delete your eduID account?"
    },
    institutions: {
        title: "Connected institutions",
        explanation: "By connecting your educational institution to your eduID, you will be able to prove through eduID that you study or work at one of these institutions.",
        noInstitutions: "You currently have not connected any educational institutions to your eduID. Click the button below to connect an institution.",
        name: "Short name",
        add: "Connect new institution",
        addInstitutionConfirmation: "When you proceed you will be asked to login at the institution you want to connect to your eduID. First, select which institution you want to connect; then, login at that institution.<br/><br/>After a successful login you will come back here.",
        proceed: "Proceed",
        addInstitution: "Add institution"
    },
    institution: {
        title: "Connected institution",
        info: "This institution was connected to your eduID account on {{date}} at {{hours}}:{{minutes}}",
        name: "Institution name",
        eppn: "Identifier at institution",
        displayName: "Display name",
        affiliations: "Affiliation(s) at institution",
        expires: "This connection expires at",
        expiresValue: "{{date}}",
        delete: "Remove connection",
        cancel: "Cancel",
        deleted: "The connection with your institution {{name}} has been removed",
        back: "/institutions",
        deleteInstitution: "Delete institution",
        deleteInstitutionConfirmation: "Are you sure you want to delete the connection with this institution?<br/><br/>Some services require that you your eduID is connected to an institution. You might be prompted to connect an institution if you access one of those services."
    },
    services: {
        title: "Used services",
        info: "Each service you accessed through eduID receives certain personal data (attributes) from your eduID account. For example, your name, your email address or a pseudonym which the service can use to uniquely identify you.",
        explanation: "The overview below shows you all services you have logged in to using eduID.",
        noServices: "You did not yet use eduID to login to any service.",
        name: "Name"
    },
    service: {
        title: "Service",
        info: "You first logged in to this service on {{date}} at {{hours}}:{{minutes}}",
        tokenInfo: "The service {{name}} has an API token to access your account.",
        name: "Name",
        eduId: "EduID",
        access: "Access",
        accounts: "Accounts",
        delete: "Delete",
        cancel: "Cancel",
        revoke: "Revoke",
        deleted: "Your service eduID for {{name}} has been deleted",
        back: "/services",
        deleteService: "Delete service",
        deleteServiceConfirmation: "Are you sure you want to delete your unique pseudonymized eduID for {{name}} and revoke access to your linked accounts?<br/><br/>This service might not recognize you the next time you login and all your personal data with this Service could be deleted.",
        deleteTokenConfirmation: "Are you sure you want to revoke the API access token for {{name}}?",
        deleteToken: "Revoke token"
    },
    credential: {
        title: "Public Key Credential",
        info: "You added this key on {{date}} at {{hours}}:{{minutes}}",
        name: "Name",
        delete: "Delete",
        cancel: "Cancel",
        deleted: "Your key {{name}} has been deleted",
        back: "/weauthn",
        deleteCredential: "Delete key",
        deleteCredentialConfirmation: "Are you sure you want to delete your public key credential {{name}}? The key will be deleted from your eduID account, but will not be removed from your browser and / or YubiKey device."
    },
    password: {
        setTitle: "Set password",
        updateTitle: "Change password",
        resetTitle: "Reset forgotten password",
        currentPassword: "Current password",
        newPassword: "New password",
        confirmPassword: "Confirm new password",
        setUpdate: "Set password",
        updateUpdate: "Update password",
        cancel: "Cancel",
        set: "Your password has been set",
        reset: "Your password has been reset to a new password",
        updated: "Your password has been updated",
        back: "/security",
        passwordDisclaimer: "Make sure it's at least 15 characters OR at least 8 characters including a number and a uppercase letter.",
        invalidCurrentPassword: "Your current password is invalid.",
        passwordResetHashExpired: "Your password reset link has expired. ",
        forgotPassword: "Help! I forgot my current password",
        passwordResetSendAgain: "Send an email to reset my password.",
        forgotPasswordConfirmation: "Forgot your password? Press 'Confirm' below to instantly receive an email with a link to reset your current password.",
        flash: {
            passwordLink: "An email has been sent to {{name}} with a link to reset your password"
        }
    },
    webauthn: {
        setTitle: "Enable WebAuthn",
        updateTitle: "Configure WebAuthn",
        publicKeys: "Your public keys",
        noPublicKeys: "You have not added any keys. ",
        nameRequired: "Before you can add a new key you will need to give it a name.",
        revoke: "Revoke",
        addDevice: "Add device",
        info: "Public Key Cryptography and Web Authentication (a.k.a. WebAuthn) allows eduID to authenticate you using public key cryptography instead of a magic link or password.",
        info2: "When your click on {{action}} you will be redirected to the eduID identity server and your browser will prompt you for your identity.",
        back: "/security",
        setUpdate: "Start",
        updateUpdate: "Add device",
        credentialName: "Device name",
        credentialNamePlaceholder: "e.g. my red YubiKey",
        test: "Test",
        testInfo: "Hit the <strong>test</strong> button to test one of your WebAuthn keys. You will be redirected to the eduID identity server.",
        testFlash: "You successfully tested your WebAuthn key to authenticate"
    },
    rememberMe: {
        updated: "Your device is no longer remembered",
        forgetMeTitle: "Remember this device.",
        info: "Your device is currently remembered. You will be automatically logged in on the eduID.",
        cancel: "Cancel",
        update: "Forget me",
        forgetMeConfirmation: "Are you sure you no longer want this device remembered?",
        forgetMe: "Forget this device"
    },
    footer: {
        privacy: "Privacy policy",
        terms: "Terms of Use",
        help: "Help",
        poweredBy: "Powered by"
    },
    modal: {
        cancel: "Cancel",
        confirm: "Confirm"
    },
    migration: {
        header: "Your eduID has been created!",
        info: "Your Onegini account is succesfully migrated.",
        info2: "From now on you must use your eduID guest account to logon to services where you previously used Onegini.",
        info3: "Tip! Your eduID account by default doesn’t need a password (we will send a magic link to your email to sign you in), but if you want to, you can set one right now under the",
        securityLink: " Security tab.",
        link: "Show my account details"
    },
    migrationError: {
        header: "Warning: email address already used for eduID",
        info: "You already have an {{email}} eduID with the same email address as your Onegini account. You must therefore choose how you wish to proceed:",
        sub1: "Continue with migrating your existing Onegini account to a new eduID. This means:",
        sub1Inner1: "Existing SURFconext Teams memberships and authorisations within services belonging to your current Onegini account will be transferred to your new eduID.",
        sub1Inner2: "Team memberships and authorisations linked to your current eduID will be lost.",
        sub2: "Abort migration and continue with your existing eduID. This means:",
        sub2Inner1: "You will continue to use your current eduID.",
        sub2Inner2: "You can no longer use your existing Onegini account per 1 July 2020. Existing SURFconext Teams memberships and authorisations within services belonging to your current Onegini account will be lost.",
        abortMigration: "Abort migration",
        continueMigration: "Continue migration",
        abort: "abort",
        continue: "continue",
        help: "Need help? Send an email to <a href=\"mailto:support@surfconext.nl\">support@surfconext.nl</a>."
    },
    format: {
        creationDate: "Your eduID account was created on {{date}} at {{hours}}:{{minutes}}"
    }
};

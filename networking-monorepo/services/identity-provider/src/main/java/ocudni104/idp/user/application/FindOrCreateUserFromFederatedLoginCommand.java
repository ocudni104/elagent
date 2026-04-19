package ocudni104.idp.user.application;

public record FindOrCreateUserFromFederatedLoginCommand(
        String email,
        String provider,
        String providerSubject
) {}

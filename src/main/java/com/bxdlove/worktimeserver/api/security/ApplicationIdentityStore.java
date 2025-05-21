package com.bxdlove.worktimeserver.api.security;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.credential   .UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import org.bson.Document;

import java.util.EnumSet;
import java.util.Set;

@ApplicationScoped
public class ApplicationIdentityStore implements IdentityStore {

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public Set<ValidationType> validationTypes() {
        return EnumSet.of(ValidationType.VALIDATE);
    }

    public CredentialValidationResult validate(UsernamePasswordCredential credential) {
        String username = credential.getCaller();
        String password = credential.getPasswordAsString();

        try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            boolean isValid = mongoClient.getDatabase("worktime_server")
                    .getCollection("employee")
                    .find(new Document("email", username).append("password", password))
                    .first() != null;

            if (isValid) {
                return new CredentialValidationResult(username, Set.of("users"));
            }
        }

        return CredentialValidationResult.NOT_VALIDATED_RESULT;
    }
}

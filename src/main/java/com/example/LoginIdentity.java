package com.example;

/** Who signed in, as vouched for by Google, Apple or GitHub. Everything but provider and subject may be null. */
public record LoginIdentity(String provider, String subject, String email, String name, String givenName,
                            String picture) {
}

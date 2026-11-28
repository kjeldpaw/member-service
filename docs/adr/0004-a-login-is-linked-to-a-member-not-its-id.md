# A login is linked to a Member, not used as the Member's id

A Member may exist without a login, such as a child looked after by their
Guardians, and get one later. So a Member's id is no longer their Keycloak
user id: each Member has an optional link to their Keycloak user, and the
Profile is found through that link. Members created before this keep a link
equal to their id.

## Considered Options

- **Keep id = Keycloak user id, and change the Member's id when they get a
  login**: the Clubs, Graduations, References and Guardians that point at
  the id would all have to be rewritten.
- **Create a disabled Keycloak user for every Member without a login**: the
  id would still match, but Keycloak would hold people who cannot log in,
  each needing a made-up username.

## Consequences

- For an existing Member the link and the id are equal, so code that still
  treats the token's subject as the Member id works for them and fails only
  for Members who got their login later. Such code is a bug.
- A Member without a login has no Keycloak user, so they have no Role
  (ADR 0003) and deleting them touches no Keycloak user.

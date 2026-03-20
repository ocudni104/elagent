# OAuth2 Flow — Explicacion del sistema

## El flujo completo

Hay **dos niveles de OAuth2** en este sistema:

```
[Astro :4321]  →  [nm-idp-service :8081]  →  [GitHub]
                         ↓
                   emite su propio JWT
                         ↓
[Astro :4321]  →  [nm-api-gateway :8080]  →  [cualquier servicio]
```

### Nivel 1 — El IDP habla con GitHub (servidor → servidor)

Esto es para **autenticar al usuario**. El IDP delega la autenticacion a GitHub.

### Nivel 2 — El Frontend habla con el IDP (cliente → IDP propio)

Esto es para **autorizar al frontend**. El IDP emite sus propios JWTs que el gateway entiende.

---

## El flujo paso a paso

```
1. Astro abre en el navegador:
   http://localhost:8081/oauth2/authorize?client_id=nm-frontend&...&code_challenge=XYZ

2. El IDP recibe esto → ve que el usuario NO esta autenticado
   → redirige al login de GitHub:
   https://github.com/login/oauth/authorize?client_id=...

3. El usuario escribe su usuario/contrasena en GitHub
   → GitHub autentica y redirige de vuelta al IDP:
   http://localhost:8081/login/oauth2/code/github?code=GITHUB_CODE

4. El IDP intercambia ese code con GitHub por un token de GitHub
   → obtiene el perfil del usuario (username = "login" de GitHub)

5. FederatedIdentityAuthenticationSuccessHandler entra en accion:
   → convierte el OAuth2AuthenticationToken (de GitHub) en un
     UsernamePasswordAuthenticationToken local con el username de GitHub
   → lo mete en el SecurityContext

6. Ahora el IDP sabe quien es el usuario → completa el flujo OAuth2 propio:
   → redirige a Astro: http://localhost:4321/callback?code=IDP_CODE

7. Astro hace POST a /oauth2/token con ese code + code_verifier
   → el IDP valida el PKCE y emite: { access_token, refresh_token, id_token }

8. Astro guarda el access_token y lo envia en cada request:
   GET /api/algo  →  nm-api-gateway :8080
   Authorization: Bearer <access_token>

9. El Gateway valida el JWT contra el JWKS del IDP
   → si es valido, reenvía la request al servicio destino
   → si no, devuelve 401
```

---

## El codigo del IDP explicado

### `KeyConfig.java` — La llave RSA

```java
KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
generator.initialize(2048);
KeyPair keyPair = generator.generateKeyPair();
```

Genera un par de llaves RSA al arrancar. La **clave privada** firma los JWTs. La **clave publica**
se expone en `/oauth2/jwks` para que cualquiera (el gateway) pueda verificar que los tokens son autenticos.

> Advertencia: Se genera en memoria → si reinicias el IDP, todos los tokens anteriores quedan invalidos.

---

### `AuthorizationServerConfig.java` — El servidor de autorizacion

**El cliente registrado:**

```java
RegisteredClient nmFrontend = RegisteredClient.withId(...)
    .clientId("nm-frontend")
    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)  // sin contrasena (publico)
    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
    .redirectUri("http://localhost:4321/callback")
    .clientSettings(ClientSettings.builder()
        .requireProofKey(true)  // obliga PKCE
        .build())
    .tokenSettings(TokenSettings.builder()
        .accessTokenTimeToLive(Duration.ofMinutes(15))
        .refreshTokenTimeToLive(Duration.ofDays(7))
        .build())
    .build();
```

Esto registra `nm-frontend` (el Astro) como cliente valido. Es un **cliente publico** (sin secreto)
que usa **PKCE** en lugar de un secreto para demostrar que el que pide el token es el mismo que inicio el flujo.

**El issuer:**

```java
AuthorizationServerSettings.builder()
    .issuer("http://localhost:8081")
```

Todos los JWTs que emita tendran `"iss": "http://localhost:8081"`. El gateway usara esto para validar.

---

### `SecurityConfig.java` — Que rutas son publicas

```java
.requestMatchers(
    "/.well-known/openid-configuration",  // discovery doc
    "/oauth2/jwks",                        // llave publica RSA
    "/actuator/health",
    "/error"
).permitAll()
.anyRequest().authenticated()             // todo lo demas requiere login
.oauth2Login(oauth2 -> oauth2
    .successHandler(new FederatedIdentityAuthenticationSuccessHandler())
)
```

Hay dos `SecurityFilterChain`:

- **Order(1)** — `AuthorizationServerConfig`: protege los endpoints OAuth2 propios (`/oauth2/authorize`, `/oauth2/token`, etc.)
- **Order(2)** — `SecurityConfig`: todo lo demas, incluyendo el login con GitHub

---

### `FederatedIdentityAuthenticationSuccessHandler.java` — El puente

Este es el corazon del sistema. Sin esto, despues del login de GitHub el flujo se rompe porque
el IDP no sabe como conectar "autenticado en GitHub" con "autenticado en mi sistema".

```java
// GitHub nos da un OAuth2AuthenticationToken con el perfil del usuario
if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
    String githubLogin = oauthToken.getPrincipal().getAttribute("login");
    // "login" es el username de GitHub, ej: "samec"

    // Lo convertimos a un token local que Spring Authorization Server entiende
    UsernamePasswordAuthenticationToken localAuth =
        UsernamePasswordAuthenticationToken.authenticated(
            githubLogin,        // el "username" del sistema sera el login de GitHub
            null,               // sin contrasena (ya autentico GitHub)
            AuthorityUtils.createAuthorityList("ROLE_USER")
        );

    SecurityContextHolder.getContext().setAuthentication(localAuth);
}

// Reanuda el flujo OAuth2 que estaba pendiente
// → redirige a /callback?code=IDP_CODE
delegate.onAuthenticationSuccess(request, response, ...);
```

**Sin este handler:** GitHub autentica → Spring no sabe que hacer → error.
**Con este handler:** GitHub autentica → se crea sesion local → el IDP puede emitir su JWT.

---

## El codigo del Gateway explicado

### `GatewaySecurityConfig.java`

```java
@EnableWebFluxSecurity  // reactivo (WebFlux), no el tipico Servlet
public class GatewaySecurityConfig {

    .csrf(disable)                          // no necesario para APIs con JWT
    .pathMatchers("/actuator/health").permitAll()  // health sin token
    .anyExchange().authenticated()          // todo lo demas requiere JWT valido
    .oauth2ResourceServer(oauth2 -> oauth2
        .jwt(jwt -> {})                     // valida JWTs automaticamente
    );
}
```

El gateway es un **resource server** — no autentica usuarios, solo **verifica tokens**.

### `application.yml` del gateway

```yaml
security:
  oauth2:
    resourceserver:
      jwt:
        issuer-uri: http://localhost:8081   # de aqui descarga la llave publica
```

Al arrancar, el gateway hace `GET http://localhost:8081/.well-known/openid-configuration`
para encontrar la URL del JWKS, luego descarga la clave publica RSA. Cada vez que llega un
request con `Bearer <token>`, verifica la firma con esa clave.

```yaml
routes:
  - id: idp
    uri: http://localhost:8081
    predicates:
      - Path=/api/idp/**
    filters:
      - StripPrefix=2   # /api/idp/foo → /foo
```

Si el token es valido, reenvía el request al servicio destino. `StripPrefix=2` elimina los
primeros dos segmentos del path antes de reenviar.

---

## Resumen de los actores

| Componente | Rol | Puerto |
|---|---|---|
| **GitHub** | Proveedor de identidad externo | (nube) |
| **nm-idp-service** | Authorization Server (emite JWTs) + OAuth2 Client (habla con GitHub) | 8081 |
| **nm-api-gateway** | Resource Server (valida JWTs) + Router | 8080 |
| **Astro frontend** | OAuth2 Client publico (PKCE) | 4321 |

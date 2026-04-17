# Flujo de Autenticacion — Explicacion del sistema

## El flujo completo

El sistema tiene **dos mecanismos de autenticacion** separados:

- El **frontend** usa sesiones (cookie `JSESSIONID`) — no maneja JWTs directamente.
- Los **servicios internos** usan JWTs emitidos por el IDP.

El gateway es el puente: recibe la cookie de sesion del navegador, la intercambia por un JWT interno, y reenvía ese JWT a los servicios.

```
[Astro :4321]  →  [nm-api-gateway :8080]  →  [nm-idp-service :8081]  →  [Google]
                                                       ↓
                                             crea sesion HTTP (JSESSIONID)
                                                       ↓
[Astro :4321]  →  [nm-api-gateway :8080]  (con cookie JSESSIONID)
                         ↓
               SessionToJwtFilter llama a IDP:
               GET /internal/token + cookie
                         ↓
               IDP emite JWT interno (5 min)
                         ↓
               Gateway inyecta: Authorization: Bearer <jwt>
                         ↓
               [cualquier servicio backend]
```

> **Importante**: El IDP no debe ser directamente accesible desde el navegador en produccion.
> Solo el gateway (puerto 8080) es publico. El IDP es interno. En Docker, se elimina el
> mapeo de puertos del IDP para forzar que todo pase por el gateway.

---

## El flujo paso a paso

### Fase 1 — Login (una sola vez)

```
1. El usuario navega a:
   http://localhost:8080/api/idp/oauth2/authorization/google
   (SIEMPRE a traves del gateway, nunca directamente al IDP)

2. Gateway: /api/idp/** → permitAll → enruta al IDP (StripPrefix=2)
   SessionToJwtFilter: ruta /api/idp/ → SKIP (no toca la sesion)

3. IDP recibe GET /oauth2/authorization/google
   → guarda el OAuth2 state en la sesion HTTP
   → Set-Cookie: JSESSIONID=abc123 (va al browser via el gateway → cookie fijada en localhost:8080)
   → redirige a Google con:
     redirect_uri=http://localhost:8080/api/idp/login/oauth2/code/google

4. El usuario inicia sesion en Google.
   Google redirige al navegador a:
   http://localhost:8080/api/idp/login/oauth2/code/google?code=GOOGLE_CODE&state=XYZ

5. Gateway: /api/idp/** → permitAll → enruta al IDP
   SessionToJwtFilter: ruta /api/idp/ → SKIP (critico: no tocar la sesion aqui evita
   que Spring Security migre la sesion y pierda el OAuth2 state almacenado)

6. IDP recibe GET /login/oauth2/code/google?code=...&state=...
   → verifica que el state coincide con el guardado en sesion
   → intercambia el code con Google por un token de Google
   → obtiene perfil del usuario (email, nombre, etc.)

7. FederatedIdentityAuthenticationSuccessHandler:
   → extrae el email del perfil de Google
   → lo convierte en UsernamePasswordAuthenticationToken local (ROLE_USER)
   → lo mete en el SecurityContext
   → Spring guarda la autenticacion en la sesion abc123

8. Post-login redirect → http://localhost:8080/  (el gateway, configurado via app.gateway-url)
   El navegador sigue la cookie JSESSIONID=abc123 fijada en localhost:8080 ✓
```

### Fase 2 — Peticion a la API (cada request)

```
9. Astro hace una llamada:
   GET http://localhost:8080/hello
   Cookie: JSESSIONID=abc123   ← el navegador la envia automaticamente

10. Gateway — SessionToJwtFilter (HIGHEST_PRECEDENCE):
    → ruta /hello → NO es /api/idp/ → sigue adelante
    → ve que no hay Authorization header pero si JSESSIONID
    → llama al IDP: GET http://localhost:8081/internal/token
                    Cookie: JSESSIONID=abc123

11. IDP — InternalTokenController:
    → Spring Security carga el SecurityContext de la sesion abc123
    → el Authentication (email del usuario) se inyecta como parametro del metodo
    → emite JWT interno firmado con clave RSA:
      {
        "sub": "usuario@gmail.com",
        "iss": "http://localhost:8081",
        "exp": <ahora + 5 minutos>,
        "roles": ["ROLE_USER"]
      }

12. Gateway — SessionToJwtFilter:
    → inyecta el JWT en la request:
      Authorization: Bearer <jwt>

13. Gateway — Spring Security (resource server):
    → descarga JWKS del IDP: GET http://localhost:8081/oauth2/jwks
    → valida firma del JWT con la clave publica RSA
    → si es valido → deja pasar

14. Gateway enruta a app-service segun la ruta configurada.
    El servicio backend recibe la request con el JWT en el header.
    Puede leer "sub" para saber quien es el usuario.
```

---

## Decisiones de diseno clave

| Aspecto | Decision |
|---|---|
| **El browser nunca ve un JWT** | Solo maneja la cookie `JSESSIONID`. El JWT es interno entre gateway y servicios. |
| **El JWT vive 5 minutos** | Se genera uno nuevo en cada request. Sin estado problematico que invalidar. |
| **La clave RSA es in-memory** | Se regenera al reiniciar el IDP — todos los JWT existentes quedan invalidos. En prod requiere clave persistida. |
| **`/internal/token` devuelve 401** | `SecurityConfig` configura `HttpStatusEntryPoint` para `/internal/**` — el gateway maneja el error limpiamente en lugar de recibir un redirect a `/login`. |
| **`NullRequestCache` en el IDP** | Evita que Spring Security guarde URLs como destino post-login (ej. `/internal/token` si se visita directamente). |
| **`SessionToJwtFilter` salta `/api/idp/**`** | Si el filtro llama a `/internal/token` durante el callback de Google, Spring Security puede migrar la sesion (session fixation) y borrar el OAuth2 state → error. |
| **`redirect-uri` por el gateway** | El callback de Google vuelve a `localhost:8080/api/idp/...` (no al IDP directamente), para que la cookie `JSESSIONID` se fije en el puerto del gateway. |
| **`app.gateway-url` en el IDP** | Controla el redirect post-login. Sin esto, el IDP redirige a su propia URL (`172.30.x.x:8081`) y el browser pierde la sesion. |
| **No hay OAuth2 clients registrados** | `RegisteredClientRepository` vacio — el Authorization Server solo se usa para `JwtEncoder` + endpoint JWKS. El frontend no hace PKCE ni auth-code flow (aun). |

---

## Arquitectura de seguridad por capas

```
Internet / Browser
      ↓
[Gateway :8080]  ← unico punto de entrada publico
  - /api/idp/**  → permitAll (flujo de login)
  - /actuator/health → permitAll
  - resto        → requiere JWT valido
      ↓
[IDP :8081]      ← red interna, no accesible desde browser en prod
  - /oauth2/jwks, /.well-known/... → publicos
  - /internal/**  → 401 (no redirect, para uso exclusivo del gateway)
  - resto         → requiere sesion activa
      ↓
[Servicios :8082+] ← red interna
  - validan JWT contra JWKS del IDP
```

---

## El codigo del IDP explicado

### `KeyConfig.java` — La llave RSA

```java
KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
generator.initialize(2048);
KeyPair keyPair = generator.generateKeyPair();
```

Genera un par de llaves RSA al arrancar. La **clave privada** firma los JWTs internos.
La **clave publica** se expone en `/oauth2/jwks` para que el gateway pueda verificar
que los tokens son autenticos.

> Advertencia: Se genera en memoria → si reinicias el IDP, todos los tokens anteriores quedan invalidos.

---

### `AuthorizationServerConfig.java` — El servidor de autorizacion

El rol principal es proporcionar infraestructura JWT, no gestionar clientes OAuth2.

Lo que esta activo del Authorization Server:
- **`JwtEncoder`** bean: usado por `InternalTokenController` para firmar tokens.
- **`/oauth2/jwks`**: expone la clave publica RSA para que el gateway valide tokens.
- **`/.well-known/openid-configuration`**: discovery doc que el gateway descarga al arrancar.

`RegisteredClientRepository` esta vacio — el frontend usara PKCE cuando se implemente,
pero por ahora el sistema usa sesiones HTTP, no el auth-code flow estandar.

---

### `SecurityConfig.java` — Que rutas son publicas

```java
@Value("${app.gateway-url:http://localhost:8080}")
private String gatewayUrl;

.requestMatchers(
    "/.well-known/openid-configuration",
    "/oauth2/jwks",
    "/actuator/health",
    "/error"
).permitAll()
.anyRequest().authenticated()
.oauth2Login(oauth2 -> oauth2
    .successHandler(new FederatedIdentityAuthenticationSuccessHandler(gatewayUrl))
)
// NullRequestCache: evita guardar URLs como destino post-login
.requestCache(cache -> cache.requestCache(new NullRequestCache()))
// /internal/** devuelve 401 (no redirect al login) para uso exclusivo del gateway
.exceptionHandling(ex -> ex
    .defaultAuthenticationEntryPointFor(
        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
        request -> request.getServletPath().startsWith("/internal/")
    )
);
```

Hay dos `SecurityFilterChain`:
- **Order(1)** — `AuthorizationServerConfig`: protege los endpoints OAuth2 del AS (`/oauth2/jwks`, etc.)
- **Order(2)** — `SecurityConfig`: todo lo demas, incluyendo el login con Google y `/internal/token`

---

### `FederatedIdentityAuthenticationSuccessHandler.java` — El puente

Conecta la identidad de Google con la sesion local de Spring Security.
Redirige al gateway (no al IDP) despues del login exitoso.

```java
public FederatedIdentityAuthenticationSuccessHandler(String defaultTargetUrl) {
    SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler();
    handler.setDefaultTargetUrl(defaultTargetUrl);  // ej: http://localhost:8080/
    this.delegate = handler;
}

if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
    String email = oauthToken.getPrincipal().getAttribute("email");

    UsernamePasswordAuthenticationToken localAuth =
        UsernamePasswordAuthenticationToken.authenticated(
            email,      // el "username" del sistema sera el email de Google
            null,       // sin contrasena (ya autentico Google)
            AuthorityUtils.createAuthorityList("ROLE_USER")
        );

    SecurityContextHolder.getContext().setAuthentication(localAuth);
}

// Spring crea la sesion HTTP aqui y devuelve JSESSIONID al navegador
delegate.onAuthenticationSuccess(request, response, ...);
```

**Sin este handler:** Google autentica → Spring no sabe que hacer → error.
**Con este handler:** Google autentica → sesion local creada → el IDP puede emitir JWTs internos.

---

### `InternalTokenController.java` — El emisor de tokens internos

Solo lo llama el gateway. Requiere una sesion autenticada.
El parametro `Authentication` lo inyecta Spring MVC automaticamente desde el `SecurityContext`
de la sesion (via `HttpSessionSecurityContextRepository`).

```java
@GetMapping("/internal/token")
public ResponseEntity<Map<String, String>> issueToken(Authentication authentication) {
    JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuerUri)                    // debe coincidir con lo que valida el gateway
            .subject(authentication.getName())    // el email del usuario
            .issuedAt(now)
            .expiresAt(now.plusSeconds(300))      // 5 minutos — el gateway pide uno nuevo por request
            .claim("roles", ...)
            .build();

    String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    return ResponseEntity.ok(Map.of("token", token));
}
```

---

## El codigo del Gateway explicado

### `SessionToJwtFilter.java` — El puente sesion → JWT

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)  // corre ANTES que Spring Security
public class SessionToJwtFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Rutas del IDP → SKIP siempre.
        // Si se llamara a /internal/token durante el callback de Google, Spring Security
        // podria migrar la sesion (session fixation) y perder el OAuth2 state → error en login.
        if (exchange.getRequest().getPath().value().startsWith("/api/idp/")) {
            return chain.filter(exchange);
        }

        // Si ya tiene Bearer token, no hacer nada
        if (exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return chain.filter(exchange);
        }

        // Si tiene cookie de sesion, intercambiarla por JWT
        var sessionCookie = exchange.getRequest().getCookies().getFirst("JSESSIONID");
        if (sessionCookie == null) {
            return chain.filter(exchange);  // sin sesion ni token → Spring Security devolvera 401
        }

        return idpClient.get()
                .uri("/internal/token")
                .cookie("JSESSIONID", sessionCookie.getValue())
                .exchangeToMono(response -> ...)
                .flatMap(body -> {
                    var mutated = exchange.getRequest().mutate()
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                .onErrorResume(e -> chain.filter(exchange));  // si el IDP falla → 401 natural
    }
}
```

---

### `GatewaySecurityConfig.java` — Validacion del JWT

```java
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    .csrf(disable)
    .pathMatchers("/actuator/health").permitAll()
    .pathMatchers("/api/idp/**").permitAll()   // flujo de login: no requiere JWT
    .anyExchange().authenticated()
    .oauth2ResourceServer(oauth2 -> oauth2
        .jwt(jwt -> {})
        // BearerTokenServerAuthenticationEntryPoint falla con ReadOnlyHttpHeaders
        // en Spring Cloud Gateway al intentar escribir WWW-Authenticate.
        .authenticationEntryPoint((exchange, ex) -> {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        })
    );
}
```

Al arrancar, el gateway descarga `http://localhost:8081/.well-known/openid-configuration`
(o `http://identity-provider:8081` en Docker) para encontrar la URL del JWKS y obtener
la clave publica RSA. Cada JWT entrante se verifica con esa clave.

---

## Redirect post-login y cookies

El punto mas delicado de la arquitectura es que la cookie `JSESSIONID` debe estar fijada
en el mismo origen que el gateway (`localhost:8080`), no en el IDP (`localhost:8081`).

Esto funciona porque:
1. **Todo el flujo OAuth2 pasa por el gateway** (`/api/idp/**`).
2. La respuesta con `Set-Cookie: JSESSIONID=...` llega al browser proxeada por el gateway.
3. El browser fija la cookie en `localhost:8080`.
4. El redirect post-login apunta al gateway (`app.gateway-url`), no al IDP.

Si alguna de estas condiciones falla (ej. el usuario accede directamente a `localhost:8081`),
la cookie se fija en el puerto incorrecto y las requests al gateway no incluyen la sesion.

---

## Redirect "volver a donde ibas"

**No esta implementado aun.** El gateway es un resource server: devuelve 401 cuando no hay JWT,
no guarda la URL original ni redirige al login.

Cuando se implemente el frontend (Astro), el patron correcto es:
1. Frontend detecta 401 del gateway.
2. Guarda la URL original en `sessionStorage`.
3. Redirige al usuario a `http://localhost:8080/api/idp/oauth2/authorization/google`.
4. Tras el login, lee la URL guardada y navega alli.

---

## Resumen de los actores

| Componente | Rol | Puerto local |
|---|---|---|
| **Google** | Proveedor de identidad externo | (nube) |
| **nm-idp-service** | Autentica con Google, gestiona sesiones, emite JWTs internos via `/internal/token` | 8081 |
| **nm-api-gateway** | Intercambia sesion por JWT (`SessionToJwtFilter`), valida JWT, enruta | 8080 |
| **Astro frontend** | Redirige al login de Google, usa cookies de sesion para las llamadas a la API | 4321 |
| **Servicios backend** | Resource servers: reciben el JWT que el gateway inyecta, no saben nada de sesiones | 8082+ |

## Variables de entorno relevantes

| Variable | Servicio | Descripcion |
|---|---|---|
| `GOOGLE_CLIENT_ID` | IDP | Client ID de Google Cloud Console |
| `GOOGLE_CLIENT_SECRET` | IDP | Client Secret de Google Cloud Console |
| `GATEWAY_BASE_URL` | IDP | URL publica del gateway (default: `http://localhost:8080`) |
| `IDP_ISSUER_URI` | Gateway, App-service | URL del IDP para JWKS y token validation (default: `http://localhost:8081`) |
| `CONSUL_HOST` / `CONSUL_PORT` | Todos | Direccion del service registry |

## URLs registradas en Google Cloud Console

Para que el flujo funcione, estas URIs deben estar en **Authorized redirect URIs**:

```
http://localhost:8080/api/idp/login/oauth2/code/google   ← desarrollo local
```
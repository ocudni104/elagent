# Lo que debe implementar el frontend (Astro)

El frontend tiene que implementar 4 cosas para completar el flujo OAuth2 PKCE.

---

## 1. Boton "Login con GitHub"

Cuando el usuario hace click, el frontend genera el PKCE y redirige al IDP:

```
genera code_verifier (string aleatorio)
genera code_challenge (SHA256 del verifier, en base64url)
guarda code_verifier en sessionStorage

redirige a:
http://localhost:8081/oauth2/authorize
  ?client_id=nm-frontend
  &response_type=code
  &scope=openid
  &redirect_uri=http://localhost:4321/callback
  &code_challenge=<code_challenge>
  &code_challenge_method=S256
```

El navegador abandona la pagina y va al IDP. El IDP redirige a GitHub. El usuario se loguea.

---

## 2. Pagina `/callback`

Despues del login, GitHub redirige al IDP, y el IDP redirige aqui:

```
http://localhost:4321/callback?code=IDP_CODE
```

Esta pagina tiene que leer el `code` de la URL y hacer el intercambio:

```
POST http://localhost:8081/oauth2/token
  grant_type=authorization_code
  client_id=nm-frontend
  code=<IDP_CODE>
  redirect_uri=http://localhost:4321/callback
  code_verifier=<el que guardaste en sessionStorage>
```

El IDP responde con:

```json
{
  "access_token": "eyJ...",
  "refresh_token": "...",
  "id_token": "eyJ..."
}
```

---

## 3. Guardar el token

Guarda el `access_token` (y el `refresh_token`) en `localStorage` o en memoria,
segun la politica de seguridad que quieras.

---

## 4. Enviarlo en cada request al gateway

Cada llamada a la API tiene que incluir el header:

```
Authorization: Bearer <access_token>
```

Por ejemplo:

```
GET http://localhost:8080/api/workspace/...
Authorization: Bearer eyJ...
```

---

## Resumen del ciclo de vida

```
Usuario click "Login"
  → frontend genera PKCE → redirige al IDP

IDP → GitHub → usuario se autentica → vuelve al IDP
  → IDP redirige a /callback?code=XYZ

/callback recibe el code
  → hace POST /oauth2/token con code + code_verifier
  → guarda access_token

Cada request al gateway
  → Authorization: Bearer <access_token>
  → gateway valida → reenvía al servicio
```

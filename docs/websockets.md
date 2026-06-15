# WebSockets temps réel (Phase 5)

Notifications poussées en temps réel via **STOMP over WebSocket**, authentifiées par **JWT au
handshake**.

## Vue d'ensemble

```
Navigateur / client        Nginx (/ws/**)          App Spring (SimpleBroker)
  SockJS/STOMP   ──────►  upgrade WebSocket  ──────►  /ws  ──►  ChannelInterceptor (JWT)
        ▲                                                          │
        └──────────  /topic/notifications/{userId}  ◄─────────────┘  push après événement métier
```

- **Endpoint d'upgrade** : `/ws` (natif pour les clients Java/tests, **SockJS** pour le
  navigateur). Proxifié par Nginx (`location /ws/**`, headers `Upgrade`/`Connection`).
- **Broker** : `SimpleBroker` en mémoire sur le préfixe `/topic` (mono-instance — voir
  « Mise à l'échelle » plus bas).
- **Destinations** : `/topic/notifications/{userId}` (par utilisateur).
- **Heartbeats** : `10000 / 10000` ms (serveur), avec un `TaskScheduler` dédié.

## Authentification (JWT au CONNECT)

L'auth se fait **à l'ouverture de la connexion**, pas par frame. Le client envoie le token dans le
header natif STOMP du frame `CONNECT` :

```
CONNECT
Authorization: Bearer <jwt>
```

`JwtChannelInterceptor` (un `ChannelInterceptor` sur le canal entrant) intercepte le `CONNECT`,
valide le JWT (`JwtService`, HS256) et attache un `Principal` à la session. **Un token absent ou
invalide rejette la connexion.** (Phase 6 : RS256 + refresh tokens.)

## Événements métier → notifications

Les services publient des événements applicatifs (`ApplicationEventPublisher`) ; un
`NotificationListener` réagit **après commit** (`@TransactionalEventListener`), persiste la
notification en base **puis** la pousse via `SimpMessagingTemplate` :

| Événement | Déclencheur | Destinataire | Type de notif |
|---|---|---|---|
| `SessionCompletedEvent` | création d'une séance (`TrainingSessionService.create`) | chaque follower de l'auteur | `FRIEND_SESSION_COMPLETED` |
| `NewPrEvent` | charge ajoutée battant le max précédent (`addExercise`) | l'utilisateur lui-même | `NEW_PR` |

Persister **avant** de pousser garantit que la notif est récupérable via `GET /api/v1/notifications`
même si le client était hors ligne au moment du push.

## Client minimal

Page de démo : [`src/main/resources/static/notifications.html`](../src/main/resources/static/notifications.html)
(vanilla JS, SockJS + `@stomp/stompjs`). Flux : login REST → JWT → connexion STOMP avec
`connectHeaders.Authorization` → souscription à `/topic/notifications/{userId}` → affichage + bouton
« marquer comme lu » (`PATCH /api/v1/notifications/{id}/read`).

Extrait :

```js
const client = new StompJs.Client({
  webSocketFactory: () => new SockJS('/ws'),
  connectHeaders: { Authorization: 'Bearer ' + token },
  reconnectDelay: 5000,          // reconnexion automatique
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
  onConnect: () => client.subscribe('/topic/notifications/' + userId, msg => render(JSON.parse(msg.body))),
});
client.activate();
```

### Reconnexion

`@stomp/stompjs` gère la reconnexion automatique via `reconnectDelay`. Pour un backoff exponentiel,
augmenter le délai à chaque échec (ex. 1s, 2s, 4s… plafonné à 30s) dans `onWebSocketClose`. À la
reconnexion, le client re-souscrit et peut recharger l'historique via `GET /api/v1/notifications`
(pagination cursor) pour combler les notifs manquées hors ligne.

## Démo manuelle

1. `docker compose up -d` (ou `./mvnw spring-boot:run` + `docker compose up -d db`).
2. Ouvrir `http://localhost/notifications.html` (via Nginx) dans **deux** navigateurs.
3. Navigateur 1 : login user A. Navigateur 2 : login user B.
4. A suit B : `POST /api/v1/users/{idA}/follows` body `{ "followeeId": "<idB>" }` (avec le Bearer de A).
5. B crée une séance : `POST /api/v1/training-sessions` (avec le Bearer de B).
6. → A reçoit la notification `FRIEND_SESSION_COMPLETED` **en temps réel**.

Couper le réseau de l'onglet A puis le rétablir illustre la reconnexion automatique.

## Test automatisé

`NotificationWebSocketIT` (Testcontainers + `WebSocketStompClient`) : connecte un follower avec son
JWT, déclenche la création d'une séance par le followee et vérifie la réception du frame en < 5s.

## Mise à l'échelle multi-instances (prod)

Le `SimpleBroker` en mémoire ne diffuse qu'au sein d'une instance : si deux instances de l'app
tournent derrière Nginx, un message poussé par l'instance 1 n'atteint pas un client connecté à
l'instance 2. Deux options pour la prod :

1. **Relais STOMP externe** (RabbitMQ/ActiveMQ) : remplacer `enableSimpleBroker` par
   `enableStompBrokerRelay("/topic")`. Le broker devient le point de diffusion partagé.
2. **Pont Redis pub/sub** : publier chaque message sortant sur un canal Redis et, dans chaque
   instance, ré-émettre les messages reçus de Redis vers le `SimpleBroker` local. Redis 7 (déjà dans
   la stack Compose) sert alors de bus inter-instances. Plus léger que RabbitMQ mais à coder.

Pour ce projet (démo mono-instance), le `SimpleBroker` suffit ; le chemin Redis est documenté ici
comme évolution.

# SCRIBE — Contexte du client Android (pour le développement backend)

> Objet : décrire le **client Android natif** de SCRIBE et, surtout, **le contrat d'API**
> dont il dépend, afin que les évolutions futures du serveur SCRIBE restent **compatibles**
> et exposent ce qu'il faut pour la **parité mobile**.
> Client : `com.scribe.app` — Kotlin / Jetpack Compose / Retrofit. minSdk 29, target 35.

---

## 1. Architecture du client

- **UI** : Jetpack Compose (Material 3), thème DSFR (bleu `#003189`, rouge `#e1000f`), mode clair forcé.
- **Réseau** : Retrofit + OkHttp + Moshi (réflexif). Un `AuthInterceptor` ajoute
  `Authorization: Bearer <JWT>` sur toutes les requêtes **sauf** `/auth/login` et `/mfa/`.
- **URL d'instance dynamique** : saisie au login (sans schéma) ; `ApiProvider` reconstruit Retrofit
  pour l'URL choisie. Bascule http/https. Chaque hôpital = une instance.
- **Sécurité** : JWT chiffré AES‑256‑GCM via **Android Keystore** ; `FLAG_SECURE` (pas de capture
  d'écran) ; logs réseau sans corps ; `allowBackup=false`. (Exigences HDS/RGPD.)
- **Navigation** : un écran `Home` (tiroir latéral) hébergeant les sections ; détail incident et
  création d'incident sont des routes empilées ; la touche **Retour** revient à la vue précédente
  puis au Tableau de bord (ne quitte l'app que depuis le Tableau de bord).
- **Moshi ignore les champs JSON inconnus** : le serveur peut ajouter des champs sans casser le client.

---

## 2. Contrat d'API consommé (ne pas casser)

Tous les chemins sont relatifs à `https://<instance>/`. Réponses en **snake_case**.
Le client ne lit que les champs listés ; les autres sont ignorés.

### Authentification
| Méthode | Chemin | Corps / Réponse | Notes |
|---|---|---|---|
| POST | `/api/v1/auth/login` | `{username, password}` → `{token, user{…}}` **OU** `{require_mfa:true, mfa_token, username}` | **Le client gère la bifurcation MFA.** Ne pas supprimer le champ `require_mfa`/`mfa_token`. |
| POST | `/api/v1/mfa/verify` | `{mfa_token, code}` → `{token, user{…}}` | |
| GET | `/api/v1/auth/me` | → user | Sert à valider le token au démarrage. |
| GET | `/api/v1/auth/annuaire-messagerie` | → `[{id, username, display_name, service, role, site_tag, online, inactivity_label}]` | Carnet d'adresses (destinataires + présence). |

### Incidents (main courante)
| Méthode | Chemin | Notes |
|---|---|---|
| GET | `/api/v1/sitrep/history` | **Liste** des incidents (PAS `/sitrep`). Champs lus : `id, timestamp, declarant_nom, directeur_crise, site_id, unite_fonctionnelle, type_crise, urgency(1‑4), fait, analyse, status, completion_percent, impact_fonctionnel, jalons`. |
| POST | `/api/v1/sitrep/post` | Création. Obligatoires : `declarant_nom, site_id, fait`. |
| PUT | `/api/v1/sitrep/{id}/status` | `{status, completion_percent?}`. Passage en `RÉSOLU` exige au moins un jalon validé. |
| PUT | `/api/v1/sitrep/{id}/jalons` | `{jalons:[{label, done, done_at}]}`. `jalons` est renvoyé par l'API **sous forme de chaîne JSON** — le client la parse. |
| GET | `/api/v1/sitrep/stats` | `{total, critical, ouverts, cyber, sanitaire}` (tableau de bord). |

### Communiqués / statut
| Méthode | Chemin | Notes |
|---|---|---|
| GET | `/api/v1/status/current` | `{site_nom, niveau_global, message_public, updated_at, updated_by, services_si:[{id,label,statut}], prise_en_charge:[{id,label,statut}], faq:[{question,reponse,visible}], chronologie:[{ts,texte,…}]}`. Le client affiche toutes les FAQ ayant une question (la FAQ par défaut est `visible:false`). |

### Capacité / Soins
| Méthode | Chemin | Notes |
|---|---|---|
| GET | `/api/v1/capacite/synthese` | `Map<site, Map<pole, {lits_total, lits_vides_h/f/i, alertes, non_declares, statut_pole, services[…]}>>`. Sert à **Soins** (synthèse, triée par criticité) et au compteur « secteurs impactés ». |
| GET | `/api/v1/capacite/referentiel` | `[{id, service_nom, uf_code, pole, site, capacite_totale, statut_global, derniere_declaration{…}}]`. |
| POST | `/api/v1/capacite/declaration` | `{referentiel_id, redacteur, point, lits_vides_h/f/i, statut_lits/rh/materiel, alerte_*, commentaire_general, …}`. Crée un incident si une alerte est cochée (comportement attendu et reproduit dans l'UI). |

### Messagerie
| Méthode | Chemin | Notes |
|---|---|---|
| GET | `/api/v1/messagerie` | `[{id, sujet, contenu, expediteur_id, expediteur_nom, destinataire_id, destinataire_nom, lu, reply_to, created_at, is_mine}]`. |
| POST | `/api/v1/messagerie` | `{destinataire_id, sujet, contenu, reply_to?}`. **Le client n'exploite pas la réponse** (il recharge la liste) — voir §3. |
| PUT | `/api/v1/messagerie/{id}/lire` | Marquer lu (ouverture d'un message, « tout marquer comme lu »). |
| GET | `/api/v1/messagerie/non-lus` | `{count}` (badge + tableau de bord). |

### Chat
| Méthode | Chemin | Notes |
|---|---|---|
| GET | `/api/v1/chat/salons` | `[{id, nom, description, icone, type}]` — renvoie **tous** les salons non archivés. |
| GET | `/api/v1/chat/salons/{id}/messages` | `[{id, auteur_nom, auteur_sigle, contenu, horodatage}]`. Polling 5 s quand un salon est ouvert. |
| POST | `/api/v1/chat/salons/{id}/messages` | `{contenu}`. |
| POST | `/api/v1/chat/salons` | `{nom, description?, couleur?, icone?, type}` — **le serveur normalise `nom`** (minuscules, espaces→tirets) ; le client en tient compte. Utilisé pour les « salons privés » 1‑à‑1 (type `local`). |
| GET | `/api/v1/chat/presence` | `Map<sigle, [{user_id, display_name}]>` — utilisateurs en ligne (≤5 min). |
| POST | `/api/v1/chat/presence/ping` | Signale la présence (envoyé à l'ouverture d'un salon et au polling). |

### Kanban / Cellule / Transferts / Brancardage
| Méthode | Chemin | Notes |
|---|---|---|
| GET | `/api/v1/tasks/` | `[{id, titre, description, assignee, priorite, colonne, incident_id}]`. Colonnes : `BACKLOG, EN_COURS, EN_ATTENTE, TERMINÉ`. |
| PUT | `/api/v1/tasks/{id}/move` | `{colonne}`. |
| GET | `/api/v1/cellule/decisions` | `[{id, timestamp, contenu, responsable, base_reglementaire, statut_validation}]`. |
| POST | `/api/v1/cellule/decisions` | `{contenu, responsable?, base_reglementaire?}`. |
| GET | `/api/v1/transferts` | Liste des transferts actifs (route servie par `v140`). Le client ne mappe **que** les champs non‑nominatifs : `id, unite_origine, etablissement_origine, unite_destination, etablissement_destination, statut, horodatage_depart` (jamais nom/prénom/IPP). |
| PATCH | `/api/v1/transferts/{id}/statut` | `{statut, reason?}`. Statuts : `EN_PREPARATION, EN_COURS, ARRIVE, ANNULE`. Un **recul** exige `reason`. |
| GET | `/api/v1/brancardage/missions` | Missions actives. Champs lus : `id, ref_patient, uf_origine, chambre_depart, uf_destination, chambre_arrivee, type_transport, priorite_label, motif, statut, statut_label, agent_nom`. |
| POST | `/api/v1/brancardage/missions/{id}/prendre_en_charge` | `{agent_nom, agent_tel?}` (EN_ATTENTE→EN_COURS). |
| POST | `/api/v1/brancardage/missions/{id}/arrivee` | `{commentaire?}` (→TERMINE). |
| PATCH | `/api/v1/brancardage/missions/{id}` | `{statut, commentaire?}`. Statuts : `EN_ATTENTE, EN_COURS, TERMINE, ANNULE`. |

---

## 3. Points d'attention rencontrés (à préserver côté serveur)

1. **Liste des incidents = `/sitrep/history`** (et non `/sitrep`). Ne pas renommer.
2. **Login MFA** : la réponse du login peut être soit le token, soit la bifurcation MFA.
   Garder `require_mfa` / `mfa_token`.
3. **`jalons` renvoyé en chaîne JSON** dans l'incident : le client parse une chaîne, pas un tableau.
   Si un jour c'est typé tableau, prévenir (changement de contrat).
4. **Transferts** : la route `/api/v1/transferts/anonymes` **renvoie 405** sur l'instance (préfixe
   doublé du plugin + route absente côté `v140`). Le client est donc passé sur **`GET /api/v1/transferts`**.
   → *Recommandation* : exposer **une route de lecture anonymisée stable** (sans données patient),
   p.ex. `GET /api/v1/transferts/public`, pour que le mobile n'ait jamais à recevoir de nominatif.
5. **POST `/messagerie`** : la forme de la réponse a provoqué une erreur de désérialisation
   (`Required value 'id' missing`). Le client **ignore désormais le corps** et recharge la liste.
   → *Recommandation* : renvoyer systématiquement l'objet message complet avec `id`.
6. **Chat — création de salon** : le serveur **normalise** le `nom` (minuscules, espaces→tirets).
   Le client recalcule ce nom normalisé pour retrouver/ouvrir le salon créé.
7. **Chat — type de salon** : un salon `type:"prive"` n'était **pas affiché** par le front web.
   Le client crée donc les salons 1‑à‑1 en `type:"local"`. → *Recommandation* : si un vrai mode
   **privé** (DM avec contrôle d'accès) est souhaité, l'ajouter explicitement côté serveur
   (membres + visibilité), sinon documenter que les salons sont visibles de tous.
8. **Présence** : OK au sein d'une instance. **Les salons ad‑hoc ne sont pas fédérés** entre
   établissements → un salon créé sur l'instance A n'apparaît pas sur l'instance B (par design).

---

## 4. Ce que le backend doit AJOUTER pour la parité mobile

Fonctionnalités demandées côté mobile mais **non réalisables sans évolution serveur** :

1. **Archiver / supprimer un message** (long‑press dans la boîte) : **aucune route** n'existe.
   → Ajouter `DELETE /api/v1/messagerie/{id}` et/ou `PUT /api/v1/messagerie/{id}/archive`.
2. **Notifications push (app fermée)** : le serveur fait du **Web Push / VAPID**, pas de **FCM**.
   Pour notifier message reçu / **mention `@`** dans le chat / **assignation** d'une tâche ou d'un
   incident, prévoir :
   - `POST /api/v1/devices/register-fcm-token` (enregistrement du token FCM par appareil) ;
   - envoi FCM côté serveur sur les événements (nouveau message, mention, assignation).
   (En attendant, le mobile peut faire des **notifications locales** uniquement app ouverte, via polling.)
3. **Mentions chat** : exposer/normaliser les mentions (`@user`) pour pouvoir notifier le mentionné.
4. **Salon privé natif** (optionnel) : DM 1‑à‑1 avec contrôle d'accès, ou fédération des salons ad‑hoc
   si l'usage inter‑établissements est voulu.
5. **i18n de l'app** : les libellés mobiles sont en français en dur. Pour le multilingue, fournir la
   liste des langues disponibles (`/api/v1/i18n`) et, idéalement, un jeu de clés stable que le mobile
   pourra mapper sur ses ressources `values-xx/`.

---

## 5. Recommandations de compatibilité (règles d'or)

- **Ne pas renommer** les chemins ni les champs JSON existants listés au §2 ; ajouter plutôt de nouveaux champs.
- **Garder le snake_case** dans les réponses.
- **Versionner** les changements de contrat (`/api/v2/…`) plutôt que modifier `/api/v1` en place.
- **Préfixes cohérents** : éviter les doubles préfixes de plugin (cas `transferts`) qui rendent des
  routes inatteignables.
- **Toujours renvoyer un `id`** dans les objets créés (POST), pour la désérialisation mobile.
- **Toute donnée nominative patient** doit avoir une route de lecture **anonymisée** dédiée au mobile.

---

*Document généré pour accompagner le client Android `com.scribe.app`. À mettre à jour à chaque
évolution du contrat d'API.*

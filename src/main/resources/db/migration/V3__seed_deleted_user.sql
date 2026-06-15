-- =====================================================================
-- FitTracker V3 - User sentinelle "deleted" pour RGPD.
-- Les training_sessions des utilisateurs supprimes sont reassignees a
-- ce user (UUID ...000) pour conserver les agregats statistiques de
-- maniere anonyme. Il est cree deja soft-supprime (deleted_at = now())
-- pour qu'aucune route applicative ne le retourne.
-- NB : distinct du user de test ...001 (CurrentUserProvider.TEST_USER_ID).
-- =====================================================================

INSERT INTO users (id, email, password_hash, display_name, created_at, updated_at, deleted_at)
VALUES (
    '00000000-0000-0000-0000-000000000000',
    'deleted-sentinel@fittracker.invalid',
    'N/A',
    '[deleted user]',
    now(),
    now(),
    now()
);

-- Dev seed: creates players rohernandez@gmail.com can review.
-- All seeded users share completed past matches together.
-- Password for all seeded users: password123

-- ── Users ─────────────────────────────────────────────────────────────────────

INSERT INTO users (username, name, last_name, email, role, password_hash, email_verified_at, preferred_language)
VALUES
    ('rohernandez',   'Rodrigo',  'Hernandez', 'rohernandez@gmail.com',    'user', '$2a$10$gMs.ApWgzNTjrWoWcRs4SOd6A3h7mcnaQxbUfnZwRSYiLkf8d0P/C', NOW(), 'es'),
    ('cgonzalez',     'Carlos',   'González',  'cgonzalez@gmail.com',      'user', '$2a$10$gMs.ApWgzNTjrWoWcRs4SOd6A3h7mcnaQxbUfnZwRSYiLkf8d0P/C', NOW(), 'es'),
    ('mperez',        'María',    'Pérez',     'mperez@gmail.com',         'user', '$2a$10$gMs.ApWgzNTjrWoWcRs4SOd6A3h7mcnaQxbUfnZwRSYiLkf8d0P/C', NOW(), 'es'),
    ('jmartinez',     'Juan',     'Martínez',  'jmartinez@gmail.com',      'user', '$2a$10$gMs.ApWgzNTjrWoWcRs4SOd6A3h7mcnaQxbUfnZwRSYiLkf8d0P/C', NOW(), 'es'),
    ('alopez',        'Ana',      'López',     'alopez@gmail.com',         'user', '$2a$10$gMs.ApWgzNTjrWoWcRs4SOd6A3h7mcnaQxbUfnZwRSYiLkf8d0P/C', NOW(), 'es'),
    ('psanchez',      'Pedro',    'Sánchez',   'psanchez@gmail.com',       'user', '$2a$10$gMs.ApWgzNTjrWoWcRs4SOd6A3h7mcnaQxbUfnZwRSYiLkf8d0P/C', NOW(), 'es')
ON CONFLICT (email) DO NOTHING;

-- ── Matches (completed, all in the past) ──────────────────────────────────────

INSERT INTO matches (host_user_id, sport, address, title, description,
                     starts_at, ends_at, max_players, price_per_player,
                     visibility, join_policy, status)
VALUES
    (
        (SELECT id FROM users WHERE email = 'cgonzalez@gmail.com'),
        'football',
        'Av. del Libertador 5000, Buenos Aires',
        'Fútbol del sábado',
        'Partido de fútbol 6 vs 6 en cancha de césped sintético.',
        NOW() - INTERVAL '14 days',
        NOW() - INTERVAL '14 days' + INTERVAL '90 minutes',
        12, 0, 'public', 'direct', 'completed'
    ),
    (
        (SELECT id FROM users WHERE email = 'rohernandez@gmail.com'),
        'basketball',
        'Parque Centenario, Av. Ángel Gallardo 490, Buenos Aires',
        'Básquet en el parque',
        'Partido de básquet al aire libre, llevate agua.',
        NOW() - INTERVAL '7 days',
        NOW() - INTERVAL '7 days' + INTERVAL '60 minutes',
        10, 0, 'public', 'direct', 'completed'
    ),
    (
        (SELECT id FROM users WHERE email = 'jmartinez@gmail.com'),
        'padel',
        'Club Náutico, Costanera Norte, Buenos Aires',
        'Pádel mixto',
        'Partido de pádel mixto, todos los niveles bienvenidos.',
        NOW() - INTERVAL '3 days',
        NOW() - INTERVAL '3 days' + INTERVAL '90 minutes',
        8, 500, 'public', 'direct', 'completed'
    );

-- ── Participants ───────────────────────────────────────────────────────────────
-- Match 1 (football): all 6 users

INSERT INTO match_participants (match_id, user_id, status, scope)
SELECT m.id, u.id, 'joined', 'match'
FROM matches m, users u
WHERE m.title = 'Fútbol del sábado'
  AND u.email IN (
      'rohernandez@gmail.com',
      'cgonzalez@gmail.com',
      'mperez@gmail.com',
      'jmartinez@gmail.com',
      'alopez@gmail.com',
      'psanchez@gmail.com'
  )
ON CONFLICT (match_id, user_id) DO NOTHING;

-- Match 2 (basketball): rohernandez, cgonzalez, alopez, psanchez

INSERT INTO match_participants (match_id, user_id, status, scope)
SELECT m.id, u.id, 'joined', 'match'
FROM matches m, users u
WHERE m.title = 'Básquet en el parque'
  AND u.email IN (
      'rohernandez@gmail.com',
      'cgonzalez@gmail.com',
      'alopez@gmail.com',
      'psanchez@gmail.com'
  )
ON CONFLICT (match_id, user_id) DO NOTHING;

-- Match 3 (padel): rohernandez, mperez, jmartinez, alopez

INSERT INTO match_participants (match_id, user_id, status, scope)
SELECT m.id, u.id, 'joined', 'match'
FROM matches m, users u
WHERE m.title = 'Pádel mixto'
  AND u.email IN (
      'rohernandez@gmail.com',
      'mperez@gmail.com',
      'jmartinez@gmail.com',
      'alopez@gmail.com'
  )
ON CONFLICT (match_id, user_id) DO NOTHING;

-- ── Reviews between other players (leaving rohernandez's reviews blank) ────────

INSERT INTO player_reviews (reviewer_user_id, reviewed_user_id, reaction, comment)
VALUES
    (
        (SELECT id FROM users WHERE email = 'cgonzalez@gmail.com'),
        (SELECT id FROM users WHERE email = 'mperez@gmail.com'),
        'like',
        'Excelente jugadora, siempre puntual y con buen espíritu deportivo.'
    ),
    (
        (SELECT id FROM users WHERE email = 'mperez@gmail.com'),
        (SELECT id FROM users WHERE email = 'cgonzalez@gmail.com'),
        'like',
        'Muy buen jugador, organiza bien los partidos.'
    ),
    (
        (SELECT id FROM users WHERE email = 'jmartinez@gmail.com'),
        (SELECT id FROM users WHERE email = 'alopez@gmail.com'),
        'like',
        'Muy agradable, buen nivel de pádel.'
    ),
    (
        (SELECT id FROM users WHERE email = 'alopez@gmail.com'),
        (SELECT id FROM users WHERE email = 'psanchez@gmail.com'),
        'like',
        'Buen compañero de equipo.'
    ),
    (
        (SELECT id FROM users WHERE email = 'psanchez@gmail.com'),
        (SELECT id FROM users WHERE email = 'cgonzalez@gmail.com'),
        'dislike',
        'Llegó tarde al partido y no avisó.'
    )
ON CONFLICT (reviewer_user_id, reviewed_user_id) DO NOTHING;

-- ── Sport ratings (ELO) ───────────────────────────────────────────────────────

INSERT INTO user_sport_ratings (user_id, sport, elo)
VALUES
    ((SELECT id FROM users WHERE email = 'rohernandez@gmail.com'),  'football',   1180),
    ((SELECT id FROM users WHERE email = 'rohernandez@gmail.com'),  'basketball', 1050),
    ((SELECT id FROM users WHERE email = 'rohernandez@gmail.com'),  'padel',       990),
    ((SELECT id FROM users WHERE email = 'cgonzalez@gmail.com'),    'football',   1250),
    ((SELECT id FROM users WHERE email = 'cgonzalez@gmail.com'),    'basketball', 1100),
    ((SELECT id FROM users WHERE email = 'mperez@gmail.com'),       'football',    980),
    ((SELECT id FROM users WHERE email = 'mperez@gmail.com'),       'padel',      1080),
    ((SELECT id FROM users WHERE email = 'jmartinez@gmail.com'),    'padel',      1320),
    ((SELECT id FROM users WHERE email = 'jmartinez@gmail.com'),    'football',   1010),
    ((SELECT id FROM users WHERE email = 'alopez@gmail.com'),       'football',   1150),
    ((SELECT id FROM users WHERE email = 'alopez@gmail.com'),       'padel',      1200),
    ((SELECT id FROM users WHERE email = 'alopez@gmail.com'),       'basketball',  920),
    ((SELECT id FROM users WHERE email = 'psanchez@gmail.com'),     'football',   1090),
    ((SELECT id FROM users WHERE email = 'psanchez@gmail.com'),     'basketball', 1300)
ON CONFLICT (user_id, sport) DO NOTHING;

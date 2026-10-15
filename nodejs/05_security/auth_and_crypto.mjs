/**
 * Authentication and crypto with node:crypto only - including a JWT implemented from scratch.
 *
 * Run: node nodejs/05_security/auth_and_crypto.mjs
 *
 * REMEMBER:
 *  * NEVER store plaintext passwords, and never hash them with md5/sha256 - those are FAST, which is
 *    exactly what an attacker wants. Use a slow KDF: scrypt / bcrypt / argon2, with a per-user SALT.
 *  * Compare secrets with timingSafeEqual, never ===, to avoid timing attacks.
 *  * JWT = base64url(header).base64url(payload).signature - the payload is only ENCODED, not encrypted.
 *    Anyone can read it: never put secrets in it. Always verify `alg`, `exp`, and the signature.
 *  * Sessions vs JWT: sessions are revocable server state; JWTs are stateless but can't be revoked
 *    before they expire (mitigate with short expiry + refresh tokens + a deny list).
 *  * Use randomUUID()/randomBytes() for tokens - Math.random() is NOT cryptographically secure.
 */
import {
  createHmac,
  randomBytes,
  randomUUID,
  scrypt,
  timingSafeEqual,
  createCipheriv,
  createDecipheriv,
} from 'node:crypto';
import { promisify } from 'node:util';

const scryptAsync = promisify(scrypt);

function check(label, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(44)} ${a}`);
}

// ---------------------------------------------------------------- password hashing (scrypt)

async function hashPassword(password) {
  const salt = randomBytes(16); // unique per user: stops rainbow tables and makes identical passwords differ
  const derived = await scryptAsync(password.normalize('NFKC'), salt, 64);
  return `scrypt$${salt.toString('base64url')}$${derived.toString('base64url')}`;
}

async function verifyPassword(password, stored) {
  const [scheme, saltB64, hashB64] = stored.split('$');
  if (scheme !== 'scrypt') return false;
  const salt = Buffer.from(saltB64, 'base64url');
  const expected = Buffer.from(hashB64, 'base64url');
  const actual = await scryptAsync(password.normalize('NFKC'), salt, expected.length);
  return actual.length === expected.length && timingSafeEqual(actual, expected); // constant time
}

// ---------------------------------------------------------------- JWT (HS256) from scratch

const base64url = (input) => Buffer.from(input).toString('base64url');
const fromBase64url = (input) => Buffer.from(input, 'base64url').toString('utf8');

function signJwt(payload, secret, { expiresInSeconds = 900 } = {}) {
  const header = { alg: 'HS256', typ: 'JWT' };
  const now = Math.floor(Date.now() / 1000);
  const body = { ...payload, iat: now, exp: now + expiresInSeconds, jti: randomUUID() };
  const data = `${base64url(JSON.stringify(header))}.${base64url(JSON.stringify(body))}`;
  const signature = createHmac('sha256', secret).update(data).digest('base64url');
  return `${data}.${signature}`;
}

function verifyJwt(token, secret, { now = Math.floor(Date.now() / 1000) } = {}) {
  const parts = token.split('.');
  if (parts.length !== 3) throw new Error('malformed token');
  const [headerB64, payloadB64, signature] = parts;

  const header = JSON.parse(fromBase64url(headerB64));
  // CRITICAL: pin the algorithm. Accepting `alg` from the token allows the classic
  // "alg: none" and RS256->HS256 confusion attacks.
  if (header.alg !== 'HS256') throw new Error('unexpected algorithm');

  const expected = createHmac('sha256', secret).update(`${headerB64}.${payloadB64}`).digest();
  const provided = Buffer.from(signature, 'base64url');
  if (expected.length !== provided.length || !timingSafeEqual(expected, provided)) {
    throw new Error('invalid signature');
  }

  const payload = JSON.parse(fromBase64url(payloadB64));
  if (typeof payload.exp === 'number' && payload.exp < now) throw new Error('token expired');
  return payload;
}

// ---------------------------------------------------------------- opaque session tokens (the alternative)

const sessions = new Map();

function createSession(userId, { ttlMs = 60_000 } = {}) {
  const token = randomBytes(32).toString('base64url'); // opaque, unguessable, revocable
  sessions.set(token, { userId, expiresAt: Date.now() + ttlMs });
  return token;
}

function readSession(token) {
  const session = sessions.get(token);
  if (!session) return null;
  if (session.expiresAt < Date.now()) {
    sessions.delete(token);
    return null;
  }
  return session;
}

const revokeSession = (token) => sessions.delete(token); // <- what a JWT cannot do

// ---------------------------------------------------------------- encryption at rest (AES-256-GCM)

function encrypt(plaintext, key) {
  const iv = randomBytes(12); // NEVER reuse an IV with the same key
  const cipher = createCipheriv('aes-256-gcm', key, iv);
  const ciphertext = Buffer.concat([cipher.update(plaintext, 'utf8'), cipher.final()]);
  return { iv: iv.toString('base64'), tag: cipher.getAuthTag().toString('base64'), data: ciphertext.toString('base64') };
}

function decrypt({ iv, tag, data }, key) {
  const decipher = createDecipheriv('aes-256-gcm', key, Buffer.from(iv, 'base64'));
  decipher.setAuthTag(Buffer.from(tag, 'base64')); // GCM also AUTHENTICATES: tampering throws
  return Buffer.concat([decipher.update(Buffer.from(data, 'base64')), decipher.final()]).toString('utf8');
}

// ---------------------------------------------------------------- webhook signature verification

function signWebhook(body, secret, timestamp) {
  return createHmac('sha256', secret).update(`${timestamp}.${body}`).digest('hex');
}

function verifyWebhook(body, secret, timestamp, signature, { toleranceSeconds = 300, now = Date.now() } = {}) {
  // Replay protection: reject old timestamps even if the signature is valid
  if (Math.abs(now / 1000 - timestamp) > toleranceSeconds) return false;
  const expected = Buffer.from(signWebhook(body, secret, timestamp), 'hex');
  const provided = Buffer.from(signature, 'hex');
  return expected.length === provided.length && timingSafeEqual(expected, provided);
}

// ---------------------------------------------------------------- input safety helpers

const escapeHtml = (value) =>
  String(value).replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]);

/** Parameterized query placeholder - what stops SQL injection (never concatenate user input). */
const buildQuery = (email) => ({ text: 'SELECT * FROM users WHERE email = $1', values: [email] });

const SECRET = 'a-very-secret-key-from-env-not-source-control';

const stored = await hashPassword('correct horse battery staple');
check('hash format', stored.split('$')[0], 'scrypt');
check('same password verifies', await verifyPassword('correct horse battery staple', stored), true);
check('wrong password rejected', await verifyPassword('wrong password', stored), false);
const stored2 = await hashPassword('correct horse battery staple');
check('same password -> different hash (salt)', stored === stored2, false);

const token = signJwt({ sub: 'user-42', role: 'admin' }, SECRET, { expiresInSeconds: 60 });
const payload = verifyJwt(token, SECRET);
check('JWT round trip', [payload.sub, payload.role], ['user-42', 'admin']);
check('JWT payload is readable by anyone', JSON.parse(fromBase64url(token.split('.')[1])).sub, 'user-42');

const tampered = `${token.split('.')[0]}.${base64url(JSON.stringify({ sub: 'user-42', role: 'superadmin' }))}.${token.split('.')[2]}`;
check('tampered payload rejected', (() => { try { verifyJwt(tampered, SECRET); return 'accepted'; } catch (e) { return e.message; } })(),
      'invalid signature');
check('wrong secret rejected', (() => { try { verifyJwt(token, 'other-secret'); return 'accepted'; } catch (e) { return e.message; } })(),
      'invalid signature');

const noneAlg = `${base64url(JSON.stringify({ alg: 'none', typ: 'JWT' }))}.${base64url(JSON.stringify({ sub: 'attacker' }))}.`;
check('alg:none attack rejected', (() => { try { verifyJwt(noneAlg, SECRET); return 'accepted'; } catch (e) { return e.message; } })(),
      'unexpected algorithm');

const expired = signJwt({ sub: 'user-1' }, SECRET, { expiresInSeconds: -10 });
check('expired token rejected', (() => { try { verifyJwt(expired, SECRET); return 'accepted'; } catch (e) { return e.message; } })(),
      'token expired');

const sessionToken = createSession('user-42');
check('session lookup', readSession(sessionToken)?.userId, 'user-42');
revokeSession(sessionToken);
check('session revoked immediately', readSession(sessionToken), null);

const key = randomBytes(32);
const encrypted = encrypt('card-token-abc123', key);
check('AES-GCM round trip', decrypt(encrypted, key), 'card-token-abc123');
check('tampering detected', (() => {
  const broken = { ...encrypted, data: Buffer.from('tampered').toString('base64') };
  try { decrypt(broken, key); return 'accepted'; } catch { return 'rejected'; }
})(), 'rejected');

const nowSeconds = Math.floor(Date.now() / 1000);
const webhookBody = JSON.stringify({ event: 'payment.succeeded' });
const webhookSignature = signWebhook(webhookBody, SECRET, nowSeconds);
check('valid webhook accepted', verifyWebhook(webhookBody, SECRET, nowSeconds, webhookSignature), true);
check('replayed old webhook rejected', verifyWebhook(webhookBody, SECRET, nowSeconds - 3600, webhookSignature), false);
check('modified webhook body rejected', verifyWebhook('{"event":"refund"}', SECRET, nowSeconds, webhookSignature), false);

check('escapeHtml', escapeHtml('<script>alert("x")</script>'),
      '&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;');
check('parameterized query', buildQuery("' OR 1=1 --"), { text: 'SELECT * FROM users WHERE email = $1', values: ["' OR 1=1 --"] });
check('randomUUID is unguessable', new Set([randomUUID(), randomUUID(), randomUUID()]).size, 3);

console.log('\nall checks passed');

/**
 * SECURITY CHECKLIST FOR A NODE SERVICE
 *  * helmet (security headers), CORS allow-list, HTTPS only, HSTS
 *  * Validate every input at the boundary (zod/joi); reject unknown fields
 *  * Parameterized queries / an ORM - never string-concatenate SQL
 *  * Rate limit auth endpoints; lock accounts after repeated failures; add CAPTCHA
 *  * Secrets from env/a secret manager; never in git; rotate them
 *  * npm audit / Dependabot; lockfile committed; `npm ci` in CI
 *  * Don't run as root; drop capabilities; read-only filesystem in the container
 *  * Log with a request id; never log tokens, passwords or full card numbers
 *  * Set body size limits and timeouts (slowloris); use a reverse proxy
 */

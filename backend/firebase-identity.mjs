// Supply an official Firebase Admin Auth instance; never decode a JWT as authentication.
export function createFirebaseIdentity(auth, { projectId, now = Date.now }) {
  if (!/^[a-z][a-z0-9-]{4,62}$/.test(projectId ?? '')
      || typeof auth?.verifyIdToken !== 'function' || typeof auth?.getUser !== 'function')
    throw new Error('invalid_identity_config');
  return {
    async verify(token, recent = false) {
      if (typeof token !== 'string' || token.length < 32 || token.length > 8192)
        throw new Error('unauthorized');
      let claims;
      try { claims = await auth.verifyIdToken(token, true); }
      catch { throw new Error('unauthorized'); }
      const seconds = Math.floor(now() / 1000);
      if (claims?.aud !== projectId || claims?.iss !== `https://securetoken.google.com/${projectId}`
          || typeof claims.sub !== 'string' || claims.sub.length < 1 || claims.sub.length > 128
          || claims.uid !== claims.sub || claims.email_verified !== true
          || !Number.isSafeInteger(claims.exp) || claims.exp <= seconds
          || !Number.isSafeInteger(claims.auth_time) || claims.auth_time > seconds
          || (recent && seconds - claims.auth_time > 300)) throw new Error('unauthorized');
      return { subject: claims.sub };
    },
    async assertActive(subject, grantedAt) {
      try {
        const user = await auth.getUser(subject);
        if (user.uid !== subject || user.disabled || user.emailVerified !== true) throw new Error();
        const validAfter = Date.parse(user.tokensValidAfterTime);
        if (!Number.isFinite(validAfter) || !Number.isSafeInteger(grantedAt)
            || validAfter > grantedAt) throw new Error();
      } catch { throw new Error('unauthorized'); }
    }
  };
}

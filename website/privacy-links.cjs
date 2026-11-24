const providerPolicies = new Set([
  'https://alquran.cloud/terms-and-conditions#privacy',
  'https://api-docs.quran.com/legal/developer-privacy/',
  'https://firebase.google.com/support/privacy',
  'https://upstash.com/trust/privacy',
  'https://vercel.com/legal/privacy-notice',
  'https://www.cloudflare.com/privacypolicy/',
]);

module.exports = function withoutProviderLinks(name, html) {
  if (name !== 'privacy.html') return html;
  return html.replace(/<a href="([^"]+)">/g, (tag, url) =>
    providerPolicies.has(url) ? '<a>' : tag);
};

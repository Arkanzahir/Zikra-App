import handler from '../quran-sync-handler.mjs';
export default function quran(req, res) {
  const query = req.url.includes('?') ? req.url.slice(req.url.indexOf('?')) : '';
  req.url = '/v1/quran' + query;
  return handler(req, res);
}

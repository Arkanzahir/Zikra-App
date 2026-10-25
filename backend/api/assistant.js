import handler from '../vercel-handler.mjs';
export default function assistant(req, res) { req.url = '/v1/assistant'; return handler(req, res); }

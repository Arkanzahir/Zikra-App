import handler from '../vercel-handler.mjs';
export default function health(req, res) { req.url = '/health'; return handler(req, res); }

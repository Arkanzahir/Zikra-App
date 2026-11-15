import handler from '../vercel-handler.mjs';
export default function answerReports(req, res) { req.url = '/v1/answer-reports'; return handler(req, res); }

import handler from '../vercel-handler.mjs';
export default function capabilities(req,res){req.url = '/v1/capabilities';return handler(req,res);}

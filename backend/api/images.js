import handler from '../vercel-handler.mjs';
export default function images(req,res){req.url = '/v1/images';return handler(req,res);}

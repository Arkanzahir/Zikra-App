import { getAdultRuntime } from '../adult-account-runtime.mjs';
export default async function adultAccount(req,res){
  const query=req.query??{};
  if(Object.keys(query).some(key=>key!=='action')||query.action!==undefined
      &&(typeof query.action!=='string'||!['declare','renew','withdraw'].includes(query.action))){
    res.writeHead(404,{'Content-Type':'application/json','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});res.end('{"error":"not_found"}');return;
  }
  try{
    req.url='/v1/adult-account'+(query.action?'/'+query.action:'');
    return await getAdultRuntime().handler(req,res);
  }catch{
    res.writeHead(503,{'Content-Type':'application/json','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});
    res.end('{"error":"account_unavailable"}');
  }
}

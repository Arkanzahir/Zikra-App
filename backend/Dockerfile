FROM node:24-alpine
WORKDIR /app
RUN mkdir /data && chown node:node /data
COPY --chown=node:node package.json *.mjs ./
USER node
ENV HOST=0.0.0.0 PORT=8787 ZIKRA_ACCESS_MODE=installation ZIKRA_CLIENTS_FILE=/data/clients.json ZIKRA_USAGE_FILE=/data/usage.json
EXPOSE 8787
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s CMD node -e "fetch('http://127.0.0.1:8787/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"
CMD ["node", "server.mjs"]

FROM node:20-alpine

WORKDIR /app

COPY package.json ./
COPY template.mustache ./
COPY index.js ./index.js
COPY utils.js ./utils.js
RUN npm install --omit=dev

EXPOSE 8080

CMD ["node", "index.js"]

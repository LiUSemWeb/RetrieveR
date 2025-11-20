const WebSocket = require('ws');
const Mustache = require('mustache');
const path = require('path');
const {
  loadTemplateFromDisk,
  createValueSimulator
} = require('./utils');

const PORT = process.env.PORT || 8080;
const INTERVAL_MS = Number(process.env.INTERVAL_MS || 1000);
const TEMPLATE_PATH = process.env.TEMPLATE_PATH || path.join(__dirname, 'template.mustache');
const TEMPLATE = loadTemplateFromDisk(TEMPLATE_PATH);
const SENSOR = process.env.SENSOR || 'ExampleSensor';
const INITIAL_VALUE = Number(process.env.INITIAL_VALUE) || 21;
const MIN_VALUE = Number(process.env.MIN_VALUE) || 18;
const MAX_VALUE = Number(process.env.MAX_VALUE) || 26;
const MAX_DELTA_PER_SEC = Number(process.env.MAX_DELTA_PER_SEC) || 0.02;

const wss = new WebSocket.Server({ port: PORT }, () => {
  console.log(`WebSocket server listening on port ${PORT}`);
});

// Global state
let count = 0;

const valueSim = createValueSimulator({
  initial: INITIAL_VALUE,
  min: MIN_VALUE,
  max: MAX_VALUE,
  maxDeltaPerSec: MAX_DELTA_PER_SEC
});

// Global interval that generates events forever
setInterval(() => {
  count += 1;

  const dtSeconds = INTERVAL_MS / 1000;
  const value = valueSim.step(dtSeconds);

  const payload = {
    count,
    timestamp: new Date().toISOString(),
	sensor: SENSOR,
	value
  };

  // Render using Mustache:
  const message = Mustache.render(TEMPLATE, payload);

  // Broadcast to all clients
  wss.clients.forEach(client => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(message);
    }
  });

  // Feedback when no clients are connected
  if (wss.clients.size === 0) {
    console.log('### Generated (no listeners) ###');
    console.log(message);
  }

}, INTERVAL_MS);

wss.on('connection', (ws) => {
  console.log('Client connected. Current count =', count);

  ws.on('close', () => console.log('Client disconnected'));
});

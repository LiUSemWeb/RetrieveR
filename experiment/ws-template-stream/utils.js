const fs = require('fs');
const path = require('path');

function loadTemplateFromDisk(path) {
  try {
    const contents = fs.readFileSync(path, 'utf8');
    console.log(`Loaded template from ${path}`);
    return contents;
  } catch (err) {
    console.warn(`Could not read template file at ${path}. Reason: ${err.message}`);
	process.exit(1);
  }
}

function createValueSimulator(config) {
  if (!config) {
	throw new Error("createValueSimulator: config object is required");
  }

  const {
	initial,
	min,
	max,
	maxDeltaPerSec,
	driftProb
  } = config;

  let value = initial;
  let direction = Math.random() < 0.5 ? -1 : 1; // start going up or down

  return {
	get value() {
	  return value;
	},

	step(dtSeconds = 1) {
	  if (Math.random() < driftProb) {
		direction += (Math.random() - 0.5) * 0.5;
	  }

	  // Clamp direction to a reasonable range
	  if (direction > 1) direction = 1;
	  if (direction < -1) direction = -1;

	  // Add delta proportional to direction
	  value += direction * maxDeltaPerSec * dtSeconds;

	  // Limit to bounds
	  if (value > max) {
		value = max;
		direction = -Math.abs(direction);
	  } else if (value < min) {
		value = min;
		direction = Math.abs(direction);
	  }

	  return value;
	}
  };
}

// Export functions
module.exports = {
  loadTemplateFromDisk,
  createValueSimulator
};
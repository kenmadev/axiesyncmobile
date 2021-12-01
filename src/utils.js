const axios = require('axios');
const rax = require('retry-axios');

const useAxios = (options = {}) => {
  const instance = axios.create({
    timeout: 5000,
    ...options,
  });

  instance.defaults.raxConfig = {
    instance,
    retry: 3,
    noResponseRetries: 2,
    retryDelay: 100,
    // onRetryAttempt: err => {
    //   const cfg = rax.getConfig(err);
    //   console.log(`Retry attempt #${cfg.currentRetryAttempt}`);
    // },
  };

  rax.attach(instance);
  return instance;
};

module.exports = {
  useAxios,
};

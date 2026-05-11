// environment.docker.ts — Angular running in Docker
export const environment = {
  production: false,
  apiGatewayUrl: 'http://localhost:8151'  // Docker maps host port 8151
  // OR use relative URL if nginx proxy is set up (recommended below)
};
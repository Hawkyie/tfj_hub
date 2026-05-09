import axios from 'axios';

export const api = axios.create({
  baseURL: 'http://167.235.36.212:3000',
});
import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import operationsRoutes from './routes/operations';

dotenv.config();

const app = express();

app.use(cors());
app.use(express.json());
app.use('/operations', operationsRoutes);

app.get('/', (req, res) => {
  res.json({
    message: 'TFJ API Online',
  });
});

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`TFJ API running on port ${PORT}`);
});